package com.casinocore.games.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackHand {

    private final List<BlackjackCard> cards = new ArrayList<>();

    public void add(BlackjackCard card) {
        cards.add(card);
    }

    public List<BlackjackCard> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public int getBestValue() {
        int total = 0;
        int aces = 0;

        for (BlackjackCard card : cards) {
            total += card.value();
            if (card.rank() == CardRank.ACE) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    public boolean isSoft() {
        int total = 0;
        int aces = 0;
        for (BlackjackCard card : cards) {
            total += card.value();
            if (card.rank() == CardRank.ACE) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return aces > 0;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && getBestValue() == 21;
    }

    public boolean isBust() {
        return getBestValue() > 21;
    }
}
