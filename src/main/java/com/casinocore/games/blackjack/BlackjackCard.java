package com.casinocore.games.blackjack;

public record BlackjackCard(CardRank rank, CardSuit suit) {

    public String display() {
        return rank.getDisplayName() + " of " + suit.getDisplayName();
    }

    public String displayShort() {
        return switch (suit) {
            case HEARTS -> rank.getDisplayName() + " Hearts";
            case DIAMONDS -> rank.getDisplayName() + " Diamonds";
            case CLUBS -> rank.getDisplayName() + " Clubs";
            case SPADES -> rank.getDisplayName() + " Spades";
        };
    }

    public int value() {
        return rank.getValue();
    }
}
