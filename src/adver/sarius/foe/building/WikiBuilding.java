package adver.sarius.foe.building;

public class WikiBuilding {

	private String name;
	private String type;
	private Street street = Street.NO_STREET;
	// Size 3x4 considered as 3 height and 4 width.
	private int height;
	private int width;
	private String set = "-";
	private int coins24;
	private int gemsMin24;
	private int gemsMax24;
	// Some buildings only work when connected to the starting building.
	private boolean needsStarting = false;
	// Possibly age dependent values. Could be a formula for something like excel documents.
	private String happiness;
	private String population;
	private String atacker_attack;
	private String atacker_defense;
	private String defender_attack;
	private String defender_defense;
	private String ranking;
	private String money;
	private String supplies;
	private String medals;
	private String goods;
	private String fp;
	private String units;
	private String specialProduction;
	// Not reliable. Some buildings can be upgraded with special kit, which is not listed on the buildings page.
	//private boolean upgradeable;
	// Some productions need to be motivated. But not all, which makes it hard to factor in and display correctly. So just assume everything is motivated anyways.
	//private boolean needsMotivation;
	// Some productions only count with enough set parts. They should be made as separate buildings.
	// Some buildings say they can not be plundered or motivated. Since some buildings only produce stuff when motivated, which blocks plundering, it's not really accurate.
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Street getStreet() {
		return street;
	}
	public void setStreet(Street street) {
		this.street = street;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getCoins24() {
		return coins24;
	}
	public void setCoins24(int coins24) {
		this.coins24 = coins24;
	}
	public String getSet() {
		return set;
	}
	public void setSet(String set) {
		this.set = set;
	}
	public int getGemsMin24() {
		return gemsMin24;
	}
	public void setGemsMin24(int gemsMin24) {
		this.gemsMin24 = gemsMin24;
	}
	public int getGemsMax24() {
		return gemsMax24;
	}
	public void setGemsMax24(int gemsMax24) {
		this.gemsMax24 = gemsMax24;
	}
	public boolean isNeedsStarting() {
		return needsStarting;
	}
	public void setNeedsStarting(boolean needsStarting) {
		this.needsStarting = needsStarting;
	}
	public String getHappiness() {
		return happiness;
	}
	public void setHappiness(String happiness) {
		this.happiness = happiness;
	}
	public String getPopulation() {
		return population;
	}
	public void setPopulation(String population) {
		this.population = population;
	}
	public String getAtacker_attack() {
		return atacker_attack;
	}
	public void setAtacker_attack(String atacker_attack) {
		this.atacker_attack = atacker_attack;
	}
	public String getAtacker_defense() {
		return atacker_defense;
	}
	public void setAtacker_defense(String atacker_defense) {
		this.atacker_defense = atacker_defense;
	}
	public String getDefender_attack() {
		return defender_attack;
	}
	public void setDefender_attack(String defender_attack) {
		this.defender_attack = defender_attack;
	}
	public String getDefender_defense() {
		return defender_defense;
	}
	public void setDefender_defense(String defender_defense) {
		this.defender_defense = defender_defense;
	}
	public String getRanking() {
		return ranking;
	}
	public void setRanking(String ranking) {
		this.ranking = ranking;
	}
	public String getMoney() {
		return money;
	}
	public void setMoney(String money) {
		this.money = money;
	}
	public String getSupplies() {
		return supplies;
	}
	public void setSupplies(String supplies) {
		this.supplies = supplies;
	}
	public String getMedals() {
		return medals;
	}
	public void setMedals(String medals) {
		this.medals = medals;
	}
	public String getGoods() {
		return goods;
	}
	public void setGoods(String goods) {
		this.goods = goods;
	}
	public String getFp() {
		return fp;
	}
	public void setFp(String fp) {
		this.fp = fp;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public String getSpecialProduction() {
		return specialProduction;
	}
	public void setSpecialProduction(String specialProduction) {
		this.specialProduction = specialProduction;
	}
	
	@Override
	public String toString() {
		return name + " | " + type + " | " + street + " | " + height + " | " + width + " | " + set 
				+ " | " + coins24 + " | " + gemsMin24 + " | " + gemsMax24 + " | " + needsStarting;
	}
	
	// TODO: Non-24 hour productions.
	// TODO: Kaputte fragment-chancen? https://de.wiki.forgeofempires.com/index.php?title=Druidenh%C3%BCtte_-_St._9

	
}
