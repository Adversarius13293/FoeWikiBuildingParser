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
	public static final String wikiUrl = "https://de.wiki.forgeofempires.com/index.php?title=";
	public static final String specialBuildingsPage = "Liste_besonderer_Gebäude";
	public static final String special2BuildingsPage = "Liste_besonderer_Gebäude_Teil_2";
	public static final String limitedBuildingsPage = "Eingeschränkte_Gebäude";

	// TODO: Parse maybe even military buildings?
	// TODO: Some hard coded buildings? Like settlement and GEX buildings? Fountain
	// probabilities?
	// TODO: Pass down the building type from main table, if available.
	// TODO: Remove WENN from formula in post processing, if every age produces the
	// same?
	// TODO: Differentiate between goods productions? random ones, of one type,
	// equal of each.
	// TODO: Which buildings need to be connected to the street for their boosts?
	// TODO: We are getting more and more fragments. Find a better way than "special
	// production" to store and display them?
	public static void main(String[] args) {
		var allBuildings = new ArrayList<WikiBuilding>();
		var buildingUrls = new ArrayList<String>();

//		buildingUrls.addAll(getManualEdgeCaseBuildingUrls());
//		buildingUrls.addAll(getBuildingUrls(specialBuildingsPage));
		buildingUrls.addAll(getBuildingUrls(special2BuildingsPage));
//		buildingUrls.addAll(getBuildingUrls(limitedBuildingsPage));

		for (int i = 0; i < buildingUrls.size(); i++) {
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
	 * @return Urls of manually selected buildings that tend to make problems.
	 */
	private static List<String> getManualEdgeCaseBuildingUrls() {
		var buildings = new ArrayList<String>();
		// Percentages with random production.
		buildings.add(wikiUrl + "Druidentempel_-_St._10");
		// No properties text.
		buildings.add(wikiUrl + "Kloster");
		// Upgradeable, no age column.
		buildings.add(wikiUrl + "Agenten-Versteck");
		// Production building, requires population.
		buildings.add(wikiUrl + "Aviarium");
		// PRoduces special military unit.
		buildings.add(wikiUrl + "Fahnenwachen-Camp");
		// Chain building.
		buildings.add(wikiUrl + "Fischerpier");
		// Negative happiness value.
		buildings.add(wikiUrl + "Haus_des_Wolfs");
		// Set productions in the middle.
		buildings.add(wikiUrl + "Lussebullar-Bäckerei");
		// Limited building.
		buildings.add(wikiUrl + "Forge-Brunnen_-_Aktiv");
		// Limited building with production table in properties table.
		buildings.add(wikiUrl + "Kobaltblaue_Lagune_-_Aktiv");
		// New page layout, multiple boosts, productions, fragments, motivated.
		buildings.add(wikiUrl + "Chocolaterie_-_St._10");
		// Multiple random item productions, besides fragments.
		buildings.add(wikiUrl + "Druidenhütte_-_St._9");

		return buildings;
	}

	/**
	 * Get urls for all buildings that can be found in a table on the given url. For
	 * example {@link WebsiteParser#specialBuildingsPage specialBuildingsPart}
	 * 
	 * @param tableUrlPart The part of the url that leads to a table of buildings.
	 * @return Urls of all buildings on the given buildings site.
	 */
	private static List<String> getBuildingUrls(String tableUrlPart) {
		var buildings = new ArrayList<String>();
		// Fetch the HTML content of the web site.
		String url = wikiUrl + tableUrlPart;
		String htmlContent = fetchHtmlContent(url);

		// Parse the table rows within the HTML content
		int tableStartIndex = htmlContent.indexOf(tableStartTag);
		int tableEndIndex = htmlContent.indexOf(tableEndTag, tableStartIndex);
		String tableHtml = htmlContent.substring(tableStartIndex, tableEndIndex + tableEndTag.length());
		String[] rows = tableHtml.split(tableRowStartTag);

		// Iterate over each row, with one building per row.
		// Skipping the first row which contains headers and first split before content.
		for (int i = 2; i < rows.length; i++) {
			// Extract the building sub page
			String[] cells = rows[i].split("<td");
			String buildingLink = cells[2].split("title=")[1].split("\"")[0];

			String buildingUrl = wikiUrl + buildingLink;
			buildings.add(buildingUrl);
		}
		return buildings;
	}

	// TODO: Split up this method even more, still convoluted.
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

		// Some pages have a table just with the heading "Eigenschaften" followed
		// directly by a new table with the data like size and street. While other pages
		// have that in just one table... In that case connect both tables.
		buildingHtmlContent = buildingHtmlContent.replace("</tbody></table><table><tbody>", "");
		// And some production tables contain multiple tables inside their cell...
		// Remove new rows, and treat them as line breaks.
		// Assuming only the inner table data starts exactly like that.
		buildingHtmlContent = buildingHtmlContent.replace("<tr><td style=\"text-align: center;\">", "<br />");
		// And there are even completely empty tables inside cells...
		buildingHtmlContent = buildingHtmlContent
				.replace("<table style=\"margin: auto; width: 100%\"><tbody><tr><td></td></tr></tbody></table>", "");

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
		lastAge = "undefined";
		// Process each cell within the table rows
		for (String buildingRow : buildingRows) {
			String[] buildingCells = buildingRow.split("<td");

			for (String cell : buildingCells) {
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
					} else if ("Eigenschaften".equals(lastHeading) && cell.contains("<ul>")) {
						// Process each line of the listing, to find properties lines.
						String[] listings = cell.split("<li");
						for (String li : listings) {
							String liContent = cleanHtmlSplit(li);
							if (!liContent.isBlank()) {
								addPropertiesToBuildings(buildings, lastHeading, liContent);
							}
						}
						lastHeading = null;
					} else {
						// Unspecific boost. Need to analyze icon and value of data later.
						if ("Boosts:".equals(lastHeading)) {
							cellContent = cell;
						}
						addPropertiesToBuildings(buildings, lastHeading, cellContent);
						lastHeading = null;
					}
				}
			}
		}
		// Finished properties table. Now to the age dependent productions.

		buildingTableStartIndex = buildingHtmlContent.indexOf(tableStartTag, buildingTableEndIndex);
		// Assuming its the last table on the page. To skip over tables inside of cells.
		buildingTableEndIndex = buildingHtmlContent.lastIndexOf(tableEndTag);
		buildingTableHtml = buildingHtmlContent.substring(buildingTableStartIndex,
				buildingTableEndIndex + tableEndTag.length());
		buildingRows = buildingTableHtml.split(tableRowStartTag);

		// Type of production, with same index as the column, starting at 1.
		List<String> headings = new ArrayList<>();
		headings.add("dummyIndex");
		// Used for percentage and non-24 hour productions.
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
			// Some pages have each age of the specific productions as headers...
			// So assume we left the headings, as soon as we find some <td.
			if (buildingRows[r].contains("<th") && !buildingRows[r].contains("<td")) {
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
							} else if (cell.contains("<img ") && cleanedCell.equals("1 T. Produktion")) {
								// New layout, all the production in one cell as a sub-table.
								// Could run it through the img branch, but not sure if there will be other
								// cases with non-1T productions, which use the same icon.
								// TODO: Maybe skip the img branch if cell contains text?
								if (headings.size() <= spanningCol) {
									if (headings.size() != spanningCol) {
										throw new IllegalArgumentException(
												"Unexpected order of headings: " + spanningCol);
									}
									headings.add(cleanedCell);
								}
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
								case "Punkte":
								case "Boosts":
								case "Zufriedenheit":
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
								case "Liefert":
									// Only ignore sometimes...
									// Assuming only multi-column heading needs to be ignored.
									if (colspan == 1 && headings.size() <= spanningCol) {
										if (headings.size() != spanningCol) {
											throw new IllegalArgumentException(
													"Unexpected order of headings: " + spanningCol);
										}
										headings.add(cleanedCell);
									}
									break;
								case "wenn motiviert":
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
				// Not in header, so we are in table data, even with <th.
				String[] productionCells = buildingRows[r].split("<td|<th");
				lastAge = "undefined";
				int c;
				for (c = 1; c < productionCells.length; c++) {
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
					String heading = headings.get(c);

					// Some cells contain multiple lines and bonuses.
					if ("Boosts".equals(heading) || "1 T. Produktion".equals(heading) || "Liefert".equals(heading)) {
						// And sometimes even multiple bonuses in one line, so add line break.
						// Split without closing tag. so the clean-method can add an opening tag.
						String[] splitted = productionCells[c].replace(", ", "<br />").split("<br ");
						for (String s : splitted) {
							if (cleanHtmlSplit(s).isBlank() || "Wenn motiviert:".equals(cleanHtmlSplit(s))) {
								// All this replacing parts of the inner table with line breaks may lead to
								// empty entries. Assuming images without text aren't data.
								continue;
							}
							if (s.contains("<img ")) {
								// Take table heading, but can overwrite by image text if an image was found.
								heading = getImageText(s);
							} else {
								// Reset heading in case one of the lines does not have an image.
								heading = headings.get(c);
							}
							if ("Zufällig 🪄".equals(cleanHtmlSplit(s))) {
								buildings.forEach(b -> b.appendSpecialProduction("Zufallsproduktion!"));
							} else {
								addProductionToBuildings(filteredBuildings, heading, cleanHtmlSplit(s),
										multFactor.get(c));
							}
						}
					} else {
						// Probably could run everything through the upper part. But then cells with
						// proper heading and images in cell would only take the image as heading. Not
						// quite sure if every image is only used for exactly one effect. Especially in
						// the first table with building time and stuff.
						addProductionToBuildings(filteredBuildings, heading, cleanHtmlSplit(productionCells[c]),
								multFactor.get(c));
					}

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
			return cell.split("alt=\"")[1].split("[-.]")[0];
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
				.replace(" T.", "").replace(".", "").replace("+", "").replace("--", "-").trim());
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
		if (!cell.startsWith("<") && !cell.isBlank()) {
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
		case "Zufriedenheit":
		case "Zufriedenheit:":
			buildings.forEach(
					b -> b.setHappiness(buildFormulaString(lastAge, b.getHappiness(), parseInt(data), factor)));
			break;
		case "population":
			if (requiresPopulation) {
				buildings.forEach(b -> b.setPopulation(buildFormulaString(lastAge, b.getPopulation(),
						parseInt("-" + data.replace("Bevölkerung", "")), factor)));
			} else {
				buildings.forEach(b -> b.setPopulation(buildFormulaString(lastAge, b.getPopulation(),
						parseInt(data.replace("Bevölkerung", "")), factor)));
			}
			break;
		case "rank":
		case "Punkte":
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
			buildings.forEach(b -> b
					.setMoney(buildFormulaString(lastAge, b.getMoney(), parseInt(data.replace("Münzen", "")), factor)));
			break;
		case "supplies":
			buildings.forEach(b -> b.setSupplies(
					buildFormulaString(lastAge, b.getSupplies(), parseInt(data.replace("Vorräte", "")), factor)));
			break;
		case "clan_power":
			buildings.forEach(
					b -> b.setGuildPower(buildFormulaString(lastAge, b.getGuildPower(), parseInt(data), factor)));
			break;
		case "medals":
			buildings.forEach(b -> b.setMedals(
					buildFormulaString(lastAge, b.getMedals(), parseInt(data.replace("Medaillen", "")), factor)));
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
		case "all_goods_of_previous_age":
		case "random_good_of_previous_age":
			// TODO: Save different age goods as own value?
			if (data.startsWith("Gildenkasse: ")) {
				buildings.forEach(b -> b.setGuildGoods(buildFormulaString(lastAge, b.getGuildGoods(),
						parseInt(data.replace("Gildenkasse: ", "")), factor)));
			} else {
				buildings.forEach(b -> b.setGoods(buildFormulaString(lastAge, b.getGoods(), parseInt(data), factor)));
			}
			break;
		case "icon_great_building_bonus_guild_goods":
			buildings.forEach(
					b -> b.setGuildGoods(buildFormulaString(lastAge, b.getGuildGoods(), parseInt(data), factor)));
			break;
		case "strategy_points":
		case "Forge-Punkte":
			// Sometimes the data contains number, icon, and text all at once.
			buildings.forEach(b -> b.setForgePoints(buildFormulaString(lastAge, b.getForgePoints(),
					parseInt(data.replace("Forge-Punkte", "")), factor)));
			break;
		case "forge_points_production":
			// For now as special production, since its only one building.
			buildings.forEach(
					b -> b.appendSpecialProduction(converDoubleToString(parseInt(data) * factor) + "% FP Bonus"));
			break;
		case "military":
			// Sometimes can be written in a weird form.
			// TODO: Remove regex replace once bug report is implemented.
			buildings.forEach(b -> b.setUnits(buildFormulaString(lastAge, b.getUnits(),
					parseInt(data.replace("zufällige Einheit", "").replaceAll("^0x ", "")), factor)));
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
		case "icon_fragment":
		case "boost_coins_large":
		case "rush_mass_coins_medium":
		case "dead_man_stash":
		case "buccaneer":
			// TODO: Find a safe way to automatically apply this to all upcoming items.
			// Since special production is not age dependent, its hard to properly display
			// multiple fragments from one age, and also from different ages.
			// Assuming production is the same for every age.
			// So force append for first age, in case the same fragment can be produces from
			// normal and from motivated production.
			buildings.forEach(b -> b.appendSpecialProduction(data, "Bronzezeit".equals(lastAge)));
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
			switch (data) {
			case "Gibt dem ersten Gebäude desselben Sets zusätzliche Produktionen , wenn verbunden":
				buildings.forEach(b -> b.setNeedsStarting(true));
				break;
			case "Erhält zusätzliche Produktionen, wenn andere Gebäude desselben Sets  damit verbunden sind":
				// TODO: Also mark chain start buildings?
			case "Zusätzliche Produktion bei Platzierung neben anderen einzigartigen Gebäuden desselben Sets":
			case "Basisproduktion wird verdoppelt, wenn es motiviert ist. Kann geplündert werden, wenn es nicht motiviert ist":
			case "Renovierungs-Kit zur Verbesserung auf aktuelles Zeitalter nötig":
			case "Plus-Eins-Kit zur Verbesserung auf nächstes Zeitalter nötig":
			case "Kann mit  Einlagerungs-Kit im Inventar verstaut werden":
			case "Automatische Verbesserung zu deinem aktuellen Zeitalter":
			case "Kann poliert werden. Hält 12 Std.":
			case "Kann motiviert werden":
			case "Kann nicht geplündert oder motiviert werden":
				// TODO: Add plunder to building stats?
			case "Kann geplündert werden (kann nicht motiviert werden)":
			case "Boosts durch Zufriedenheit/Legendäre Bauwerke beeinflussen nur Münz- und Vorratsproduktion":
			case "Zufriedenheit wird verdoppelt, wenn es poliert ist":
				// Ignore.
				break;
			default:
				if (data.contains("verbessert dieses Gebäude zu")) {
					buildings.forEach(b -> b.setUpgradeable(true));
				} else if (data.startsWith("Dieses Gebäude wird nach") && data.endsWith("herabgestuft.")) {
					buildings.forEach(b -> b.appendSpecialProduction("Eingeschränkte Produktion: "
							+ data.split("Dieses Gebäude wird nach ")[1].split(" auf")[0]));
				} else {
					throw new IllegalArgumentException("Unexpected properties: " + data);
				}
			}
			break;
		case "Art:":
			buildings.forEach(b -> b.setType(data));
			break;
		case "Benötigte Straße:":
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
		case "Boosts:":
			var tmpDataType = getImageText(data);
			var tmpData = cleanHtmlSplit(data);
			addProductionToBuildings(buildings, tmpDataType, tmpData, 1);
			break;
		case "Zufriedenheit:":
			addProductionToBuildings(buildings, dataType, data, 1);
			break;
		case "Bauzeit:":
			buildings.forEach(b -> b.setBuildTime(data));
			break;
		case "Verbesserte Sammlung:":
			buildings.forEach(b -> b.appendSpecialProduction(data));
			break;
		case "1 T. Produktion:":
			// Only tested for one entry. Seems to be a table inside that cell.
			int indexSpace = data.indexOf(' ');
			addProductionToBuildings(buildings, data.substring(indexSpace).trim(), data.substring(0, indexSpace), 1);
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
			Thread.sleep(random.nextLong(1500, 2500));
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