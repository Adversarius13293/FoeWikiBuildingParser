package adver.sarius.foe.wikiparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	/**
	 * Contains additional properties for building urls. For informations from the
	 * main table, or manually added data.
	 */
	private static final Map<String, Map<String, String>> additionalProperties = new HashMap<>();

	// TODO: Parse maybe even military buildings?
	// TODO: Some hard coded building properties? Like settlement and GEX buildings?
	// Fountain probabilities?
	// TODO: Pass down the building type from main table, if available.
	// TODO: Differentiate between goods productions? random ones, of one type,
	// equal of each.
	// TODO: Which buildings need to be connected to the street for their boosts?
	// TODO: We are getting more and more fragments. Find a better way than "special
	// production" to store and display them?
	public static void main(String[] args) {
		var allBuildings = new ArrayList<WikiBuilding>();
		var buildingUrls = new ArrayList<String>();

		testManualEdgeCaseBuildings();

//		buildingUrls.addAll(getBuildingUrls(specialBuildingsPage));
//		buildingUrls.addAll(getBuildingUrls(special2BuildingsPage));
//		buildingUrls.addAll(getBuildingUrls(limitedBuildingsPage));

		for (int i = 0; i < buildingUrls.size(); i++) {
			List<WikiBuilding> buildings = processBuildingWebSite(buildingUrls.get(i));
			// For easier debugging. Output each building when processed, include its row.
			final int temp = i;
			buildings.forEach(WebsiteParser::simplifyBuildingFormulas);
			buildings.forEach(b -> System.out.println(temp + ": " + b.toString()));
			allBuildings.addAll(buildings);
		}
		System.out.println("Done");
		// No real need to filter out buildings afterwards? Can just do that in the
		// resulting document itself.
//			outputBuildings(allBuildings);
	}

	/**
	 * Test the results of parsed building pages against expected results. Will
	 * throw exception if the results don't match.
	 */
	private static void testManualEdgeCaseBuildings() {
		// TODO: How to integrate building type from main page?
		System.out.println("Start doing test cases.");
		Map<String, List<String>> toTest = getManualEdgeCaseBuildings();

		for (String url : toTest.keySet()) {
			List<WikiBuilding> buildings = processBuildingWebSite(url);
			buildings.forEach(WebsiteParser::simplifyBuildingFormulas);

			List<String> buildingResult = buildings.stream().map(WikiBuilding::toString).toList();
			List<String> expectedResult = toTest.get(url);
			if (!expectedResult.equals(buildingResult)) {
				System.out.println("Expected:");
				expectedResult.forEach(System.out::println);
				System.out.println("Parsed:");
				buildingResult.forEach(System.out::println);
				throw new IllegalArgumentException("Resulting string does not match the expected result.");
			}
		}
		System.out.println("Done doing test cases.");
	}

	/**
	 * @return Urls and resulting strings of manually selected buildings that tend
	 *         to make problems.
	 */
	private static Map<String, List<String>> getManualEdgeCaseBuildings() {
		var buildings = new HashMap<String, List<String>>();

		// Percentages with random production.
		buildings.put(wikiUrl + "Druidentempel_-_St._10", Arrays.asList(
				"Druidentempel - St. 10|Wohngebäude|1x1|5|4|||16200|21|32|false||=WENN($AO$1=\"Bronzezeit\";230;WENN($AO$1=\"Eisenzeit\";490;WENN($AO$1=\"Frühes Mittelalter\";760;WENN($AO$1=\"Hochmittelalter\";1060;WENN($AO$1=\"Spätes Mittelalter\";1390;WENN($AO$1=\"Kolonialzeit\";1760;WENN($AO$1=\"Industriezeitalter\";2150;WENN($AO$1=\"Jahrhundertwende\";2570;WENN($AO$1=\"Die Moderne\";3010;WENN($AO$1=\"Die Postmoderne\";3470;WENN($AO$1=\"Gegenwart\";3950;WENN($AO$1=\"Morgen\";4450;WENN($AO$1=\"Die Zukunft\";4980;WENN($AO$1=\"Arktische Zukunft\";5530;WENN($AO$1=\"Ozeanische Zukunft\";6090;WENN($AO$1=\"Virtuelle Zukunft\";6660;WENN($AO$1=\"Raumfahrt: Mars\";10550;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";11460;WENN($AO$1=\"Raumfahrt: Venus\";12380;WENN($AO$1=\"Raumfahrt: Jupitermond\";13340;\"ERROR\"))))))))))))))))))))||=WENN($AO$1=\"Bronzezeit\";14;WENN($AO$1=\"Eisenzeit\";14;WENN($AO$1=\"Frühes Mittelalter\";14;WENN($AO$1=\"Hochmittelalter\";16;WENN($AO$1=\"Spätes Mittelalter\";16;WENN($AO$1=\"Kolonialzeit\";16;WENN($AO$1=\"Industriezeitalter\";18;WENN($AO$1=\"Jahrhundertwende\";18;WENN($AO$1=\"Die Moderne\";20;WENN($AO$1=\"Die Postmoderne\";20;WENN($AO$1=\"Gegenwart\";22;WENN($AO$1=\"Morgen\";22;WENN($AO$1=\"Die Zukunft\";24;WENN($AO$1=\"Arktische Zukunft\";24;WENN($AO$1=\"Ozeanische Zukunft\";26;WENN($AO$1=\"Virtuelle Zukunft\";26;WENN($AO$1=\"Raumfahrt: Mars\";28;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";28;WENN($AO$1=\"Raumfahrt: Venus\";28;WENN($AO$1=\"Raumfahrt: Jupitermond\";28;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";100;WENN($AO$1=\"Eisenzeit\";160;WENN($AO$1=\"Frühes Mittelalter\";400;WENN($AO$1=\"Hochmittelalter\";800;WENN($AO$1=\"Spätes Mittelalter\";1200;WENN($AO$1=\"Kolonialzeit\";2000;WENN($AO$1=\"Industriezeitalter\";4000;WENN($AO$1=\"Jahrhundertwende\";6000;WENN($AO$1=\"Die Moderne\";10000;WENN($AO$1=\"Die Postmoderne\";16000;WENN($AO$1=\"Gegenwart\";24000;WENN($AO$1=\"Morgen\";40000;WENN($AO$1=\"Die Zukunft\";64000;WENN($AO$1=\"Arktische Zukunft\";96000;WENN($AO$1=\"Ozeanische Zukunft\";120000;WENN($AO$1=\"Virtuelle Zukunft\";160000;WENN($AO$1=\"Raumfahrt: Mars\";260000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";360000;WENN($AO$1=\"Raumfahrt: Venus\";420000;WENN($AO$1=\"Raumfahrt: Jupitermond\";480000;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";1500;WENN($AO$1=\"Eisenzeit\";3600;WENN($AO$1=\"Frühes Mittelalter\";6000;WENN($AO$1=\"Hochmittelalter\";8400;WENN($AO$1=\"Spätes Mittelalter\";11100;WENN($AO$1=\"Kolonialzeit\";16900;WENN($AO$1=\"Industriezeitalter\";21100;WENN($AO$1=\"Jahrhundertwende\";25600;WENN($AO$1=\"Die Moderne\";31800;WENN($AO$1=\"Die Postmoderne\";40000;WENN($AO$1=\"Gegenwart\";52100;WENN($AO$1=\"Morgen\";60300;WENN($AO$1=\"Die Zukunft\";69100;WENN($AO$1=\"Arktische Zukunft\";78500;WENN($AO$1=\"Ozeanische Zukunft\";88600;WENN($AO$1=\"Virtuelle Zukunft\";99200;WENN($AO$1=\"Raumfahrt: Mars\";165700;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";183500;WENN($AO$1=\"Raumfahrt: Venus\";202300;WENN($AO$1=\"Raumfahrt: Jupitermond\";222100;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";1250*0,05;WENN($AO$1=\"Eisenzeit\";5000*0,05;WENN($AO$1=\"Frühes Mittelalter\";10000*0,05;WENN($AO$1=\"Hochmittelalter\";17500*0,05;WENN($AO$1=\"Spätes Mittelalter\";27500*0,05;WENN($AO$1=\"Kolonialzeit\";38750*0,05;WENN($AO$1=\"Industriezeitalter\";52500*0,05;WENN($AO$1=\"Jahrhundertwende\";67500*0,05;WENN($AO$1=\"Die Moderne\";85000*0,05;WENN($AO$1=\"Die Postmoderne\";103750*0,05;WENN($AO$1=\"Gegenwart\";125000*0,05;WENN($AO$1=\"Morgen\";147500*0,05;WENN($AO$1=\"Die Zukunft\";172500*0,05;WENN($AO$1=\"Arktische Zukunft\";198750*0,05;WENN($AO$1=\"Ozeanische Zukunft\";226250*0,05;WENN($AO$1=\"Virtuelle Zukunft\";256250*0,05;WENN($AO$1=\"Raumfahrt: Mars\";287500*0,05;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";321250*0,05;WENN($AO$1=\"Raumfahrt: Venus\";356250*0,05;WENN($AO$1=\"Raumfahrt: Jupitermond\";393750*0,05;\"ERROR\"))))))))))))))))))))||=WENN($AO$1=\"Bronzezeit\";150*0,15;WENN($AO$1=\"Eisenzeit\";600*0,15;WENN($AO$1=\"Frühes Mittelalter\";1050*0,15;WENN($AO$1=\"Hochmittelalter\";1800*0,15;WENN($AO$1=\"Spätes Mittelalter\";2700*0,15;WENN($AO$1=\"Kolonialzeit\";3900*0,15;WENN($AO$1=\"Industriezeitalter\";5100*0,15;WENN($AO$1=\"Jahrhundertwende\";6450*0,15;WENN($AO$1=\"Die Moderne\";7950*0,15;WENN($AO$1=\"Die Postmoderne\";9750*0,15;WENN($AO$1=\"Gegenwart\";11550*0,15;WENN($AO$1=\"Morgen\";13500*0,15;WENN($AO$1=\"Die Zukunft\";15600*0,15;WENN($AO$1=\"Arktische Zukunft\";17850*0,15;WENN($AO$1=\"Ozeanische Zukunft\";20250*0,15;WENN($AO$1=\"Virtuelle Zukunft\";22650*0,15;WENN($AO$1=\"Raumfahrt: Mars\";25350*0,15;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";28050*0,15;WENN($AO$1=\"Raumfahrt: Venus\";30900*0,15;WENN($AO$1=\"Raumfahrt: Jupitermond\";33900*0,15;\"ERROR\"))))))))))))))))))))|=25*0,4+20||||=25*0,4||Zufallsproduktion!;Einberechnete Zufallsproduktion!|false|true"));
		// No properties text.
		buildings.put(wikiUrl + "Kloster",
				Arrays.asList("Kloster|Turm|1x1|3|3|||2030|0|1|false||||||=20|=180||||||||||||||false|true"));
		// Upgradeable, no age column.
		buildings.put(wikiUrl + "Agenten-Versteck", Arrays.asList(
				"Agenten-Versteck|Militärgebäude|1x1|2|3|||6080|5|9|false|||||||=240|||||||||||||1x Agent|true|true"));
		// Production building, requires population.
		buildings.put(wikiUrl + "Aviarium", Arrays.asList(
				"Aviarium|Produktionsstätten|1x1|3|4|||6480|8|13|false|=WENN($AO$1=\"Bronzezeit\";76;WENN($AO$1=\"Eisenzeit\";163;WENN($AO$1=\"Frühes Mittelalter\";191;WENN($AO$1=\"Hochmittelalter\";253;WENN($AO$1=\"Spätes Mittelalter\";320;WENN($AO$1=\"Kolonialzeit\";377;WENN($AO$1=\"Industriezeitalter\";425;WENN($AO$1=\"Jahrhundertwende\";467;WENN($AO$1=\"Die Moderne\";519;WENN($AO$1=\"Die Postmoderne\";520;WENN($AO$1=\"Gegenwart\";665;WENN($AO$1=\"Morgen\";807;WENN($AO$1=\"Die Zukunft\";967;WENN($AO$1=\"Arktische Zukunft\";1024;WENN($AO$1=\"Ozeanische Zukunft\";1181;WENN($AO$1=\"Virtuelle Zukunft\";1411;WENN($AO$1=\"Raumfahrt: Mars\";2258;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";2454;WENN($AO$1=\"Raumfahrt: Venus\";2651;WENN($AO$1=\"Raumfahrt: Jupitermond\";2856;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";-50;WENN($AO$1=\"Eisenzeit\";-110;WENN($AO$1=\"Frühes Mittelalter\";-150;WENN($AO$1=\"Hochmittelalter\";-200;WENN($AO$1=\"Spätes Mittelalter\";-250;WENN($AO$1=\"Kolonialzeit\";-290;WENN($AO$1=\"Industriezeitalter\";-340;WENN($AO$1=\"Jahrhundertwende\";-380;WENN($AO$1=\"Die Moderne\";-420;WENN($AO$1=\"Die Postmoderne\";-460;WENN($AO$1=\"Gegenwart\";-500;WENN($AO$1=\"Morgen\";-540;WENN($AO$1=\"Die Zukunft\";-600;WENN($AO$1=\"Arktische Zukunft\";-640;WENN($AO$1=\"Ozeanische Zukunft\";-680;WENN($AO$1=\"Virtuelle Zukunft\";-720;WENN($AO$1=\"Raumfahrt: Mars\";-1090;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";-1150;WENN($AO$1=\"Raumfahrt: Venus\";-1200;WENN($AO$1=\"Raumfahrt: Jupitermond\";-1250;\"ERROR\"))))))))))))))))))))|||||=WENN($AO$1=\"Bronzezeit\";60;WENN($AO$1=\"Eisenzeit\";96;WENN($AO$1=\"Frühes Mittelalter\";240;WENN($AO$1=\"Hochmittelalter\";480;WENN($AO$1=\"Spätes Mittelalter\";720;WENN($AO$1=\"Kolonialzeit\";1200;WENN($AO$1=\"Industriezeitalter\";2400;WENN($AO$1=\"Jahrhundertwende\";3600;WENN($AO$1=\"Die Moderne\";6000;WENN($AO$1=\"Die Postmoderne\";9600;WENN($AO$1=\"Gegenwart\";14400;WENN($AO$1=\"Morgen\";24000;WENN($AO$1=\"Die Zukunft\";38400;WENN($AO$1=\"Arktische Zukunft\";57600;WENN($AO$1=\"Ozeanische Zukunft\";72000;WENN($AO$1=\"Virtuelle Zukunft\";96000;WENN($AO$1=\"Raumfahrt: Mars\";156000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";216000;WENN($AO$1=\"Raumfahrt: Venus\";252000;WENN($AO$1=\"Raumfahrt: Jupitermond\";288000;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";270*3;WENN($AO$1=\"Eisenzeit\";580*3;WENN($AO$1=\"Frühes Mittelalter\";960*3;WENN($AO$1=\"Hochmittelalter\";1340*3;WENN($AO$1=\"Spätes Mittelalter\";1670*3;WENN($AO$1=\"Kolonialzeit\";2030*3;WENN($AO$1=\"Industriezeitalter\";2420*3;WENN($AO$1=\"Jahrhundertwende\";2810*3;WENN($AO$1=\"Die Moderne\";3500*3;WENN($AO$1=\"Die Postmoderne\";4400*3;WENN($AO$1=\"Gegenwart\";5730*3;WENN($AO$1=\"Morgen\";6630*3;WENN($AO$1=\"Die Zukunft\";7600*3;WENN($AO$1=\"Arktische Zukunft\";8640*3;WENN($AO$1=\"Ozeanische Zukunft\";9740*3;WENN($AO$1=\"Virtuelle Zukunft\";10910*3;WENN($AO$1=\"Raumfahrt: Mars\";18230*3;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";20190*3;WENN($AO$1=\"Raumfahrt: Venus\";22250*3;WENN($AO$1=\"Raumfahrt: Jupitermond\";24420*3;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";180*6+43*96+13*288;WENN($AO$1=\"Eisenzeit\";432*6+102*96+31*288;WENN($AO$1=\"Frühes Mittelalter\";720*6+170*96+52*288;WENN($AO$1=\"Hochmittelalter\";1008*6+238*96+73*288;WENN($AO$1=\"Spätes Mittelalter\";1332*6+315*96+96*288;WENN($AO$1=\"Kolonialzeit\";1692*6+400*96+122*288;WENN($AO$1=\"Industriezeitalter\";1938*6+485*96+148*288;WENN($AO$1=\"Jahrhundertwende\";2278*6+570*96+174*288;WENN($AO$1=\"Die Moderne\";2652*6+663*96+203*288;WENN($AO$1=\"Die Postmoderne\";3026*6+757*96+231*288;WENN($AO$1=\"Gegenwart\";3400*6+850*96+260*288;WENN($AO$1=\"Morgen\";3808*6+952*96+291*288;WENN($AO$1=\"Die Zukunft\";4182*6+1046*96+320*288;WENN($AO$1=\"Arktische Zukunft\";4590*6+1148*96+351*288;WENN($AO$1=\"Ozeanische Zukunft\";5032*6+1258*96+385*288;WENN($AO$1=\"Virtuelle Zukunft\";5440*6+1360*96+416*288;WENN($AO$1=\"Raumfahrt: Mars\";8806*6+2202*96+673*288;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";9452*6+2363*96+723*288;WENN($AO$1=\"Raumfahrt: Venus\";10132*6+2533*96+775*288;WENN($AO$1=\"Raumfahrt: Jupitermond\";10778*6+2695*96+824*288;\"ERROR\"))))))))))))))))))))||=WENN($AO$1=\"Bronzezeit\";1*24;WENN($AO$1=\"Eisenzeit\";1*24;WENN($AO$1=\"Frühes Mittelalter\";1*24;WENN($AO$1=\"Hochmittelalter\";2*24;WENN($AO$1=\"Spätes Mittelalter\";2*24;WENN($AO$1=\"Kolonialzeit\";2*24;WENN($AO$1=\"Industriezeitalter\";3*24;WENN($AO$1=\"Jahrhundertwende\";3*24;WENN($AO$1=\"Die Moderne\";5*24;WENN($AO$1=\"Die Postmoderne\";6*24;WENN($AO$1=\"Gegenwart\";9*24;WENN($AO$1=\"Morgen\";12*24;WENN($AO$1=\"Die Zukunft\";15*24;WENN($AO$1=\"Arktische Zukunft\";18*24;WENN($AO$1=\"Ozeanische Zukunft\";20*24;WENN($AO$1=\"Virtuelle Zukunft\";24*24;WENN($AO$1=\"Raumfahrt: Mars\";30*24;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";35*24;WENN($AO$1=\"Raumfahrt: Venus\";40*24;WENN($AO$1=\"Raumfahrt: Jupitermond\";45*24;\"ERROR\"))))))))))))))))))))|||||=2||Produktion auf 24 Stunden gerechnet!|false|true"));
		// PRoduces special military unit.
		buildings.put(wikiUrl + "Fahnenwachen-Camp", Arrays.asList(
				"Fahnenwachen-Camp|Militärgebäude|1x1|4|4|||2430|0|2|false||=-150|||||=640|||||||||||||1x Fahnenwache|false|true"));
		// Chain building.
		buildings.put(wikiUrl + "Fischerpier", Arrays.asList(
				"Fischerpier|Dekorationen|Keine Straße benötigt|5|2||Piratenversteck|9720|8|13|true|=WENN($AO$1=\"Bronzezeit\";330;WENN($AO$1=\"Eisenzeit\";470;WENN($AO$1=\"Frühes Mittelalter\";560;WENN($AO$1=\"Hochmittelalter\";690;WENN($AO$1=\"Spätes Mittelalter\";800;WENN($AO$1=\"Kolonialzeit\";940;WENN($AO$1=\"Industriezeitalter\";1060;WENN($AO$1=\"Jahrhundertwende\";1170;WENN($AO$1=\"Die Moderne\";1330;WENN($AO$1=\"Die Postmoderne\";1520;WENN($AO$1=\"Gegenwart\";1760;WENN($AO$1=\"Morgen\";2200;WENN($AO$1=\"Die Zukunft\";2640;WENN($AO$1=\"Arktische Zukunft\";2990;WENN($AO$1=\"Ozeanische Zukunft\";3350;WENN($AO$1=\"Virtuelle Zukunft\";4230;WENN($AO$1=\"Raumfahrt: Mars\";6590;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";7160;WENN($AO$1=\"Raumfahrt: Venus\";7730;WENN($AO$1=\"Raumfahrt: Jupitermond\";8330;\"ERROR\"))))))))))))))))))))||||||=WENN($AO$1=\"Bronzezeit\";50;WENN($AO$1=\"Eisenzeit\";80;WENN($AO$1=\"Frühes Mittelalter\";200;WENN($AO$1=\"Hochmittelalter\";400;WENN($AO$1=\"Spätes Mittelalter\";600;WENN($AO$1=\"Kolonialzeit\";1000;WENN($AO$1=\"Industriezeitalter\";2000;WENN($AO$1=\"Jahrhundertwende\";3000;WENN($AO$1=\"Die Moderne\";5000;WENN($AO$1=\"Die Postmoderne\";8000;WENN($AO$1=\"Gegenwart\";12000;WENN($AO$1=\"Morgen\";20000;WENN($AO$1=\"Die Zukunft\";32000;WENN($AO$1=\"Arktische Zukunft\";48000;WENN($AO$1=\"Ozeanische Zukunft\";60000;WENN($AO$1=\"Virtuelle Zukunft\";80000;WENN($AO$1=\"Raumfahrt: Mars\";130000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";180000;WENN($AO$1=\"Raumfahrt: Venus\";210000;WENN($AO$1=\"Raumfahrt: Jupitermond\";240000;\"ERROR\"))))))))))))))))))))|||||||||||=4|||false|true"));
		// Negative happiness value.
		buildings.put(wikiUrl + "Haus_des_Wolfs", Arrays.asList(
				"Haus des Wolfs|Wohngebäude|1x1|3|4|||14180|18|28|false|=WENN($AO$1=\"Bronzezeit\";-50;WENN($AO$1=\"Eisenzeit\";-90;WENN($AO$1=\"Frühes Mittelalter\";-100;WENN($AO$1=\"Hochmittelalter\";-110;WENN($AO$1=\"Spätes Mittelalter\";-130;WENN($AO$1=\"Kolonialzeit\";-150;WENN($AO$1=\"Industriezeitalter\";-170;WENN($AO$1=\"Jahrhundertwende\";-190;WENN($AO$1=\"Die Moderne\";-200;WENN($AO$1=\"Die Postmoderne\";-210;WENN($AO$1=\"Gegenwart\";-240;WENN($AO$1=\"Morgen\";-290;WENN($AO$1=\"Die Zukunft\";-350;WENN($AO$1=\"Arktische Zukunft\";-410;WENN($AO$1=\"Ozeanische Zukunft\";-470;WENN($AO$1=\"Virtuelle Zukunft\";-560;WENN($AO$1=\"Raumfahrt: Mars\";-900;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";-980;WENN($AO$1=\"Raumfahrt: Venus\";-1060;WENN($AO$1=\"Raumfahrt: Jupitermond\";-1140;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";97;WENN($AO$1=\"Eisenzeit\";209;WENN($AO$1=\"Frühes Mittelalter\";320;WENN($AO$1=\"Hochmittelalter\";450;WENN($AO$1=\"Spätes Mittelalter\";590;WENN($AO$1=\"Kolonialzeit\";745;WENN($AO$1=\"Industriezeitalter\";911;WENN($AO$1=\"Jahrhundertwende\";1087;WENN($AO$1=\"Die Moderne\";1274;WENN($AO$1=\"Die Postmoderne\";1469;WENN($AO$1=\"Gegenwart\";1674;WENN($AO$1=\"Morgen\";1886;WENN($AO$1=\"Die Zukunft\";2110;WENN($AO$1=\"Arktische Zukunft\";2340;WENN($AO$1=\"Ozeanische Zukunft\";2578;WENN($AO$1=\"Virtuelle Zukunft\";2822;WENN($AO$1=\"Raumfahrt: Mars\";4468;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";4853;WENN($AO$1=\"Raumfahrt: Venus\";5245;WENN($AO$1=\"Raumfahrt: Jupitermond\";5648;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";6;WENN($AO$1=\"Eisenzeit\";6;WENN($AO$1=\"Frühes Mittelalter\";7;WENN($AO$1=\"Hochmittelalter\";7;WENN($AO$1=\"Spätes Mittelalter\";8;WENN($AO$1=\"Kolonialzeit\";8;WENN($AO$1=\"Industriezeitalter\";9;WENN($AO$1=\"Jahrhundertwende\";9;WENN($AO$1=\"Die Moderne\";10;WENN($AO$1=\"Die Postmoderne\";10;WENN($AO$1=\"Gegenwart\";11;WENN($AO$1=\"Morgen\";11;WENN($AO$1=\"Die Zukunft\";12;WENN($AO$1=\"Arktische Zukunft\";12;WENN($AO$1=\"Ozeanische Zukunft\";13;WENN($AO$1=\"Virtuelle Zukunft\";13;WENN($AO$1=\"Raumfahrt: Mars\";14;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";14;WENN($AO$1=\"Raumfahrt: Venus\";15;WENN($AO$1=\"Raumfahrt: Jupitermond\";15;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";6;WENN($AO$1=\"Eisenzeit\";6;WENN($AO$1=\"Frühes Mittelalter\";7;WENN($AO$1=\"Hochmittelalter\";7;WENN($AO$1=\"Spätes Mittelalter\";8;WENN($AO$1=\"Kolonialzeit\";8;WENN($AO$1=\"Industriezeitalter\";9;WENN($AO$1=\"Jahrhundertwende\";9;WENN($AO$1=\"Die Moderne\";10;WENN($AO$1=\"Die Postmoderne\";10;WENN($AO$1=\"Gegenwart\";11;WENN($AO$1=\"Morgen\";11;WENN($AO$1=\"Die Zukunft\";12;WENN($AO$1=\"Arktische Zukunft\";12;WENN($AO$1=\"Ozeanische Zukunft\";13;WENN($AO$1=\"Virtuelle Zukunft\";13;WENN($AO$1=\"Raumfahrt: Mars\";14;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";14;WENN($AO$1=\"Raumfahrt: Venus\";15;WENN($AO$1=\"Raumfahrt: Jupitermond\";15;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";60;WENN($AO$1=\"Eisenzeit\";96;WENN($AO$1=\"Frühes Mittelalter\";240;WENN($AO$1=\"Hochmittelalter\";480;WENN($AO$1=\"Spätes Mittelalter\";720;WENN($AO$1=\"Kolonialzeit\";1200;WENN($AO$1=\"Industriezeitalter\";2400;WENN($AO$1=\"Jahrhundertwende\";3600;WENN($AO$1=\"Die Moderne\";6000;WENN($AO$1=\"Die Postmoderne\";9600;WENN($AO$1=\"Gegenwart\";14400;WENN($AO$1=\"Morgen\";24000;WENN($AO$1=\"Die Zukunft\";38400;WENN($AO$1=\"Arktische Zukunft\";57600;WENN($AO$1=\"Ozeanische Zukunft\";72000;WENN($AO$1=\"Virtuelle Zukunft\";96000;WENN($AO$1=\"Raumfahrt: Mars\";156000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";216000;WENN($AO$1=\"Raumfahrt: Venus\";252000;WENN($AO$1=\"Raumfahrt: Jupitermond\";288000;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";900;WENN($AO$1=\"Eisenzeit\";2200;WENN($AO$1=\"Frühes Mittelalter\";3600;WENN($AO$1=\"Hochmittelalter\";5000;WENN($AO$1=\"Spätes Mittelalter\";6700;WENN($AO$1=\"Kolonialzeit\";10100;WENN($AO$1=\"Industriezeitalter\";12600;WENN($AO$1=\"Jahrhundertwende\";15300;WENN($AO$1=\"Die Moderne\";19100;WENN($AO$1=\"Die Postmoderne\";24000;WENN($AO$1=\"Gegenwart\";31300;WENN($AO$1=\"Morgen\";36200;WENN($AO$1=\"Die Zukunft\";41500;WENN($AO$1=\"Arktische Zukunft\";47100;WENN($AO$1=\"Ozeanische Zukunft\";53100;WENN($AO$1=\"Virtuelle Zukunft\";59500;WENN($AO$1=\"Raumfahrt: Mars\";99400;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";110100;WENN($AO$1=\"Raumfahrt: Venus\";121400;WENN($AO$1=\"Raumfahrt: Jupitermond\";133200;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";840;WENN($AO$1=\"Eisenzeit\";1440;WENN($AO$1=\"Frühes Mittelalter\";1600;WENN($AO$1=\"Hochmittelalter\";1780;WENN($AO$1=\"Spätes Mittelalter\";1990;WENN($AO$1=\"Kolonialzeit\";2340;WENN($AO$1=\"Industriezeitalter\";2630;WENN($AO$1=\"Jahrhundertwende\";2890;WENN($AO$1=\"Die Moderne\";3100;WENN($AO$1=\"Die Postmoderne\";3230;WENN($AO$1=\"Gegenwart\";3750;WENN($AO$1=\"Morgen\";4550;WENN($AO$1=\"Die Zukunft\";5450;WENN($AO$1=\"Arktische Zukunft\";6350;WENN($AO$1=\"Ozeanische Zukunft\";7320;WENN($AO$1=\"Virtuelle Zukunft\";8750;WENN($AO$1=\"Raumfahrt: Mars\";14000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";15220;WENN($AO$1=\"Raumfahrt: Venus\";16440;WENN($AO$1=\"Raumfahrt: Jupitermond\";17710;\"ERROR\"))))))))))))))))))))|||=5||||=4|||false|true"));
		// Set productions in the middle.
		buildings.put(wikiUrl + "Lussebullar-Bäckerei", Arrays.asList(
				"Lussebullar-Bäckerei|Wohngebäude|1x1|3|5||Winterbäckerei-Set|14580|19|29|false|=WENN($AO$1=\"Bronzezeit\";1170;WENN($AO$1=\"Eisenzeit\";2020;WENN($AO$1=\"Frühes Mittelalter\";2230;WENN($AO$1=\"Hochmittelalter\";2490;WENN($AO$1=\"Spätes Mittelalter\";2780;WENN($AO$1=\"Kolonialzeit\";3270;WENN($AO$1=\"Industriezeitalter\";3680;WENN($AO$1=\"Jahrhundertwende\";4040;WENN($AO$1=\"Die Moderne\";4330;WENN($AO$1=\"Die Postmoderne\";4510;WENN($AO$1=\"Gegenwart\";5240;WENN($AO$1=\"Morgen\";6360;WENN($AO$1=\"Die Zukunft\";7620;WENN($AO$1=\"Arktische Zukunft\";8870;WENN($AO$1=\"Ozeanische Zukunft\";10240;WENN($AO$1=\"Virtuelle Zukunft\";12230;WENN($AO$1=\"Raumfahrt: Mars\";19570;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";21270;WENN($AO$1=\"Raumfahrt: Venus\";22980;WENN($AO$1=\"Raumfahrt: Jupitermond\";24750;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";216;WENN($AO$1=\"Eisenzeit\";464;WENN($AO$1=\"Frühes Mittelalter\";712;WENN($AO$1=\"Hochmittelalter\";1000;WENN($AO$1=\"Spätes Mittelalter\";1312;WENN($AO$1=\"Kolonialzeit\";1656;WENN($AO$1=\"Industriezeitalter\";2024;WENN($AO$1=\"Jahrhundertwende\";2416;WENN($AO$1=\"Die Moderne\";2832;WENN($AO$1=\"Die Postmoderne\";3264;WENN($AO$1=\"Gegenwart\";3720;WENN($AO$1=\"Morgen\";4192;WENN($AO$1=\"Die Zukunft\";4688;WENN($AO$1=\"Arktische Zukunft\";5200;WENN($AO$1=\"Ozeanische Zukunft\";5728;WENN($AO$1=\"Virtuelle Zukunft\";6272;WENN($AO$1=\"Raumfahrt: Mars\";9928;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";10784;WENN($AO$1=\"Raumfahrt: Venus\";11656;WENN($AO$1=\"Raumfahrt: Jupitermond\";12552;\"ERROR\"))))))))))))))))))))|||||=WENN($AO$1=\"Bronzezeit\";75;WENN($AO$1=\"Eisenzeit\";120;WENN($AO$1=\"Frühes Mittelalter\";300;WENN($AO$1=\"Hochmittelalter\";600;WENN($AO$1=\"Spätes Mittelalter\";900;WENN($AO$1=\"Kolonialzeit\";1500;WENN($AO$1=\"Industriezeitalter\";3000;WENN($AO$1=\"Jahrhundertwende\";4500;WENN($AO$1=\"Die Moderne\";7500;WENN($AO$1=\"Die Postmoderne\";12000;WENN($AO$1=\"Gegenwart\";18000;WENN($AO$1=\"Morgen\";30000;WENN($AO$1=\"Die Zukunft\";48000;WENN($AO$1=\"Arktische Zukunft\";72000;WENN($AO$1=\"Ozeanische Zukunft\";90000;WENN($AO$1=\"Virtuelle Zukunft\";120000;WENN($AO$1=\"Raumfahrt: Mars\";195000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";270000;WENN($AO$1=\"Raumfahrt: Venus\";315000;WENN($AO$1=\"Raumfahrt: Jupitermond\";360000;\"ERROR\"))))))))))))))))))))||=25|=WENN($AO$1=\"Bronzezeit\";1600;WENN($AO$1=\"Eisenzeit\";3700;WENN($AO$1=\"Frühes Mittelalter\";6200;WENN($AO$1=\"Hochmittelalter\";8700;WENN($AO$1=\"Spätes Mittelalter\";11500;WENN($AO$1=\"Kolonialzeit\";17500;WENN($AO$1=\"Industriezeitalter\";21800;WENN($AO$1=\"Jahrhundertwende\";26400;WENN($AO$1=\"Die Moderne\";32900;WENN($AO$1=\"Die Postmoderne\";41300;WENN($AO$1=\"Gegenwart\";53800;WENN($AO$1=\"Morgen\";62300;WENN($AO$1=\"Die Zukunft\";71400;WENN($AO$1=\"Arktische Zukunft\";81200;WENN($AO$1=\"Ozeanische Zukunft\";91500;WENN($AO$1=\"Virtuelle Zukunft\";102500;WENN($AO$1=\"Raumfahrt: Mars\";171200;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";189600;WENN($AO$1=\"Raumfahrt: Venus\";209100;WENN($AO$1=\"Raumfahrt: Jupitermond\";229500;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";1530;WENN($AO$1=\"Eisenzeit\";2640;WENN($AO$1=\"Frühes Mittelalter\";2920;WENN($AO$1=\"Hochmittelalter\";3260;WENN($AO$1=\"Spätes Mittelalter\";3630;WENN($AO$1=\"Kolonialzeit\";4270;WENN($AO$1=\"Industriezeitalter\";4820;WENN($AO$1=\"Jahrhundertwende\";5290;WENN($AO$1=\"Die Moderne\";5660;WENN($AO$1=\"Die Postmoderne\";5900;WENN($AO$1=\"Gegenwart\";6850;WENN($AO$1=\"Morgen\";8320;WENN($AO$1=\"Die Zukunft\";9960;WENN($AO$1=\"Arktische Zukunft\";11600;WENN($AO$1=\"Ozeanische Zukunft\";13390;WENN($AO$1=\"Virtuelle Zukunft\";15990;WENN($AO$1=\"Raumfahrt: Mars\";25590;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";27810;WENN($AO$1=\"Raumfahrt: Venus\";30050;WENN($AO$1=\"Raumfahrt: Jupitermond\";32370;\"ERROR\"))))))))))))))))))))|||=10||||=5|||false|false",
				"Lussebullar-Bäckerei [1 x Set]|Wohngebäude|1x1|3|5||Winterbäckerei-Set|14580|19|29|false|=WENN($AO$1=\"Bronzezeit\";1170;WENN($AO$1=\"Eisenzeit\";2020;WENN($AO$1=\"Frühes Mittelalter\";2230;WENN($AO$1=\"Hochmittelalter\";2490;WENN($AO$1=\"Spätes Mittelalter\";2780;WENN($AO$1=\"Kolonialzeit\";3270;WENN($AO$1=\"Industriezeitalter\";3680;WENN($AO$1=\"Jahrhundertwende\";4040;WENN($AO$1=\"Die Moderne\";4330;WENN($AO$1=\"Die Postmoderne\";4510;WENN($AO$1=\"Gegenwart\";5240;WENN($AO$1=\"Morgen\";6360;WENN($AO$1=\"Die Zukunft\";7620;WENN($AO$1=\"Arktische Zukunft\";8870;WENN($AO$1=\"Ozeanische Zukunft\";10240;WENN($AO$1=\"Virtuelle Zukunft\";12230;WENN($AO$1=\"Raumfahrt: Mars\";19570;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";21270;WENN($AO$1=\"Raumfahrt: Venus\";22980;WENN($AO$1=\"Raumfahrt: Jupitermond\";24750;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";216;WENN($AO$1=\"Eisenzeit\";464;WENN($AO$1=\"Frühes Mittelalter\";712;WENN($AO$1=\"Hochmittelalter\";1000;WENN($AO$1=\"Spätes Mittelalter\";1312;WENN($AO$1=\"Kolonialzeit\";1656;WENN($AO$1=\"Industriezeitalter\";2024;WENN($AO$1=\"Jahrhundertwende\";2416;WENN($AO$1=\"Die Moderne\";2832;WENN($AO$1=\"Die Postmoderne\";3264;WENN($AO$1=\"Gegenwart\";3720;WENN($AO$1=\"Morgen\";4192;WENN($AO$1=\"Die Zukunft\";4688;WENN($AO$1=\"Arktische Zukunft\";5200;WENN($AO$1=\"Ozeanische Zukunft\";5728;WENN($AO$1=\"Virtuelle Zukunft\";6272;WENN($AO$1=\"Raumfahrt: Mars\";9928;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";10784;WENN($AO$1=\"Raumfahrt: Venus\";11656;WENN($AO$1=\"Raumfahrt: Jupitermond\";12552;\"ERROR\"))))))))))))))))))))|||||=WENN($AO$1=\"Bronzezeit\";75;WENN($AO$1=\"Eisenzeit\";120;WENN($AO$1=\"Frühes Mittelalter\";300;WENN($AO$1=\"Hochmittelalter\";600;WENN($AO$1=\"Spätes Mittelalter\";900;WENN($AO$1=\"Kolonialzeit\";1500;WENN($AO$1=\"Industriezeitalter\";3000;WENN($AO$1=\"Jahrhundertwende\";4500;WENN($AO$1=\"Die Moderne\";7500;WENN($AO$1=\"Die Postmoderne\";12000;WENN($AO$1=\"Gegenwart\";18000;WENN($AO$1=\"Morgen\";30000;WENN($AO$1=\"Die Zukunft\";48000;WENN($AO$1=\"Arktische Zukunft\";72000;WENN($AO$1=\"Ozeanische Zukunft\";90000;WENN($AO$1=\"Virtuelle Zukunft\";120000;WENN($AO$1=\"Raumfahrt: Mars\";195000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";270000;WENN($AO$1=\"Raumfahrt: Venus\";315000;WENN($AO$1=\"Raumfahrt: Jupitermond\";360000;\"ERROR\"))))))))))))))))))))||=25|=WENN($AO$1=\"Bronzezeit\";1600;WENN($AO$1=\"Eisenzeit\";3700;WENN($AO$1=\"Frühes Mittelalter\";6200;WENN($AO$1=\"Hochmittelalter\";8700;WENN($AO$1=\"Spätes Mittelalter\";11500;WENN($AO$1=\"Kolonialzeit\";17500;WENN($AO$1=\"Industriezeitalter\";21800;WENN($AO$1=\"Jahrhundertwende\";26400;WENN($AO$1=\"Die Moderne\";32900;WENN($AO$1=\"Die Postmoderne\";41300;WENN($AO$1=\"Gegenwart\";53800;WENN($AO$1=\"Morgen\";62300;WENN($AO$1=\"Die Zukunft\";71400;WENN($AO$1=\"Arktische Zukunft\";81200;WENN($AO$1=\"Ozeanische Zukunft\";91500;WENN($AO$1=\"Virtuelle Zukunft\";102500;WENN($AO$1=\"Raumfahrt: Mars\";171200;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";189600;WENN($AO$1=\"Raumfahrt: Venus\";209100;WENN($AO$1=\"Raumfahrt: Jupitermond\";229500;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";1530;WENN($AO$1=\"Eisenzeit\";2640;WENN($AO$1=\"Frühes Mittelalter\";2920;WENN($AO$1=\"Hochmittelalter\";3260;WENN($AO$1=\"Spätes Mittelalter\";3630;WENN($AO$1=\"Kolonialzeit\";4270;WENN($AO$1=\"Industriezeitalter\";4820;WENN($AO$1=\"Jahrhundertwende\";5290;WENN($AO$1=\"Die Moderne\";5660;WENN($AO$1=\"Die Postmoderne\";5900;WENN($AO$1=\"Gegenwart\";6850;WENN($AO$1=\"Morgen\";8320;WENN($AO$1=\"Die Zukunft\";9960;WENN($AO$1=\"Arktische Zukunft\";11600;WENN($AO$1=\"Ozeanische Zukunft\";13390;WENN($AO$1=\"Virtuelle Zukunft\";15990;WENN($AO$1=\"Raumfahrt: Mars\";25590;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";27810;WENN($AO$1=\"Raumfahrt: Venus\";30050;WENN($AO$1=\"Raumfahrt: Jupitermond\";32370;\"ERROR\"))))))))))))))))))))|||=10+5||||=5|||false|false",
				"Lussebullar-Bäckerei [2 x Set]|Wohngebäude|1x1|3|5||Winterbäckerei-Set|14580|19|29|false|=WENN($AO$1=\"Bronzezeit\";1170;WENN($AO$1=\"Eisenzeit\";2020;WENN($AO$1=\"Frühes Mittelalter\";2230;WENN($AO$1=\"Hochmittelalter\";2490;WENN($AO$1=\"Spätes Mittelalter\";2780;WENN($AO$1=\"Kolonialzeit\";3270;WENN($AO$1=\"Industriezeitalter\";3680;WENN($AO$1=\"Jahrhundertwende\";4040;WENN($AO$1=\"Die Moderne\";4330;WENN($AO$1=\"Die Postmoderne\";4510;WENN($AO$1=\"Gegenwart\";5240;WENN($AO$1=\"Morgen\";6360;WENN($AO$1=\"Die Zukunft\";7620;WENN($AO$1=\"Arktische Zukunft\";8870;WENN($AO$1=\"Ozeanische Zukunft\";10240;WENN($AO$1=\"Virtuelle Zukunft\";12230;WENN($AO$1=\"Raumfahrt: Mars\";19570;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";21270;WENN($AO$1=\"Raumfahrt: Venus\";22980;WENN($AO$1=\"Raumfahrt: Jupitermond\";24750;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";216;WENN($AO$1=\"Eisenzeit\";464;WENN($AO$1=\"Frühes Mittelalter\";712;WENN($AO$1=\"Hochmittelalter\";1000;WENN($AO$1=\"Spätes Mittelalter\";1312;WENN($AO$1=\"Kolonialzeit\";1656;WENN($AO$1=\"Industriezeitalter\";2024;WENN($AO$1=\"Jahrhundertwende\";2416;WENN($AO$1=\"Die Moderne\";2832;WENN($AO$1=\"Die Postmoderne\";3264;WENN($AO$1=\"Gegenwart\";3720;WENN($AO$1=\"Morgen\";4192;WENN($AO$1=\"Die Zukunft\";4688;WENN($AO$1=\"Arktische Zukunft\";5200;WENN($AO$1=\"Ozeanische Zukunft\";5728;WENN($AO$1=\"Virtuelle Zukunft\";6272;WENN($AO$1=\"Raumfahrt: Mars\";9928;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";10784;WENN($AO$1=\"Raumfahrt: Venus\";11656;WENN($AO$1=\"Raumfahrt: Jupitermond\";12552;\"ERROR\"))))))))))))))))))))|||||=WENN($AO$1=\"Bronzezeit\";75;WENN($AO$1=\"Eisenzeit\";120;WENN($AO$1=\"Frühes Mittelalter\";300;WENN($AO$1=\"Hochmittelalter\";600;WENN($AO$1=\"Spätes Mittelalter\";900;WENN($AO$1=\"Kolonialzeit\";1500;WENN($AO$1=\"Industriezeitalter\";3000;WENN($AO$1=\"Jahrhundertwende\";4500;WENN($AO$1=\"Die Moderne\";7500;WENN($AO$1=\"Die Postmoderne\";12000;WENN($AO$1=\"Gegenwart\";18000;WENN($AO$1=\"Morgen\";30000;WENN($AO$1=\"Die Zukunft\";48000;WENN($AO$1=\"Arktische Zukunft\";72000;WENN($AO$1=\"Ozeanische Zukunft\";90000;WENN($AO$1=\"Virtuelle Zukunft\";120000;WENN($AO$1=\"Raumfahrt: Mars\";195000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";270000;WENN($AO$1=\"Raumfahrt: Venus\";315000;WENN($AO$1=\"Raumfahrt: Jupitermond\";360000;\"ERROR\"))))))))))))))))))))||=25|=WENN($AO$1=\"Bronzezeit\";1600;WENN($AO$1=\"Eisenzeit\";3700;WENN($AO$1=\"Frühes Mittelalter\";6200;WENN($AO$1=\"Hochmittelalter\";8700;WENN($AO$1=\"Spätes Mittelalter\";11500;WENN($AO$1=\"Kolonialzeit\";17500;WENN($AO$1=\"Industriezeitalter\";21800;WENN($AO$1=\"Jahrhundertwende\";26400;WENN($AO$1=\"Die Moderne\";32900;WENN($AO$1=\"Die Postmoderne\";41300;WENN($AO$1=\"Gegenwart\";53800;WENN($AO$1=\"Morgen\";62300;WENN($AO$1=\"Die Zukunft\";71400;WENN($AO$1=\"Arktische Zukunft\";81200;WENN($AO$1=\"Ozeanische Zukunft\";91500;WENN($AO$1=\"Virtuelle Zukunft\";102500;WENN($AO$1=\"Raumfahrt: Mars\";171200;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";189600;WENN($AO$1=\"Raumfahrt: Venus\";209100;WENN($AO$1=\"Raumfahrt: Jupitermond\";229500;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";1530;WENN($AO$1=\"Eisenzeit\";2640;WENN($AO$1=\"Frühes Mittelalter\";2920;WENN($AO$1=\"Hochmittelalter\";3260;WENN($AO$1=\"Spätes Mittelalter\";3630;WENN($AO$1=\"Kolonialzeit\";4270;WENN($AO$1=\"Industriezeitalter\";4820;WENN($AO$1=\"Jahrhundertwende\";5290;WENN($AO$1=\"Die Moderne\";5660;WENN($AO$1=\"Die Postmoderne\";5900;WENN($AO$1=\"Gegenwart\";6850;WENN($AO$1=\"Morgen\";8320;WENN($AO$1=\"Die Zukunft\";9960;WENN($AO$1=\"Arktische Zukunft\";11600;WENN($AO$1=\"Ozeanische Zukunft\";13390;WENN($AO$1=\"Virtuelle Zukunft\";15990;WENN($AO$1=\"Raumfahrt: Mars\";25590;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";27810;WENN($AO$1=\"Raumfahrt: Venus\";30050;WENN($AO$1=\"Raumfahrt: Jupitermond\";32370;\"ERROR\"))))))))))))))))))))|||=10+5+5||||=5|||false|true"));
		// Limited building.
		buildings.put(wikiUrl + "Forge-Brunnen_-_Aktiv", Arrays.asList(
				"Forge-Brunnen - Aktiv||Nicht definiert, wahrscheinlich keine|2|2|5 Sek.||0|0|0|false|=250||||||=WENN($AO$1=\"Bronzezeit\";20;WENN($AO$1=\"Eisenzeit\";32;WENN($AO$1=\"Frühes Mittelalter\";80;WENN($AO$1=\"Hochmittelalter\";160;WENN($AO$1=\"Spätes Mittelalter\";240;WENN($AO$1=\"Kolonialzeit\";400;WENN($AO$1=\"Industriezeitalter\";800;WENN($AO$1=\"Jahrhundertwende\";1200;WENN($AO$1=\"Die Moderne\";2000;WENN($AO$1=\"Die Postmoderne\";3200;WENN($AO$1=\"Gegenwart\";4800;WENN($AO$1=\"Morgen\";8000;WENN($AO$1=\"Die Zukunft\";12800;WENN($AO$1=\"Arktische Zukunft\";19200;WENN($AO$1=\"Ozeanische Zukunft\";24000;WENN($AO$1=\"Virtuelle Zukunft\";32000;WENN($AO$1=\"Raumfahrt: Mars\";52000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";72000;WENN($AO$1=\"Raumfahrt: Venus\";84000;WENN($AO$1=\"Raumfahrt: Jupitermond\";96000;\"ERROR\"))))))))))))))))))))|||||||||||||Eingeschränkte Produktion: 5 T.;10% FP Bonus|false|true"));
		// Limited building with production table in properties table.
		buildings.put(wikiUrl + "Kobaltblaue_Lagune_-_Aktiv", Arrays.asList(
				"Kobaltblaue Lagune - Aktiv||Nicht definiert, wahrscheinlich keine|2|2|5 Sek.||0|0|0|false|=250||||||=WENN($AO$1=\"Bronzezeit\";20;WENN($AO$1=\"Eisenzeit\";32;WENN($AO$1=\"Frühes Mittelalter\";80;WENN($AO$1=\"Hochmittelalter\";160;WENN($AO$1=\"Spätes Mittelalter\";240;WENN($AO$1=\"Kolonialzeit\";400;WENN($AO$1=\"Industriezeitalter\";800;WENN($AO$1=\"Jahrhundertwende\";1200;WENN($AO$1=\"Die Moderne\";2000;WENN($AO$1=\"Die Postmoderne\";3200;WENN($AO$1=\"Gegenwart\";4800;WENN($AO$1=\"Morgen\";8000;WENN($AO$1=\"Die Zukunft\";12800;WENN($AO$1=\"Arktische Zukunft\";19200;WENN($AO$1=\"Ozeanische Zukunft\";24000;WENN($AO$1=\"Virtuelle Zukunft\";32000;WENN($AO$1=\"Raumfahrt: Mars\";52000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";72000;WENN($AO$1=\"Raumfahrt: Venus\";84000;WENN($AO$1=\"Raumfahrt: Jupitermond\";96000;\"ERROR\"))))))))))))))))))))|||||||||||=1||Eingeschränkte Produktion: 5 Sammlungen;Wenn du dieses Gebäude sammelst, erhältst du 2 Aufladungen mit einer Chance von 100%, die nächsten Sammlungen von anderen Gebäuden um 100% zu verbessern.|false|true"));
		// New page layout, multiple boosts, productions, fragments, motivated.
		buildings.put(wikiUrl + "Chocolaterie_-_St._10", Arrays.asList(
				"Chocolaterie - St. 10||1x1|4|6|5 Sek.||15190|19|30|false|=WENN($AO$1=\"Bronzezeit\";680;WENN($AO$1=\"Eisenzeit\";1180;WENN($AO$1=\"Frühes Mittelalter\";1310;WENN($AO$1=\"Hochmittelalter\";1460;WENN($AO$1=\"Spätes Mittelalter\";1620;WENN($AO$1=\"Kolonialzeit\";1910;WENN($AO$1=\"Industriezeitalter\";2150;WENN($AO$1=\"Jahrhundertwende\";2360;WENN($AO$1=\"Die Moderne\";2530;WENN($AO$1=\"Die Postmoderne\";2640;WENN($AO$1=\"Gegenwart\";3060;WENN($AO$1=\"Morgen\";3720;WENN($AO$1=\"Die Zukunft\";4450;WENN($AO$1=\"Arktische Zukunft\";5190;WENN($AO$1=\"Ozeanische Zukunft\";5990;WENN($AO$1=\"Virtuelle Zukunft\";7150;WENN($AO$1=\"Raumfahrt: Mars\";11440;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";12430;WENN($AO$1=\"Raumfahrt: Venus\";13430;WENN($AO$1=\"Raumfahrt: Jupitermond\";14470;\"ERROR\"))))))))))))))))))))||=WENN($AO$1=\"Bronzezeit\";4;WENN($AO$1=\"Eisenzeit\";4;WENN($AO$1=\"Frühes Mittelalter\";4;WENN($AO$1=\"Hochmittelalter\";4;WENN($AO$1=\"Spätes Mittelalter\";4;WENN($AO$1=\"Kolonialzeit\";5;WENN($AO$1=\"Industriezeitalter\";5;WENN($AO$1=\"Jahrhundertwende\";5;WENN($AO$1=\"Die Moderne\";5;WENN($AO$1=\"Die Postmoderne\";5;WENN($AO$1=\"Gegenwart\";6;WENN($AO$1=\"Morgen\";6;WENN($AO$1=\"Die Zukunft\";6;WENN($AO$1=\"Arktische Zukunft\";6;WENN($AO$1=\"Ozeanische Zukunft\";6;WENN($AO$1=\"Virtuelle Zukunft\";7;WENN($AO$1=\"Raumfahrt: Mars\";7;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";7;WENN($AO$1=\"Raumfahrt: Venus\";7;WENN($AO$1=\"Raumfahrt: Jupitermond\";7;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";5;WENN($AO$1=\"Eisenzeit\";5;WENN($AO$1=\"Frühes Mittelalter\";5;WENN($AO$1=\"Hochmittelalter\";5;WENN($AO$1=\"Spätes Mittelalter\";5;WENN($AO$1=\"Kolonialzeit\";6;WENN($AO$1=\"Industriezeitalter\";6;WENN($AO$1=\"Jahrhundertwende\";6;WENN($AO$1=\"Die Moderne\";6;WENN($AO$1=\"Die Postmoderne\";6;WENN($AO$1=\"Gegenwart\";7;WENN($AO$1=\"Morgen\";7;WENN($AO$1=\"Die Zukunft\";7;WENN($AO$1=\"Arktische Zukunft\";7;WENN($AO$1=\"Ozeanische Zukunft\";7;WENN($AO$1=\"Virtuelle Zukunft\";8;WENN($AO$1=\"Raumfahrt: Mars\";8;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";8;WENN($AO$1=\"Raumfahrt: Venus\";8;WENN($AO$1=\"Raumfahrt: Jupitermond\";8;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";120;WENN($AO$1=\"Eisenzeit\";192;WENN($AO$1=\"Frühes Mittelalter\";480;WENN($AO$1=\"Hochmittelalter\";960;WENN($AO$1=\"Spätes Mittelalter\";1440;WENN($AO$1=\"Kolonialzeit\";2400;WENN($AO$1=\"Industriezeitalter\";4800;WENN($AO$1=\"Jahrhundertwende\";7200;WENN($AO$1=\"Die Moderne\";12000;WENN($AO$1=\"Die Postmoderne\";19200;WENN($AO$1=\"Gegenwart\";28800;WENN($AO$1=\"Morgen\";48000;WENN($AO$1=\"Die Zukunft\";76800;WENN($AO$1=\"Arktische Zukunft\";115200;WENN($AO$1=\"Ozeanische Zukunft\";144000;WENN($AO$1=\"Virtuelle Zukunft\";192000;WENN($AO$1=\"Raumfahrt: Mars\";312000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";432000;WENN($AO$1=\"Raumfahrt: Venus\";504000;WENN($AO$1=\"Raumfahrt: Jupitermond\";576000;\"ERROR\"))))))))))))))))))))|||||||=4+6||||=2+2||1 'Nussknacker-Wachhaus'- Fragment;1 'Nussknacker-Wachhaus'- Fragment|true|true"));
		// Multiple random item productions, besides fragments.
		buildings.put(wikiUrl + "Druidenhütte_-_St._9", Arrays.asList(
				"Druidenhütte - St. 9||1x1|5|4|5 Sek.||13670|17|27|false|||=WENN($AO$1=\"Bronzezeit\";9;WENN($AO$1=\"Eisenzeit\";10;WENN($AO$1=\"Frühes Mittelalter\";10;WENN($AO$1=\"Hochmittelalter\";11;WENN($AO$1=\"Spätes Mittelalter\";11;WENN($AO$1=\"Kolonialzeit\";11;WENN($AO$1=\"Industriezeitalter\";12;WENN($AO$1=\"Jahrhundertwende\";12;WENN($AO$1=\"Die Moderne\";12;WENN($AO$1=\"Die Postmoderne\";13;WENN($AO$1=\"Gegenwart\";13;WENN($AO$1=\"Morgen\";14;WENN($AO$1=\"Die Zukunft\";14;WENN($AO$1=\"Arktische Zukunft\";14;WENN($AO$1=\"Ozeanische Zukunft\";15;WENN($AO$1=\"Virtuelle Zukunft\";15;WENN($AO$1=\"Raumfahrt: Mars\";15;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";16;WENN($AO$1=\"Raumfahrt: Venus\";16;WENN($AO$1=\"Raumfahrt: Jupitermond\";17;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";14;WENN($AO$1=\"Eisenzeit\";14;WENN($AO$1=\"Frühes Mittelalter\";14;WENN($AO$1=\"Hochmittelalter\";14;WENN($AO$1=\"Spätes Mittelalter\";15;WENN($AO$1=\"Kolonialzeit\";15;WENN($AO$1=\"Industriezeitalter\";15;WENN($AO$1=\"Jahrhundertwende\";15;WENN($AO$1=\"Die Moderne\";16;WENN($AO$1=\"Die Postmoderne\";16;WENN($AO$1=\"Gegenwart\";16;WENN($AO$1=\"Morgen\";17;WENN($AO$1=\"Die Zukunft\";17;WENN($AO$1=\"Arktische Zukunft\";17;WENN($AO$1=\"Ozeanische Zukunft\";18;WENN($AO$1=\"Virtuelle Zukunft\";18;WENN($AO$1=\"Raumfahrt: Mars\";18;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";19;WENN($AO$1=\"Raumfahrt: Venus\";19;WENN($AO$1=\"Raumfahrt: Jupitermond\";20;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";100;WENN($AO$1=\"Eisenzeit\";160;WENN($AO$1=\"Frühes Mittelalter\";400;WENN($AO$1=\"Hochmittelalter\";800;WENN($AO$1=\"Spätes Mittelalter\";1200;WENN($AO$1=\"Kolonialzeit\";2000;WENN($AO$1=\"Industriezeitalter\";4000;WENN($AO$1=\"Jahrhundertwende\";6000;WENN($AO$1=\"Die Moderne\";10000;WENN($AO$1=\"Die Postmoderne\";16000;WENN($AO$1=\"Gegenwart\";24000;WENN($AO$1=\"Morgen\";40000;WENN($AO$1=\"Die Zukunft\";64000;WENN($AO$1=\"Arktische Zukunft\";96000;WENN($AO$1=\"Ozeanische Zukunft\";120000;WENN($AO$1=\"Virtuelle Zukunft\";160000;WENN($AO$1=\"Raumfahrt: Mars\";260000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";360000;WENN($AO$1=\"Raumfahrt: Venus\";420000;WENN($AO$1=\"Raumfahrt: Jupitermond\";480000;\"ERROR\"))))))))))))))))))))|||=WENN($AO$1=\"Bronzezeit\";50;WENN($AO$1=\"Eisenzeit\";120;WENN($AO$1=\"Frühes Mittelalter\";200;WENN($AO$1=\"Hochmittelalter\";280;WENN($AO$1=\"Spätes Mittelalter\";370;WENN($AO$1=\"Kolonialzeit\";560;WENN($AO$1=\"Industriezeitalter\";700;WENN($AO$1=\"Jahrhundertwende\";850;WENN($AO$1=\"Die Moderne\";1060;WENN($AO$1=\"Die Postmoderne\";1330;WENN($AO$1=\"Gegenwart\";1740;WENN($AO$1=\"Morgen\";2010;WENN($AO$1=\"Die Zukunft\";2300;WENN($AO$1=\"Arktische Zukunft\";2620;WENN($AO$1=\"Ozeanische Zukunft\";2950;WENN($AO$1=\"Virtuelle Zukunft\";3310;WENN($AO$1=\"Raumfahrt: Mars\";5520;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";6120;WENN($AO$1=\"Raumfahrt: Venus\";6740;WENN($AO$1=\"Raumfahrt: Jupitermond\";7400;\"ERROR\"))))))))))))))))))))||||||||=4+3||1 'Erzdruidenhütte-Kit'- Fragment;Zufallsproduktion!;70%: 5 'Baum der Geduld'- Fragmente;30%: 5 'Baum der Lebenskraft'- Fragmente;50%: 1x 100%-Münz-Trank;50%: 1x 8 Std. Münzen-Beschleuniger|false|true"));
		// New layout, only motivated production, gives population.
		buildings.put(wikiUrl + "Bühne_der_Zeitalter", Arrays.asList(
				"Bühne der Zeitalter||1x1|4|4|5 Sek.||11540|15|23|false|=WENN($AO$1=\"Bronzezeit\";90;WENN($AO$1=\"Eisenzeit\";196;WENN($AO$1=\"Frühes Mittelalter\";303;WENN($AO$1=\"Hochmittelalter\";427;WENN($AO$1=\"Spätes Mittelalter\";563;WENN($AO$1=\"Kolonialzeit\";711;WENN($AO$1=\"Industriezeitalter\";869;WENN($AO$1=\"Jahrhundertwende\";1037;WENN($AO$1=\"Die Moderne\";1217;WENN($AO$1=\"Die Postmoderne\";1402;WENN($AO$1=\"Gegenwart\";1601;WENN($AO$1=\"Morgen\";1805;WENN($AO$1=\"Die Zukunft\";2017;WENN($AO$1=\"Arktische Zukunft\";2238;WENN($AO$1=\"Ozeanische Zukunft\";2468;WENN($AO$1=\"Virtuelle Zukunft\";2701;WENN($AO$1=\"Raumfahrt: Mars\";4277;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";4648;WENN($AO$1=\"Raumfahrt: Venus\";5023;WENN($AO$1=\"Raumfahrt: Jupitermond\";5425;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";82;WENN($AO$1=\"Eisenzeit\";179;WENN($AO$1=\"Frühes Mittelalter\";276;WENN($AO$1=\"Hochmittelalter\";389;WENN($AO$1=\"Spätes Mittelalter\";512;WENN($AO$1=\"Kolonialzeit\";647;WENN($AO$1=\"Industriezeitalter\";790;WENN($AO$1=\"Jahrhundertwende\";943;WENN($AO$1=\"Die Moderne\";1107;WENN($AO$1=\"Die Postmoderne\";1275;WENN($AO$1=\"Gegenwart\";1456;WENN($AO$1=\"Morgen\";1641;WENN($AO$1=\"Die Zukunft\";1834;WENN($AO$1=\"Arktische Zukunft\";2035;WENN($AO$1=\"Ozeanische Zukunft\";2244;WENN($AO$1=\"Virtuelle Zukunft\";2456;WENN($AO$1=\"Raumfahrt: Mars\";3889;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";4226;WENN($AO$1=\"Raumfahrt: Venus\";4567;WENN($AO$1=\"Raumfahrt: Jupitermond\";4933;\"ERROR\"))))))))))))))))))))|=WENN($AO$1=\"Bronzezeit\";7;WENN($AO$1=\"Eisenzeit\";7;WENN($AO$1=\"Frühes Mittelalter\";8;WENN($AO$1=\"Hochmittelalter\";8;WENN($AO$1=\"Spätes Mittelalter\";9;WENN($AO$1=\"Kolonialzeit\";9;WENN($AO$1=\"Industriezeitalter\";10;WENN($AO$1=\"Jahrhundertwende\";10;WENN($AO$1=\"Die Moderne\";11;WENN($AO$1=\"Die Postmoderne\";11;WENN($AO$1=\"Gegenwart\";12;WENN($AO$1=\"Morgen\";12;WENN($AO$1=\"Die Zukunft\";13;WENN($AO$1=\"Arktische Zukunft\";14;WENN($AO$1=\"Ozeanische Zukunft\";15;WENN($AO$1=\"Virtuelle Zukunft\";16;WENN($AO$1=\"Raumfahrt: Mars\";17;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";18;WENN($AO$1=\"Raumfahrt: Venus\";19;WENN($AO$1=\"Raumfahrt: Jupitermond\";19;\"ERROR\"))))))))))))))))))))||||=WENN($AO$1=\"Bronzezeit\";80;WENN($AO$1=\"Eisenzeit\";128;WENN($AO$1=\"Frühes Mittelalter\";320;WENN($AO$1=\"Hochmittelalter\";640;WENN($AO$1=\"Spätes Mittelalter\";960;WENN($AO$1=\"Kolonialzeit\";1600;WENN($AO$1=\"Industriezeitalter\";3200;WENN($AO$1=\"Jahrhundertwende\";4800;WENN($AO$1=\"Die Moderne\";8000;WENN($AO$1=\"Die Postmoderne\";12800;WENN($AO$1=\"Gegenwart\";19200;WENN($AO$1=\"Morgen\";32000;WENN($AO$1=\"Die Zukunft\";51200;WENN($AO$1=\"Arktische Zukunft\";76800;WENN($AO$1=\"Ozeanische Zukunft\";96000;WENN($AO$1=\"Virtuelle Zukunft\";128000;WENN($AO$1=\"Raumfahrt: Mars\";208000;WENN($AO$1=\"Raumfahrt: Asteroidengürtel\";288000;WENN($AO$1=\"Raumfahrt: Venus\";336000;WENN($AO$1=\"Raumfahrt: Jupitermond\";384000;\"ERROR\"))))))))))))))))))))|||||||=15||||=4|||false|true"));

		return buildings;
	}

	/**
	 * Get urls for all buildings that can be found in a table on the given url. For
	 * example {@link WebsiteParser#specialBuildingsPage specialBuildingsPage}.
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

			// For some reason new building layouts don't have their building type and
			// street listed on the details page. So read it from the main table.
			// Assuming the data is the same as on teh details page if present.
			// TODO: Maybe return a list of (empty) buildings with saved urls instead of a
			// list of string urls? Then additional data could be saved directly from the
			// beginning.
			String buildingType = cleanHtmlSplit(cells[3]);
			additionalProperties.computeIfAbsent(buildingUrl, key -> new HashMap<String, String>()).put("Art:",
					buildingType);
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

		// Add additional data first, so it can be overwritten by actual data?
		if (additionalProperties.containsKey(buildingUrl)) {
			additionalProperties.get(buildingUrl).forEach((k, v) -> addPropertiesToBuildings(buildings, k, v));
		}

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
						if ("Boosts:".equals(lastHeading) || "1 T. Produktion:".equals(lastHeading)) {
							// Unspecific boost or production. Need to analyze icon and value of data later.
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
									// Assuming only multi-row heading needs to be ignored. Need to check next line
									// to see if it is still a heading row.
									if (colspan == 1 && rowspan == 1 && headings.size() <= spanningCol && r == 1
											&& buildingRows[r + 1].contains("<td")) {
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
			break;
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

	// TODO: Move all the formula stuff in building class? Or even a new class?
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
	 * If the value for every age is the same, just give out the value directly
	 * without checking for age. Should only be called after all values are added.
	 * Otherwise it could be, that age dependent values have to be added to a now
	 * not age dependent value.
	 * 
	 * @param formula The formula to simplify.
	 * @return The simplified formula, or the original formula if nothing could be
	 *         changed.
	 */
	private static String simplifyFomula(String formula) {
		// Match numbers and number operations.
		Pattern pattern = Pattern.compile(";(\\d+([+\\-*,]\\d+)*);");
		Matcher matcher = pattern.matcher(formula);

		if (matcher.find()) {
			String firstValue = matcher.group(1);
			boolean allSame = true;

			while (matcher.find()) {
				String currentValue = matcher.group(1);
				if (!currentValue.equals(firstValue)) {
					allSame = false;
					break;
				}
			}
			if (allSame) {
				return "=" + firstValue;
			}
		}
		return formula;
	}

	/**
	 * Simplify the formulas of a building. See
	 * {@link WebsiteParser#simplifyFomula(String) simplifyFormula}.
	 * 
	 * @param building The building which formulas will be simplified.
	 */
	private static void simplifyBuildingFormulas(WikiBuilding building) {
		// TODO: Just do it for every formula?
		building.setHappiness(simplifyFomula(building.getHappiness()));
		building.setPopulation(simplifyFomula(building.getPopulation()));
		building.setAttackerAttack(simplifyFomula(building.getAttackerAttack()));
		building.setAttackerDefense(simplifyFomula(building.getAttackerDefense()));
		building.setDefenderAttack(simplifyFomula(building.getDefenderAttack()));
		building.setDefenderDefense(simplifyFomula(building.getDefenderDefense()));
		building.setRanking(simplifyFomula(building.getRanking()));
		building.setMoneyPercent(simplifyFomula(building.getMoneyPercent()));
		building.setSuppliesPercent(simplifyFomula(building.getSuppliesPercent()));
		building.setMoney(simplifyFomula(building.getMoney()));
		building.setSupplies(simplifyFomula(building.getSupplies()));
		building.setGuildPower(simplifyFomula(building.getGuildPower()));
		building.setMedals(simplifyFomula(building.getMedals()));
		building.setGoods(simplifyFomula(building.getGoods()));
		building.setGuildGoods(simplifyFomula(building.getGuildGoods()));
		building.setBlueprints(simplifyFomula(building.getBlueprints()));
		building.setDiamonds(simplifyFomula(building.getDiamonds()));
		building.setForgePoints(simplifyFomula(building.getForgePoints()));
		building.setUnits(simplifyFomula(building.getUnits()));
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
			// Can contain multiple entries.
			String[] splitted = data.split("<br ");
			for (String s : splitted) {
				var tmpData = cleanHtmlSplit(s);
				var tmpDataType = getImageText(s);
				addProductionToBuildings(buildings, tmpDataType, tmpData, 1);
			}
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
			// TODO: Make it work for tables?
			if (data.contains("<img ")) {
				addProductionToBuildings(buildings, getImageText(data), cleanHtmlSplit(data), 1);
			} else {
				throw new IllegalArgumentException("Expectedt data to contain an image: " + data);
			}
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