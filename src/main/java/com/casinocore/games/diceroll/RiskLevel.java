package com.casinocore.games.diceroll;

public enum RiskLevel {
    LOW("<green>Low</green>", "low", 45, 1.5),
    MEDIUM("<yellow>Medium</yellow>", "medium", 65, 2.0),
    HIGH("<red>High</red>", "high", 85, 3.5);

    private final String displayName;
    private final String configKey;
    private final int defaultThreshold;
    private final double defaultMultiplier;

    RiskLevel(String displayName, String configKey, int defaultThreshold, double defaultMultiplier) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.defaultThreshold = defaultThreshold;
        this.defaultMultiplier = defaultMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public int getDefaultThreshold() {
        return defaultThreshold;
    }

    public double getDefaultMultiplier() {
        return defaultMultiplier;
    }

    public static RiskLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }

        for (RiskLevel level : values()) {
            if (level.configKey.equalsIgnoreCase(value) || level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }

        return null;
    }
}
