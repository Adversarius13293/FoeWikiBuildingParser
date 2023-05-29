package adver.sarius.foe.wikiparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import adver.sarius.foe.building.Street;
import adver.sarius.foe.building.WikiBuilding;

public class WebsiteParser {

	public static void main(String[] args) {
		try {
			// Fetch the HTML content of the website.
			String url = "https://de.wiki.forgeofempires.com/index.php?title=Liste_besonderer_Geb%C3%A4ude";
			String htmlContent = fetchHtmlContent(url);

			// Parse the table rows within the HTML content
			String tableStartTag = "<table";
			String tableEndTag = "</table>";
			int tableStartIndex = htmlContent.indexOf(tableStartTag);
			int tableEndIndex = htmlContent.indexOf(tableEndTag, tableStartIndex);
			String tableHtml = htmlContent.substring(tableStartIndex, tableEndIndex + tableEndTag.length());
			String[] rows = tableHtml.split("<tr>");

			// Iterate over each row, with one building per row.
			// Skipping the first row which contains headers.
//            for (int i = 2; i < rows.length; i++) {
			for (int i = 80; i < 99; i++) {
				String row = rows[i];

				// Extract the building sub page
				String[] cells = row.split("<td");
				String buildingLink = cells[2].split("href=\"")[1].split("\"")[0];
				String buildingName = cells[2].split("\">")[1].split("</a>")[0];

				// Fetch the linked page for each building
				String buildingUrl = "https://de.wiki.forgeofempires.com" + buildingLink;
				String buildingHtmlContent = fetchHtmlContent(buildingUrl);

				// Starting with the properties table.
				// Parse the table rows within the linked page
				int buildingTableStartIndex = buildingHtmlContent.indexOf(tableStartTag);
				// The page contains a table inside a table, so the first found end tag is from
				// the inner table.
				// But we just assume the inner table is always in the last row.
				int buildingTableEndIndex = buildingHtmlContent.indexOf(tableEndTag, buildingTableStartIndex);
				String buildingTableHtml = buildingHtmlContent.substring(buildingTableStartIndex,
						buildingTableEndIndex + tableEndTag.length());
				String[] buildingRows = buildingTableHtml.split("<tr>");

				// Some buildings have different production based on set members. Create one
				// instance for each.
				List<WikiBuilding> buildings = new ArrayList<>();
				var building = new WikiBuilding();
				buildings.add(building);
				building.setName(buildingName);

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
//						System.out.println("cell: " + cellContent);
					}
//					System.out.println("end row");

				}
				// Finished properties table. Now to the age dependent productions.

				buildingTableStartIndex = buildingHtmlContent.indexOf(tableStartTag, buildingTableEndIndex);
				buildingTableEndIndex = buildingHtmlContent.indexOf(tableEndTag, buildingTableStartIndex);
				buildingTableHtml = buildingHtmlContent.substring(buildingTableStartIndex,
						buildingTableEndIndex + tableEndTag.length());
				buildingRows = buildingTableHtml.split("<tr>");

				// Type of production, with same index as the column, starting at 1.
				List<String> headings = new ArrayList<>();
				headings.add("dummyIndex");
				// TODO: Used for percentage and non-24 hour productions.
				Map<Integer, Double> multFactor = new HashMap<>();

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
										// TODO: Do something with set-production.

									} else if (cleanedCell.matches("[0-9]+%")) {
										// Production chance.
										multFactor.put(spanningCol,
												multFactor.get(spanningCol) * (parseInt(cleanedCell) / 100.));
										// TODO: Maybe not needed, and already covered by text heading later on?
										buildings.forEach(b -> {
											if (!b.getSpecialProduction().contains("Zufallsproduktion!")) {
												b.appendSpecialProduction("Zufallsproduktion!");
											}
										});
									} else if (cleanedCell.matches("[0-9]+ Min.")) {
										// Different production times. Scale up to 24 hours.
										double factor = 60. / parseInt(cleanedCell) * 24;
										multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);

										// TODO: Somehow mark or handle production buildings
//										if (buildings.stream().anyMatch(b -> !"Produktionsstätten".equals(b.getType())
//												&& !"Zikkurat".equals(b.getName()) && !"Strohhütte".equals(b.getName())
//												&& !"Schrein der Inspiration".equals(b.getName())
//												&& !"Schneekugel".equals(b.getName())
//												&& !"Renaissance-Villa".equals(b.getName())
//												&& !"Lebkuchenhaus".equals(b.getName())
//												&& !"Königliches Marmortor".equals(b.getName()))) {
//											throw new IllegalArgumentException(
//													"Expected to be a production building: " + cell);
//										}
//										buildings.forEach(b -> b.setName(b.getName() + " PRODUCTION"));

									} else if (cleanedCell.matches("[0-9]+ Std.")) {
										double factor = 24. / parseInt(cleanedCell);
										multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);
									} else if (cleanedCell.matches("[0-9]+ T.")) {
										double factor = 1. / parseInt(cleanedCell);
										multFactor.put(spanningCol, multFactor.get(spanningCol) * factor);

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
														"Expected to be already marked as chain building: "
																+ cleanedCell);
											}
											break;
										case "Benötigt":
											// Assuming keyword is only for population.
											System.out.println("Assuming building requires population: "
													+ buildings.get(0).getName());
											requiresPopulation = true;
											// TODO: Maybe save in additional list, and apply this to the corresponding
											// column?
											break;
										case "Folgendes wird zufällig produziert:":
											buildings.forEach(b -> {
												if (!b.getSpecialProduction().contains("Zufallsproduktion!")) {
													b.appendSpecialProduction("Zufallsproduktion!");
												}
											});
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
							// Old logic before structure analyzing.
							/*
							 * // Images need to be analyzed for the columns meaning. if
							 * (cell.contains("<img ")) { headings.add(getImageText(cell)); // TODO: Somehow
							 * make copy of building for different set members. // TODO: Currently
							 * set-productions are ignored, since they will be added as // additional
							 * headers, which will never get used. // Somehow have to find out which
							 * set-production belongs to which product. And // somehow generate new building
							 * entries from it. } else { cell = cleanHtmlSplit(cell); if
							 * ("Zeitalter".equals(cell)) { headings.add(cell); } else { switch (cell) {
							 * case "Zeitalter": headings.add(cell); break; case "Verbindung gewährt": //
							 * Check for chain buildings. Currently assuming it is already marked in the //
							 * first properties, so just making sure. if (buildings.stream().anyMatch(b ->
							 * !b.isNeedsStarting())) { throw new IllegalArgumentException(
							 * "Expected to be already marked as chain building: " + cell); } break; case
							 * "Benötigt": // Assuming it is only for population. System.out.println(
							 * "Assuming it requires population: " + buildings.get(0).getName());
							 * requiresPopulation = true; break; case "Folgendes wird zufällig produziert:":
							 * buildings.forEach(b -> b.appendSpecialProduction("Zufallsproduktion!"));
							 * break; case "5 Min.": case "15 Min.": case "1 Std.": case "4 Std.": case
							 * "8 Std.": case "2 T.": // TODO: Probably production building, still needs
							 * work!!!!! // There are some non-24h productions. Somehow need to factor those
							 * in. Same // problem as figuring out the set-productions. if
							 * (buildings.stream().anyMatch(b -> !"Produktionsstätten".equals(b.getType())
							 * && !"Zikkurat".equals(b.getName()) && !"Strohhütte".equals(b.getName()) &&
							 * !"Schrein der Inspiration".equals(b.getName()) &&
							 * !"Schneekugel".equals(b.getName()) &&
							 * !"Renaissance-Villa".equals(b.getName()) &&
							 * !"Lebkuchenhaus".equals(b.getName()) &&
							 * !"Königliches Marmortor".equals(b.getName()))) { throw new
							 * IllegalArgumentException( "Expected to be a production building: " + cell); }
							 * buildings.forEach(b -> b.setName(b.getName() + " PRODUCTION")); break; case
							 * "1 T.": case "wenn motiviert": case "Liefert": case "Produziert": case "": //
							 * Ignore. break; default:
							 * 
							 * if (cell.contains("%")) { // TODO: If percentage is given, factor it in the
							 * production. Need to find // out // which columns, same problem as for
							 * set-productions. buildings.forEach(b -> b
							 * .appendSpecialProduction("TODO Beinhaltet Zufallsproduktion!"));
							 * buildings.forEach(b -> b.setName(b.getName() + " Zufallsproduktion!"));
							 * break; } throw new IllegalArgumentException("Unexpected header content: " +
							 * cell); } } // TODO: Always ignore everything else? }
							 */
						}
					} else {
						// Not <th, so we are in table data.
						String[] productionCells = buildingRows[r].split("<td");
						lastAge = "undefined";
						int c;
						for (c = 1; c < productionCells.length; c++) {
							String cell = cleanHtmlSplit(productionCells[c]);
//							System.out.println(headings.get(c - 1) + ": " + cell);
							addProductionToBuildings(buildings, headings.get(c), cell, multFactor.get(c));
						}
						if ((headings.size()) != c) {
							System.out.println("Missmatch of headings to production content!");
						}
					}
				}

				// Streams sometimes are weird...
				final int temp = i;
				buildings.forEach(b -> System.out.println(temp + ": " + b.toString()));
			}
			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int getRowspan(String cell) {
		if (cell.contains(" rowspan=\"")) {
			return Integer.parseInt(cell.split(" rowspan=\"")[1].split("\"")[0]);
		} else {
			return 1;
		}
	}

	private static int getColspan(String cell) {
		if (cell.contains(" colspan=\"")) {
			return Integer.parseInt(cell.split(" colspan=\"")[1].split("\"")[0]);
		} else {
			return 1;
		}
	}

	private static String getImageText(String cell) {
		if (cell.contains("<img ")) {
			return cell.split("alt=\"")[1].split("-")[0];
		} else {
			return null;
		}
	}

	private static boolean requiresPopulation = false;

	private static int parseInt(String intString) {
		// Replaces double dash with single dash. Since assuming the goal is to have it
		// negative, and not invert it.
		return Integer.parseInt(intString.replace("%", "").replace(" Min.", "").replace(" Std.", "").replace(" T.", "")
				.replace(".", "").replace("--", "-"));
	}

	private static String cleanHtmlSplit(String cell) {
		if (!cell.startsWith("<xx")) {
			cell = "<xx" + cell;
		}
		return cell.replaceAll("<.*?>", "").trim();
	}

	private static String lastAge;

	private static void addProductionToBuildings(List<WikiBuilding> buildings, String dataType, String data,
			double factor) {
		switch (dataType) {
		case "Zeitalter":
			// Remember last age, and assume all following calls are for that age.
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
			}
			buildings.forEach(
					b -> b.setPopulation(buildFormulaString(lastAge, b.getPopulation(), parseInt(data), factor)));
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
			// Special currently ignores age.
			// TODO: Get unit name from parsed page?
			buildings.forEach(b -> b.appendSpecialProduction(parseInt(data) * factor + "x Agent"));
			break;
		case "armyuniticons_90x90_color_guard":
			// Special currently ignores age.
			// TODO: Get unit name from parsed page?
			buildings.forEach(b -> b.appendSpecialProduction(parseInt(data) * factor + "x Fahnenwache"));
			break;
		case "armyuniticons_90x90_SpaceAgeJupiterMoon_champion":
			// Special currently ignores age.
			// TODO: Get unit name from parsed page?
			buildings.forEach(b -> b.appendSpecialProduction(parseInt(data) * factor + "x Held"));
		case "armyuniticons_90x90_military_drummer":
			// Special currently ignores age.
			// TODO: Get unit name from parsed page?
			buildings.forEach(b -> b.appendSpecialProduction(parseInt(data) * factor + "x Trommler"));
			break;
		default:
			throw new IllegalArgumentException("Unexpected type: " + dataType);
		}
	}

	// Used in formula to compare the age against.
	private static final String compareAgeTo = "$B$26";

//	private static final String compareAgeTo = "$Z$1";
	private static String buildFormulaString(String age, String currentFormula, double ageValue, double factor) {
		if (currentFormula == null || currentFormula.isEmpty()) {
			currentFormula = "=\"ERROR\"";
		}
		String valueString = converDoubleToString(ageValue);
		String factorString = "";
		if(factor != 1) {
			factorString = "*"+converDoubleToString(factor);
		}
		// TODO: Change here for possibly other formats like excel or google docs.
		// Current format: For german open office calc.
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
			return currentFormula.replace("\"" + age + "\";",
					"\"" + age + "\";" + valueString + factorString + "+");
		} else {
			return currentFormula.replace("\"ERROR\"",
					"WENN(" + compareAgeTo + "=\"" + age + "\";" + valueString + factorString + ";\"ERROR\")");
		}

	}

	private static String converDoubleToString(double number) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('\0'); // Disable thousands separator
        
        DecimalFormat decimalFormat = new DecimalFormat("0.########", symbols);
        
        return decimalFormat.format(number);
	}

	private static void addPropertiesToBuildings(List<WikiBuilding> buildings, String dataType, String data) {
		switch (dataType) {
		case "Eigenschaften":
			if (data.contains("wenn verbunden")) {
				buildings.forEach(b -> b.setNeedsStarting(true));
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
			// TODO: Does Kette also mean needs starting building? Or is the wording just
			// inconsistent?
			if (buildings.stream().anyMatch(b -> !b.isNeedsStarting())) {
				// Starting buildings are also Kette, so not correct.
//				throw new IllegalArgumentException("Expected building to already be marked as chain: " + data);
				System.out.println("Found Kette without needsStarting.");
			}
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

	private static Random random = new Random();

	// Try to not get blocked or something for making hundreds of calls per second,
	// or being a bot.
	private static void waitBetweenCalls() {
		try {
			Thread.sleep(random.nextLong(100, 500));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String fetchHtmlContent(String urlString) throws IOException {
		waitBetweenCalls();
		StringBuilder htmlContent = new StringBuilder();
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				htmlContent.append(line);
			}
		}
		return htmlContent.toString();
	}
}
