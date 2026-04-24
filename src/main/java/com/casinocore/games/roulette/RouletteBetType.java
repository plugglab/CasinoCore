package com.casinocore.games.roulette;

public enum RouletteBetType {
    SINGLE_NUMBER("Single Number", "single-number"),
    RED("Red", "red-black"),
    BLACK("Black", "red-black"),
    EVEN("Even", "even-odd"),
    ODD("Odd", "even-odd"),
    LOW("1-18", "low-high"),
    HIGH("19-36", "low-high"),
    FIRST_DOZEN("1st 12", "dozen"),
    SECOND_DOZEN("2nd 12", "dozen"),
    THIRD_DOZEN("3rd 12", "dozen");

    private final String displayName;
    private final String payoutKey;

    RouletteBetType(String displayName, String payoutKey) {
        this.displayName = displayName;
        this.payoutKey = payoutKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPayoutKey() {
        return payoutKey;
    }
}
