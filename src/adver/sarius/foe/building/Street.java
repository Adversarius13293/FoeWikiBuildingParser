package adver.sarius.foe.building;

public enum Street {
	STREET_1("1x1"), STREET_2("2x2"), NO_STREET("Keine Straße benötigt"), UNDEFINED("Nicht definiert, wahrscheinlich 1x1");

	private String displayName;

	Street(String displayName) {
		this.displayName = displayName;
	}

	public static Street fromString(String string) {
		for (Street s : Street.values()) {
			if (s.displayName.equals(string)) {
				return s;
			}
		}
		throw new IllegalArgumentException("No enum with displayName " + string + " found.");
	}
	
	public String toString() {
		return this.displayName;
	}
}
