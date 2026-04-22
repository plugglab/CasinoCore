package com.casinocore.games.roulette;

public enum RouletteBetType {
    SINGLE_NUMBER("Single Number", "single-number"),
    RED("Red", "red-black"),
    BLACK("Black", "red-black"),
    EVEN("Even", "even-odd"),
    ODD("Odd", "even-odd");

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
