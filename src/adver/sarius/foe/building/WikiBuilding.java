package adver.sarius.foe.building;

public class WikiBuilding {

	// TODO: Is there a fixed order for all that stuff?
	private String name;
	private String type = "";
	private Street street = Street.UNDEFINED;
	// Size 3x4 considered as 3 height and 4 width.
	private int height;
	private int width;
	private String buildTime = "";
	private String set = "";
	private int coins24;
	private int gemsMin24;
	private int gemsMax24;
	// Some buildings only work when connected to the starting building.
	private boolean needsStarting = false;

	// Possibly age dependent values. Could be a formula for something like excel
	// documents.
	private String happiness = "";
	private String population = "";
	private String attackerAttack = "";
	private String attackerDefense = "";
	private String defenderAttack = "";
	private String defenderDefense = "";
	private String attackerAttackGG = "";
	private String attackerDefenseGG = "";
	private String defenderAttackGG = "";
	private String defenderDefenseGG = "";
	private String attackerAttackGEX = "";
	private String attackerDefenseGEX = "";
	private String defenderAttackGEX = "";
	private String defenderDefenseGEX = "";
	private String ranking = "";
	private String moneyPercent = "";
	private String suppliesPercent = "";
	private String money = "";
	private String supplies = "";
	private String guildPower = "";
	private String medals = "";
	private String goods = "";
	private String guildGoods = "";
	private String blueprints = "";
	private String diamonds = "";
	private String forgePoints = "";
	private String units = "";
	private String specialProduction = "";
	// Not reliable. Some buildings can be upgraded with special kit, which is not
	// listed on the buildings page. But if it's set to true, its probably true.
	private boolean upgradeable = false;
	/** True if this building has all the set-bonus productions. */
	private boolean maxSetMembers = true;
	// Some productions need to be motivated. But not all, which makes it hard to
	// factor in and display correctly. So just assume everything is motivated
	// anyways.
	// private boolean needsMotivation;
	// Some buildings say they can not be plundered or motivated. Since some
	// buildings only produce stuff when motivated, which blocks plundering, it's
	// not really accurate.

	public WikiBuilding() {
		super();
	}

	public WikiBuilding(WikiBuilding toClone) {
		this.name = toClone.name;
		this.type = toClone.type;
		this.street = toClone.street;
		this.buildTime = toClone.buildTime;
		this.height = toClone.height;
		this.width = toClone.width;
		this.set = toClone.set;
		this.coins24 = toClone.coins24;
		this.gemsMin24 = toClone.gemsMin24;
		this.gemsMax24 = toClone.gemsMax24;
		this.needsStarting = toClone.needsStarting;
		this.happiness = toClone.happiness;
		this.population = toClone.population;
		this.attackerAttack = toClone.attackerAttack;
		this.attackerDefense = toClone.attackerDefense;
		this.defenderAttack = toClone.defenderAttack;
		this.defenderDefense = toClone.defenderDefense;
		this.attackerAttackGG = toClone.attackerAttackGG;
		this.attackerDefenseGG = toClone.attackerDefenseGG;
		this.defenderAttackGG = toClone.defenderAttackGG;
		this.defenderDefenseGG = toClone.defenderDefenseGG;
		this.attackerAttackGEX = toClone.attackerAttackGEX;
		this.attackerDefenseGEX = toClone.attackerDefenseGEX;
		this.defenderAttackGEX = toClone.defenderAttackGEX;
		this.defenderDefenseGEX = toClone.defenderDefenseGEX;
		this.ranking = toClone.ranking;
		this.moneyPercent = toClone.moneyPercent;
		this.suppliesPercent = toClone.suppliesPercent;
		this.money = toClone.money;
		this.supplies = toClone.supplies;
		this.guildPower = toClone.guildPower;
		this.medals = toClone.medals;
		this.goods = toClone.goods;
		this.guildGoods = toClone.guildGoods;
		this.blueprints = toClone.blueprints;
		this.diamonds = toClone.diamonds;
		this.forgePoints = toClone.forgePoints;
		this.units = toClone.units;
		this.specialProduction = toClone.specialProduction;
		this.upgradeable = toClone.upgradeable;
		this.maxSetMembers = toClone.maxSetMembers;
	}

	public WikiBuilding(WikiBuilding toClone, String appendName) {
		this(toClone);
		this.name = name + appendName;
	}

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

	public String getBuildTime() {
		return buildTime;
	}

	public void setBuildTime(String buildTime) {
		this.buildTime = buildTime;
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

	public String getAttackerAttack() {
		return attackerAttack;
	}

	public void setAttackerAttack(String atacker_attack) {
		this.attackerAttack = atacker_attack;
	}

	public String getAttackerDefense() {
		return attackerDefense;
	}

	public void setAttackerDefense(String atacker_defense) {
		this.attackerDefense = atacker_defense;
	}

	public String getDefenderAttack() {
		return defenderAttack;
	}

	public void setDefenderAttack(String defender_attack) {
		this.defenderAttack = defender_attack;
	}

	public String getDefenderDefense() {
		return defenderDefense;
	}

	public void setDefenderDefense(String defender_defense) {
		this.defenderDefense = defender_defense;
	}

	public String getAttackerAttackGG() {
		return attackerAttackGG;
	}

	public void setAttackerAttackGG(String attackerAttackGG) {
		this.attackerAttackGG = attackerAttackGG;
	}

	public String getAttackerDefenseGG() {
		return attackerDefenseGG;
	}

	public void setAttackerDefenseGG(String attackerDefenseGG) {
		this.attackerDefenseGG = attackerDefenseGG;
	}

	public String getDefenderAttackGG() {
		return defenderAttackGG;
	}

	public void setDefenderAttackGG(String defenderAttackGG) {
		this.defenderAttackGG = defenderAttackGG;
	}

	public String getDefenderDefenseGG() {
		return defenderDefenseGG;
	}

	public void setDefenderDefenseGG(String defenderDefenseGG) {
		this.defenderDefenseGG = defenderDefenseGG;
	}

	public String getAttackerAttackGEX() {
		return attackerAttackGEX;
	}

	public void setAttackerAttackGEX(String attackerAttackGEX) {
		this.attackerAttackGEX = attackerAttackGEX;
	}

	public String getAttackerDefenseGEX() {
		return attackerDefenseGEX;
	}

	public void setAttackerDefenseGEX(String attackerDefenseGEX) {
		this.attackerDefenseGEX = attackerDefenseGEX;
	}

	public String getDefenderAttackGEX() {
		return defenderAttackGEX;
	}

	public void setDefenderAttackGEX(String defenderAttackGEX) {
		this.defenderAttackGEX = defenderAttackGEX;
	}

	public String getDefenderDefenseGEX() {
		return defenderDefenseGEX;
	}

	public void setDefenderDefenseGEX(String defenderDefenseGEX) {
		this.defenderDefenseGEX = defenderDefenseGEX;
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

	public String getMoneyPercent() {
		return moneyPercent;
	}

	public void setMoneyPercent(String moneyPercent) {
		this.moneyPercent = moneyPercent;
	}

	public String getSuppliesPercent() {
		return suppliesPercent;
	}

	public void setSuppliesPercent(String suppliesPercent) {
		this.suppliesPercent = suppliesPercent;
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

	public String getGuildPower() {
		return guildPower;
	}

	public void setGuildPower(String guildPower) {
		this.guildPower = guildPower;
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

	public String getGuildGoods() {
		return guildGoods;
	}

	public void setGuildGoods(String guildGoods) {
		this.guildGoods = guildGoods;
	}

	public String getBlueprints() {
		return blueprints;
	}

	public void setBlueprints(String blueprints) {
		this.blueprints = blueprints;
	}

	public String getDiamonds() {
		return diamonds;
	}

	public void setDiamonds(String diamonds) {
		this.diamonds = diamonds;
	}

	public String getForgePoints() {
		return forgePoints;
	}

	public void setForgePoints(String forgePoints) {
		this.forgePoints = forgePoints;
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

	/**
	 * Append the special production by the given string. If the string is already
	 * present, do nothing. Will put delimiter between appends.
	 * 
	 * @param append String to append.
	 */
	public void appendSpecialProduction(String append) {
		appendSpecialProduction(append, false);
	}

	/**
	 * Append the special production by the given string. Will put delimiter between
	 * appends.
	 * 
	 * @param append       String to append.
	 * @param alwaysAppend True to always append, false to only append if string is
	 *                     not present yet.
	 */
	public void appendSpecialProduction(String append, boolean alwaysAppend) {
		if (this.specialProduction == null || this.specialProduction.isEmpty()) {
			this.specialProduction = append;
		} else if (alwaysAppend || !this.specialProduction.contains(append)) {
			this.specialProduction = this.specialProduction + ";" + append;
		}
	}

	public boolean isUpgradeable() {
		return upgradeable;
	}

	public void setUpgradeable(boolean upgradeable) {
		this.upgradeable = upgradeable;
	}

	public boolean isMaxSetMembers() {
		return maxSetMembers;
	}

	public void setMaxSetMembers(boolean maxSetMembers) {
		this.maxSetMembers = maxSetMembers;
	}

	@Override
	public String toString() {
		return name + "|" + type + "|" + street + "|" + height + "|" + width + "|" + buildTime + "|" + set + "|"
				+ coins24 + "|" + gemsMin24 + "|" + gemsMax24 + "|" + needsStarting + "|" + happiness + "|" + population
				+ "|" + attackerAttack + "|" + attackerDefense + "|" + defenderAttack + "|" + defenderDefense + "|"
				+ attackerAttackGG + "|" + attackerDefenseGG + "|" + defenderAttackGG + "|" + defenderDefenseGG + "|"
				+ attackerAttackGEX + "|" + attackerDefenseGEX + "|" + defenderAttackGEX + "|" + defenderDefenseGEX
				+ "|" + ranking + "|" + moneyPercent + "|" + suppliesPercent + "|" + money + "|" + supplies + "|"
				+ guildPower + "|" + medals + "|" + goods + "|" + guildGoods + "|" + blueprints + "|" + diamonds + "|"
				+ forgePoints + "|" + units + "|" + specialProduction + "|" + upgradeable + "|" + maxSetMembers;
	}
}