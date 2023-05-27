package adver.sarius.foe.wikiparser;

// Base structure generated with the help of ChatGPT-3
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import adver.sarius.foe.building.Street;
import adver.sarius.foe.building.WikiBuilding;

public class WebsiteParser {
	public static void main(String[] args) {
		try {
			// Fetch the HTML content of the website
			String url = "https://de.wiki.forgeofempires.com/index.php?title=Liste_besonderer_Geb%C3%A4ude";
			String htmlContent = fetchHtmlContent(url);

			// Parse the table rows within the HTML content
			String tableStartTag = "<table";
			String tableEndTag = "</table>";
			int tableStartIndex = htmlContent.indexOf(tableStartTag);
			int tableEndIndex = htmlContent.indexOf(tableEndTag, tableStartIndex);
			String tableHtml = htmlContent.substring(tableStartIndex, tableEndIndex + tableEndTag.length());
			String[] rows = tableHtml.split("<tr>");

			// Iterate over each row (skipping the first row which contains headers)
//            for (int i = 1; i < rows.length; i++) {
			for (int i = 2; i < 3; i++) {
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
				// Process each cell within the table rows
				for (String buildingRow : buildingRows) {
					String[] buildingCells = buildingRow.split("<td");

					for (String cell : buildingCells) {
						if (cell.isBlank()) {
							continue;
						}
						// Remove HTML tags and trim whitespace from the cell content
						String cellContent = cleanDataSplit(cell);
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
								buildings.forEach(b -> b.setCoins24(
										Integer.parseInt(cleanDataSplit(buildingCells[2]).replace(".", ""))));
								String[] splitted = cleanDataSplit(buildingCells[3]).replace(".", "").split(" - ");
								buildings.forEach(b -> b.setGemsMin24(Integer.parseInt(splitted[0])));
								buildings.forEach(b -> b.setGemsMax24(Integer.parseInt(splitted[1])));
							}
						} else {
							addPropertiesToBuildings(buildings, lastHeading, cellContent);
							lastHeading = null;
						}
						// Output the content of each cell to the console
						System.out.println("cell: " + cellContent);
					}
					System.out.println("end row");
				}

				buildings.forEach(System.out::println);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String cleanDataSplit(String cell) {
		return ("<td" + cell).replaceAll("<.*?>", "").trim();
	}

	private static void addPropertiesToBuildings(List<WikiBuilding> buildings, String dataType, String data) {
		switch (dataType) {
		case "Eigenschaften":
			if (data.contains("wenn verbunden")) {
				buildings.forEach(b -> b.setNeedsStarting(true));
			}
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
			buildings.forEach(b -> b.setSet(data));
			break;
		case "Eingeführt mit:":
			// Ignore.
			break;
		default:
			throw new IllegalArgumentException("Unexpected type: " + dataType);
		}
	}

	private static String fetchHtmlContent(String urlString) throws IOException {
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
