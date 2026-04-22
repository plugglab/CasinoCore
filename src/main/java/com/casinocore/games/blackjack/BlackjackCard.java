package com.casinocore.games.blackjack;

public record BlackjackCard(CardRank rank, CardSuit suit) {

    public String display() {
        return rank.getDisplayName() + " of " + suit.getDisplayName();
    }

    public int value() {
        return rank.getValue();
    }
}
