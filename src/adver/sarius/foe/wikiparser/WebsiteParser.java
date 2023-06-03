package adver.sarius.foe.wikiparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import adver.sarius.foe.building.Street;
import adver.sarius.foe.building.WikiBuilding;

public class WebsiteParser {

	// Used in formula to compare the age against.
	/** Used in the formulas to compare the age string against. */
//	private static final String compareAgeTo = "$Z$1";
	private static final String compareAgeTo = "$AO$1";

	/** Random object, currently used for the random waiting time. */
	private static final Random random = new Random();

	/** Internal helper variable to remember the last known age for production. */
	private static String lastAge;
	/**
	 * Internal helper variable to remember, if the current building requires
	 * instead of provides population.
	 */
	private static boolean requiresPopulation = false;

	public static final String tableStartTag = "<table";
	public static final String tableEndTag = "</table>";
	public static final String tableRowStartTag = "<tr>";
	public static final String wikiUrl = "https://de.wiki.forgeofempires.com";

	// TODO: Parse limited buildings page. Maybe even military buildings?
	// TODO: Some hard coded buildings? Like settlement and GEX buildings? Fountain
	// probabilities?
	// TODO: Remove WENN from formula in post processing, if every age produces the
	// same?
	// TODO: Broken fragment chances?
	// https://de.wiki.forgeofempires.com/index.php?title=Druidenh%C3%BCtte_-_St._9
	public static void main(String[] args) {
		var allBuildings = new ArrayList<WikiBuilding>();
		var buildingUrls = getSpecialBuildingUrls();

//		for (int i = 0; i < buildingUrls.size(); i++) {
		for (int i = 0; i < 5; i++) {
			List<WikiBuilding> buildings = processBuildingWebSite(buildingUrls.get(i));
			// For easier debugging. Output each building when processed, include its row.
			final int temp = i;
			buildings.forEach(b -> System.out.println(temp + ": " + b.toString()));
			allBuildings.addAll(buildings);
		}
		System.out.println("Done");
		// No real need to filter out buildings afterwards? Can just do that in the
		// resulting document itself.
//			outputBuildings(allBuildings);
	}

	/**
	 * @return Urls of all buildings on the special buildings site.
	 */
	private static List<String> getSpecialBuildingUrls() {
		var buildings = new ArrayList<String>();
		// Fetch the HTML content of the web site.
		String url = wikiUrl + "/index.php?title=Liste_besonderer_Geb%C3%A4ude";
		String htmlContent = fetchHtmlContent(url);

		// Parse the table rows within the HTML content
		int tableStartIndex = htmlContent.indexOf(tableStartTag);
		int tableEndIndex = htmlContent.indexOf(tableEndTag, tableStartIndex);
		String tableHtml = htmlContent.substring(tableStartIndex, tableEndIndex + tableEndTag.length());
		String[] rows = tableHtml.split(tableRowStartTag);

		// Over 700 buildings in this table.
		// Iterate over each row, with one building per row.
		// Skipping the first row which contains headers and first split before content.
		for (int i = 2; i < rows.length; i++) {
			// Extract the building sub page
			String[] cells = rows[i].split("<td");
			String buildingLink = cells[2].split("href=\"")[1].split("\"")[0];

			String buildingUrl = wikiUrl + buildingLink;
			buildings.add(buildingUrl);
		}
		return buildings;
	}

	/**
	 * Parse the html web site from the given url, and extract all relevant
	 * informations.
	 * 
	 * @param buildingUrl Url of the building's detail page.
	 * @return All buildings created from that page. Set-productions create
	 *         additional building entries.
	 * @throws IOException
	 */
	private static List<WikiBuilding> processBuildingWebSite(String buildingUrl) {
		// Fetch the linked page for each building
		String buildingHtmlContent = fetchHtmlContent(buildingUrl);
		String buildingName = buildingHtmlContent.split("<span dir=\"auto\">")[1].split("</span>")[0];

		// Starting with the properties table.
		// Parse the table rows within the linked page
		int buildingTableStartIndex = buildingHtmlContent.indexOf(tableStartTag);
		// The page contains a table inside a table, so the first found end tag is from
		// the inner table.
		// But we just assume the inner table is always in the last row.
		int buildingTableEndIndex = buildingHtmlContent.indexOf(tableEndTag, buildingTableStartIndex);
		String buildingTableHtml = buildingHtmlContent.substring(buildingTableStartIndex,
				buildingTableEndIndex + tableEndTag.length());
		String[] buildingRows = buildingTableHtml.split(tableRowStartTag);

		// Some buildings have different production based on set members. Create one
		// instance for each, which means multiple entries for one page.
		List<WikiBuilding> buildings = new ArrayList<>();
		var building = new WikiBuilding();
		building.setName(buildingName);
		buildings.add(building);

		String lastHeading = null;
		requiresPopulation = false;
		// Process each cell within the table rows
		for (String buildingRow : buildingRows) {
			String[] buildingCells = buildingRow.split("<td");

			for (String cell : buildingCells) {
				if (cell.isBlank()) {
					continue;
				}
				// Remove HTML tags and trim whitespace from the cell content
				String cellContent = cleanHtmlSplit(cell);
				if (cellContent.isBlank()) {
					continue;
				}
				// Sometimes data is in the same table row, and sometimes not.
				// So depending on the last cell, handle the content differently.
				if (lastHeading == null) {
					lastHeading = cellContent;
				} else if ("Tauschwert:".equals(lastHeading)) {
					// Bit weird solution to check trade prices here, but table in table is just
					// weird.
					// Also assuming this is the last cell, since lastHeading will be stuck now.
					if ("1 T.".equals(cellContent)) {

						buildings.forEach(b -> b.setCoins24(parseInt(cleanHtmlSplit(buildingCells[2]))));
						String[] splitted = cleanHtmlSplit(buildingCells[3]).split(" - ");
						buildings.forEach(b -> b.setGemsMin24(parseInt(splitted[0])));
						// if it is just always 0 gems.
						if (splitted.length > 1) {
							buildings.forEach(b -> b.setGemsMax24(parseInt(splitted[1])));
						}
					}
				} else {
					if ("Eigenschaften".equals(lastHeading) && "Art:".equals(cellContent)) {
						// Sometimes buildings (like Kloster) have no leading properties text.
						// Assumes "Art:" as the next column.
						lastHeading = cellContent;
					} else {
						addPropertiesToBuildings(buildings, lastHeading, cellContent);
						lastHeading = null;
					}
				}
			}
		}
		// Finished properties table. Now to the age dependent productions.

		buildingTableStartIndex = buildingHtmlContent.indexOf(tableStartTag, buildingTableEndIndex);
		buildingTableEndIndex = buildingHtmlContent.indexOf(tableEndTag, buildingTableStartIndex);
		buildingTableHtml = buildingHtmlContent.substring(buildingTableStartIndex,
				buildingTableEndIndex + tableEndTag.length());
		buildingRows = buildingTableHtml.split(tableRowStartTag);

		// Type of production, with same index as the column, starting at 1.
		List<String> headings = new ArrayList<>();
		headings.add("dummyIndex");
		// TODO: Used for percentage and non-24 hour productions.
		Map<Integer, Double> multFactor = new HashMap<>();

		// Marks specific columns for specific amounts of set members. Key is the
		// column.
		Map<Integer, Integer> setProduction = new HashMap<>();
		// Used to lay out the headers, and find the correct column for each header row.
		// Each row is saved as the key, starting with 1. They contain a map with each
		// column that was already processed in that row.
		Map<Integer, List<Integer>> headerStructureHelper = new HashMap<>();

		// Process each cell within the production table rows.
		for (int r = 1; r < buildingRows.length; r++) {
			// Get headers. They can be in multiple rows or columns.
			if (buildingRows[r].contains("<th")) {
				String[] headerCells = buildingRows[r].split("<th");
				for (int h = 1; h < headerCells.length; h++) {
					String cell = headerCells[h];

					int rowspan = getRowspan(cell);
					int colspan = getColspan(cell);

					// If multiple rows, add to each row. If multiple cols, add multiple times.
					for (int spanningRow = r; spanningRow < r + rowspan; spanningRow++) {
						if (!headerStructureHelper.containsKey(spanningRow)) {
							var array = new ArrayList<Integer>();
							// Fill with dummy, since headers start at index 1.
							array.add(-1);
							headerStructureHelper.put(spanningRow, array);
						}

						List<Integer> list = headerStructureHelper.get(spanningRow);
						int freeCol = list.size();
						// Find "holes" to fill.
						for (int tmp = 1; tmp < list.size(); tmp++) {
							if (tmp - list.get(tmp) < 0) {
								freeCol = tmp;
							}
						}

						for (int spanningCol = freeCol; spanningCol < freeCol + colspan; spanningCol++) {
							// Assuming columns are filled from left to right.
							if (list.contains(spanningCol)) {
								throw new IllegalArgumentException(
										"Column already processed: " + spanningRow + "/" + spanningCol);
							}
							// Mark column as processed.
							list.add(spanningCol, spanningCol);

							// Initialize map.
							if (!multFactor.containsKey(spanningCol)) {
								multFactor.put(spanningCol, 1.);
							}

							// Evaluate content, to fill other lists.
							String cleanedCell = cleanHtmlSplit(cell);
							if (cell.contains("<img ") && cleanedCell.matches("[0-9]+ x")) {
								// Assuming image + this text means set-production.
								if (setProduction.containsKey(spanningCol)) {
									throw new IllegalArgumentException(
											"Column already marked for set-production: " + spanningCol);
								}
								// Assuming new set buildings are also always added to the setProduction map.
								// Assuming there are no mixed sets.
								if (!setProduction.containsValue(parseInt(cleanedCell))) {
									// Assuming the first building is always without any sets.
									// BuildingName [2 x Set]
									buildings.add(new WikiBuilding(building, " [" + cleanedCell + " Set]"));
								}
								// Assuming there are no less than 1x header entries.
								setProduction.put(spanningCol, parseInt(cleanedCell));
								int maxSets = setProduction.values().stream().max(Integer::compare).get();
								String maxSetsName = building.getName() + " [" + maxSets + " x Set]";
								// Recalculate highest set building after adding a new entry.
								buildings.forEach(b -> b.setMaxSetMembers(maxSetsName.equals(b.getName())));
							} else if (cleanedCell.matches("[0-9]+%")) {
								// Production chance.
								multFactor.put(spanningCol,
										multFactor.get(spanningCol) * (parseInt(cleanedCell) / 100.));
								// TODO: Maybe not needed, and already covered by text heading later on?
								buildings.forEach(b -> b.appendSpecialProduction("Einberechnete Zufallsproduktion!"));
							} else if (cleanedCell.matches("[0-9]+ Min.")) {
								// Different production times. Scale up to 24 hours.
								double factor = 60. / parseInt(cleanedCell) * 24;
								multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);

								buildings.forEach(
										b -> b.appendSpecialProduction("Produktion auf 24 Stunden gerechnet!"));

								// TODO: Somehow mark or handle production buildings.
								// Some buildings produce only supplies. Adding them is wrong, and even too long
								// of a string.
//								if (buildings.stream().anyMatch(b -> !"Produktionsstätten".equals(b.getType())
//										&& !"Zikkurat".equals(b.getName()) && !"Strohhütte".equals(b.getName())
//										&& !"Schrein der Inspiration".equals(b.getName())
//										&& !"Schneekugel".equals(b.getName())
//										&& !"Renaissance-Villa".equals(b.getName())
//										&& !"Lebkuchenhaus".equals(b.getName())
//										&& !"Königliches Marmortor".equals(b.getName()))) {
//									throw new IllegalArgumentException(
//											"Expected to be a production building: " + cell);
//								}
//								buildings.forEach(b -> b.setName(b.getName() + " PRODUCTION"));

							} else if (cleanedCell.matches("[0-9]+ Std.")) {
								double factor = 24. / parseInt(cleanedCell);
								multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);
								buildings.forEach(
										b -> b.appendSpecialProduction("Produktion auf 24 Stunden gerechnet!"));
							} else if (cleanedCell.matches("[0-9]+ T.")) {
								double factor = 1. / parseInt(cleanedCell);
								multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);
								if (!"1 T.".equals(cleanedCell)) {
									buildings.forEach(
											b -> b.appendSpecialProduction("Produktion auf 24 Stunden gerechnet!"));
								}
							} else if (cell.contains("<img ")) {
								// Normal header processing for actual products instead of structure.
								// Does not need to go over multiple rows, and hopefully not multiple columns.

								// Images need to be analyzed for the columns meaning.
								if (headings.size() <= spanningCol) {
									if (headings.size() != spanningCol) {
										throw new IllegalArgumentException(
												"Unexpected order of headings: " + spanningCol);
									}
									headings.add(getImageText(cell));
								}
							} else {
								switch (cleanedCell) {
								case "Zeitalter":
									// Assumes the production cells can be filled from left to right.
									// But ignore if they go over multiple rows.
									if (headings.size() <= spanningCol) {
										if (headings.size() != spanningCol) {
											throw new IllegalArgumentException(
													"Unexpected order of headings: " + spanningCol);
										}
										headings.add(cleanedCell);
									}
									break;
								case "Verbindung gewährt":
									// Check for chain buildings. Currently assuming it is already marked in the
									// first properties, so just making sure.
									if (buildings.stream().anyMatch(b -> !b.isNeedsStarting())) {
										throw new IllegalArgumentException(
												"Expected to be already marked as chain building: " + cleanedCell);
									}
									break;
								case "Benötigt":
									// Assuming keyword is only for population.
									if (!"Produktionsstätten".equals(building.getType())
											&& !"Militärgebäude".equals(building.getType())) {
										System.out.println(
												"Assuming building requires population, but it has an unexpected type: "
														+ building.getName());
									}
									requiresPopulation = true;
									// TODO: Maybe save in additional list, and apply this to the corresponding
									// column?
									break;
								case "Folgendes wird zufällig produziert:":
									buildings.forEach(b -> b.appendSpecialProduction("Zufallsproduktion!"));
									break;
								case "wenn motiviert":
								case "Liefert":
								case "Produziert":
								case "":
									// Ignore.
									break;
								default:
									throw new IllegalArgumentException("Unexpected header content: " + cell);
								}
							}
						}
					}
				}
			} else {
				// Not <th, so we are in table data.
				String[] productionCells = buildingRows[r].split("<td");
				lastAge = "undefined";
				int c;
				for (c = 1; c < productionCells.length; c++) {
					String cell = cleanHtmlSplit(productionCells[c]);
					List<WikiBuilding> filteredBuildings = null;
					// If this production requires specific set counts, filter buildings out based
					// on their name.
					if (setProduction.containsKey(c)) {
						// Streams sometimes are weird...
						final int tmp = c;
						// TODO: Save number of required set members as integer in building object?
						// Apply production to all buildings with at least that many set members.
						filteredBuildings = buildings.stream()
								.filter(b -> b.getName().contains(" x Set]")
										&& parseInt(b.getName().split(" \\[")[1].split(" x Set\\]")[0]) >= setProduction
												.get(tmp))
								.collect(Collectors.toList());
					} else {
						filteredBuildings = buildings;
					}
					addProductionToBuildings(filteredBuildings, headings.get(c), cell, multFactor.get(c));
				}
				if ((headings.size()) != c) {
					throw new IllegalArgumentException("Missmatch of headings to production content! " + c);
				}
			}
		}
		return buildings;
	}

	/**
	 * 
	 * Output the list of buildings to the console. Filters the given list.
	 * 
	 * @param buildings List of all buildings, will still be filtered.
	 */
	private static void outputBuildings(List<WikiBuilding> buildings) {
		// Modify here to change what buildings should be printed out.
		buildings.stream().filter(b -> !b.isUpgradeable()).filter(WikiBuilding::isMaxSetMembers)
				.forEach(System.out::println);
	}

	/**
	 * 
	 * @param cell Text containing an html rowspan attribute.
	 * @return The html rowspan value.
	 */
	private static int getRowspan(String cell) {
		if (cell.contains(" rowspan=\"")) {
			return Integer.parseInt(cell.split(" rowspan=\"")[1].split("\"")[0]);
		} else {
			return 1;
		}
	}

	/**
	 * 
	 * @param cell Text containing an html colspan attribute.
	 * @return The html colspan value.
	 */
	private static int getColspan(String cell) {
		if (cell.contains(" colspan=\"")) {
			return Integer.parseInt(cell.split(" colspan=\"")[1].split("\"")[0]);
		} else {
			return 1;
		}
	}

	/**
	 * Get the alt text of the image html tag, up until the first dash in the
	 * string.
	 * 
	 * @param cell the html cell with an img tag and alt text.
	 * @return The parsed alt text of the image. Null if no html img tag was found.
	 */
	private static String getImageText(String cell) {
		if (cell.contains("<img ")) {
			return cell.split("alt=\"")[1].split("-")[0];
		} else {
			return null;
		}
	}

	/**
	 * Parse a String to return the value as an Integer. Removes different parts of
	 * the string that are commonly around numbers before parsing.
	 * 
	 * @param intString String that mainly contains just a number, possibly with
	 *                  some limited number related text.
	 * @return The Strings content as an Integer.
	 */
	private static int parseInt(String intString) {
		// Replaces double dash with single dash. Since assuming the goal is to have it
		// negative, and not to invert it.
		return Integer.parseInt(intString.replace("%", "").replace(" x", "").replace(" Min.", "").replace(" Std.", "")
				.replace(" T.", "").replace(".", "").replace("--", "-"));
	}

	/**
	 * Removes all html tags. Expects the string to either start with an opening
	 * tag, or having an additional closing tag, meaning a new opening tag could be
	 * added at the start.
	 * 
	 * @param cell Html cell with tags.
	 * @return The text content of the html without any tags.
	 */
	private static String cleanHtmlSplit(String cell) {
		if (!cell.startsWith("<")) {
			cell = "<xx" + cell;
		}
		return cell.replaceAll("<.*?>", "").trim();
	}

	/**
	 * Add the data for the given dataType to the production of all the buildings in
	 * the given list. Unknown dataTypes will throw an exception. Expects to add the
	 * current age first, which will then be remembered for all following data,
	 * until a new age is passed.
	 * 
	 * @param buildings List of buildings to add the properties to.
	 * @param dataType  The dataType, normally the table heading or name of an icon.
	 * @param data      The data to add, normally a cell of the table with numbers.
	 * @param factor    A multiplicative factor for the data, that may be passed to
	 *                  {@link WebsiteParser#buildFormulaString(String, String, double, double)
	 *                  buildFormulaString}
	 */
	private static void addProductionToBuildings(List<WikiBuilding> buildings, String dataType, String data,
			double factor) {
		switch (dataType) {
		case "Zeitalter":
			// Remember last age, assuming all following calls are for that age.
			lastAge = data;
			break;
		case "happiness":
		case "happiness_amount":
			buildings.forEach(
					b -> b.setHappiness(buildFormulaString(lastAge, b.getHappiness(), parseInt(data), factor)));
			break;
		case "population":
			if (requiresPopulation) {
				buildings.forEach(b -> b
						.setPopulation(buildFormulaString(lastAge, b.getPopulation(), parseInt("-" + data), factor)));
			} else {
				buildings.forEach(
						b -> b.setPopulation(buildFormulaString(lastAge, b.getPopulation(), parseInt(data), factor)));
			}
			break;
		case "rank":
			buildings.forEach(b -> b.setRanking(buildFormulaString(lastAge, b.getRanking(), parseInt(data), factor)));
			break;
		case "def_boost_attacker":
			buildings.forEach(b -> b
					.setAttackerDefense(buildFormulaString(lastAge, b.getAttackerDefense(), parseInt(data), factor)));
			break;
		case "att_boost_attacker":
			buildings.forEach(b -> b
					.setAttackerAttack(buildFormulaString(lastAge, b.getAttackerAttack(), parseInt(data), factor)));
			break;
		case "att_def_boost_attacker":
			buildings.forEach(b -> {
				b.setAttackerAttack(buildFormulaString(lastAge, b.getAttackerAttack(), parseInt(data), factor));
				b.setAttackerDefense(buildFormulaString(lastAge, b.getAttackerDefense(), parseInt(data), factor));
			});
			break;
		case "def_boost_defender":
			buildings.forEach(b -> b
					.setDefenderDefense(buildFormulaString(lastAge, b.getDefenderDefense(), parseInt(data), factor)));
			break;
		case "att_boost_defender":
			buildings.forEach(b -> b
					.setDefenderAttack(buildFormulaString(lastAge, b.getDefenderAttack(), parseInt(data), factor)));
			break;
		case "att_def_boost_defender":
			buildings.forEach(b -> {
				b.setDefenderAttack(buildFormulaString(lastAge, b.getDefenderAttack(), parseInt(data), factor));
				b.setDefenderDefense(buildFormulaString(lastAge, b.getDefenderDefense(), parseInt(data), factor));
			});
		case "coin_production":
			buildings.forEach(
					b -> b.setMoneyPercent(buildFormulaString(lastAge, b.getMoneyPercent(), parseInt(data), factor)));
			break;
		case "supply_production":
			buildings.forEach(b -> b
					.setSuppliesPercent(buildFormulaString(lastAge, b.getSuppliesPercent(), parseInt(data), factor)));
			break;
		case "money":
			buildings.forEach(b -> b.setMoney(buildFormulaString(lastAge, b.getMoney(), parseInt(data), factor)));
			break;
		case "supplies":
			buildings.forEach(b -> b.setSupplies(buildFormulaString(lastAge, b.getSupplies(), parseInt(data), factor)));
			break;
		case "clan_power":
			buildings.forEach(
					b -> b.setGuildPower(buildFormulaString(lastAge, b.getGuildPower(), parseInt(data), factor)));
			break;
		case "medals":
			buildings.forEach(b -> b.setMedals(buildFormulaString(lastAge, b.getMedals(), parseInt(data), factor)));
			break;
		case "blueprint":
			buildings.forEach(
					b -> b.setBlueprints(buildFormulaString(lastAge, b.getBlueprints(), parseInt(data), factor)));
			break;
		case "premium":
			buildings.forEach(b -> b.setDiamonds(buildFormulaString(lastAge, b.getDiamonds(), parseInt(data), factor)));
			break;
		case "goods":
		case "all_goods_of_age":
		case "random_good_of_age":
			// TODO: There should be some buildings that give goods of a different age?
			buildings.forEach(b -> b.setGoods(buildFormulaString(lastAge, b.getGoods(), parseInt(data), factor)));
			break;
		case "icon_great_building_bonus_guild_goods":
			buildings.forEach(
					b -> b.setGuildGoods(buildFormulaString(lastAge, b.getGuildGoods(), parseInt(data), factor)));
			break;
		case "strategy_points":
			buildings.forEach(
					b -> b.setForgePoints(buildFormulaString(lastAge, b.getForgePoints(), parseInt(data), factor)));
			break;
		case "military":
			buildings.forEach(b -> b.setUnits(buildFormulaString(lastAge, b.getUnits(), parseInt(data), factor)));
			break;
		case "armyuniticons_90x90_rogue":
			// Assuming the age can be ignored here.
			// TODO: Get unit name from parsed page for all the specials?
			buildings
					.forEach(b -> b.appendSpecialProduction(converDoubleToString(parseInt(data) * factor) + "x Agent"));
			break;
		case "armyuniticons_90x90_color_guard":
			// Assuming the age can be ignored here.
			buildings.forEach(
					b -> b.appendSpecialProduction(converDoubleToString(parseInt(data) * factor) + "x Fahnenwache"));
			break;
		case "armyuniticons_90x90_SpaceAgeJupiterMoon_champion":
			// Assuming the age can be ignored here.
			buildings.forEach(b -> b.appendSpecialProduction(converDoubleToString(parseInt(data) * factor) + "x Held"));
		case "armyuniticons_90x90_military_drummer":
			// Assuming the age can be ignored here.
			buildings.forEach(
					b -> b.appendSpecialProduction(converDoubleToString(parseInt(data) * factor) + "x Trommler"));
			break;
		default:
			throw new IllegalArgumentException("Unexpected type: " + dataType);
		}
	}

	/**
	 * Extend the given formula with the given data. The formula then may return
	 * different values based on {@link WebsiteParser#compareAgeTo compareAgeTo}.
	 * Current format is made for german open office calc.
	 * 
	 * @param age            The age that the given value is for.
	 * @param currentFormula Current formula. May be empty, and will be extended by
	 *                       the given value.
	 * @param ageValue       The value specific to the age.
	 * @param factor         An additional factor to multiply the value with. Factor
	 *                       of 1 will be omitted.
	 * @return The extended formula as a single string.
	 */
	private static String buildFormulaString(String age, String currentFormula, double ageValue, double factor) {
		if (currentFormula == null || currentFormula.isEmpty()) {
			currentFormula = "=\"ERROR\"";
		}
		String valueString = converDoubleToString(ageValue);
		String factorString = "";
		if (factor != 1) {
			factorString = "*" + converDoubleToString(factor);
		}
		if (age.equals("undefined")) {
			// Some buildings have only one line of production, without any age.
			if (currentFormula.equals("=\"ERROR\"")) {
				return "=" + valueString + factorString;
			} else {
				throw new IllegalArgumentException(
						"Got no age, but the formula is already filled: " + age + currentFormula);
			}
		} else if (currentFormula.contains(age)) {
			// Entry already exists. But some buildings produce different sets of goods or
			// forge points, so add all together.
			return currentFormula.replace("\"" + age + "\";", "\"" + age + "\";" + valueString + factorString + "+");
		} else {
			return currentFormula.replace("\"ERROR\"",
					"WENN(" + compareAgeTo + "=\"" + age + "\";" + valueString + factorString + ";\"ERROR\")");
		}
	}

	/**
	 * Converts a double number to a String. Will have no thousand separators, and a
	 * comma as decimal separator.
	 * 
	 * @param number number to be returned as string.
	 * @return String representation of the given number.
	 */
	private static String converDoubleToString(double number) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
		symbols.setDecimalSeparator(',');
		// Disable thousands separator.
		symbols.setGroupingSeparator('\0');

		DecimalFormat decimalFormat = new DecimalFormat("0.########", symbols);

		return decimalFormat.format(number);
	}

	/**
	 * Add the data for the given dataType to the properties of all the buildings in
	 * the given list. Unknown dataTypes will throw an exception.
	 * 
	 * @param buildings List of buildings to add the properties to.
	 * @param dataType  The dataType, normally the table heading.
	 * @param data      The data to add, normally a cell of the table with numbers.
	 */
	private static void addPropertiesToBuildings(List<WikiBuilding> buildings, String dataType, String data) {
		switch (dataType) {
		case "Eigenschaften":
			if (data.contains("wenn verbunden")) {
				buildings.forEach(b -> b.setNeedsStarting(true));
			}
			if (data.contains("verbessert dieses Gebäude zu")) {
				buildings.forEach(b -> b.setUpgradeable(true));
			}
			// TODO: Also mark chain start buildings?
			break;
		case "Art:":
			buildings.forEach(b -> b.setType(data));
			break;
		case "Straße:":
			buildings.forEach(b -> b.setStreet(Street.fromString(data)));
			break;
		case "Größe:":
			buildings.forEach(b -> {
				String[] splitted = data.split("x");
				b.setHeight(Integer.parseInt(splitted[0]));
				b.setWidth(Integer.parseInt(splitted[1]));
			});
			break;
		case "Kette:":
			// Similar to "Set", but used for buildings that need to be connected in a
			// specific order.
		case "Set:":
			buildings.forEach(b -> b.setSet(data));
			break;
		case "Einheitenkosten:":
		case "Slot-Kosten:":
		case "Eingeführt mit:":
			// Ignore.
			break;
		default:
			throw new IllegalArgumentException("Unexpected type: " + dataType);
		}
	}

	/**
	 * Just sleep for some random amount of time. To make sure the hundreds of calls
	 * per seconds don't get my IP blocked or something like that.
	 */
	private static void waitBetweenCalls() {
		try {
			Thread.sleep(random.nextLong(500, 1500));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get the complete web site as one string.
	 * 
	 * @param urlString The url of the web site.
	 * @return The html content of the web site.
	 * @throws IOException
	 */
	private static String fetchHtmlContent(String urlString) {
		StringBuilder htmlContent = new StringBuilder();
		try {
			waitBetweenCalls();
			URL url;
			url = new URL(urlString);
			URLConnection connection = url.openConnection();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					htmlContent.append(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return htmlContent.toString();
	}
}