package com.casinocore.games.blackjack;

import java.util.ArrayList;
import java.util.List;

public class BlackjackTableState {

    public enum Phase {
        PLAYER_TURN,
        DEALER_TURN,
        ROUND_OVER
    }

    private final double baseBet;
    private final List<BlackjackHand> playerHands;
    private final List<Double> handBets;
    private final BlackjackHand dealerHand;
    private boolean dealerHidden;
    private Phase phase;
    private String status;
    private int activeHandIndex;

    public BlackjackTableState(double bet) {
        this.baseBet = bet;
        this.playerHands = new ArrayList<>();
        this.playerHands.add(new BlackjackHand());
        this.handBets = new ArrayList<>();
        this.handBets.add(bet);
        this.dealerHand = new BlackjackHand();
        this.dealerHidden = true;
        this.phase = Phase.PLAYER_TURN;
        this.status = "Dealing cards...";
        this.activeHandIndex = 0;
    }

    public double getBet() {
        return getCurrentBet();
    }

    public double getBaseBet() {
        return baseBet;
    }

    public double getTotalCommittedBet() {
        return handBets.stream().mapToDouble(Double::doubleValue).sum();
    }

    public BlackjackHand getPlayerHand() {
        return getCurrentHand();
    }

    public BlackjackHand getCurrentHand() {
        return playerHands.get(activeHandIndex);
    }

    public List<BlackjackHand> getPlayerHands() {
        return List.copyOf(playerHands);
    }

    public BlackjackHand getDealerHand() {
        return dealerHand;
    }

    public boolean isDealerHidden() {
        return dealerHidden;
    }

    public void setDealerHidden(boolean dealerHidden) {
        this.dealerHidden = dealerHidden;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getActiveHandIndex() {
        return activeHandIndex;
    }

    public void setActiveHandIndex(int activeHandIndex) {
        this.activeHandIndex = activeHandIndex;
    }

    public boolean hasSplitHand() {
        return playerHands.size() > 1;
    }

    public boolean canSplitCurrentHand() {
        return playerHands.size() == 1 && getCurrentHand().canSplit();
    }

    public void splitCurrentHand() {
        BlackjackHand current = getCurrentHand();
        BlackjackCard movedCard = current.removeLast();
        BlackjackHand splitHand = new BlackjackHand();
        splitHand.add(movedCard);
        playerHands.add(splitHand);
        handBets.add(baseBet);
    }

    public boolean hasNextHand() {
        return activeHandIndex + 1 < playerHands.size();
    }

    public void moveToNextHand() {
        if (hasNextHand()) {
            activeHandIndex++;
        }
    }

    public double getCurrentBet() {
        return handBets.get(activeHandIndex);
    }

    public double getBetForHand(int handIndex) {
        return handBets.get(handIndex);
    }
}
