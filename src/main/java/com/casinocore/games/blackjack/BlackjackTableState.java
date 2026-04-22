package com.casinocore.games.blackjack;

public class BlackjackTableState {

    public enum Phase {
        PLAYER_TURN,
        DEALER_TURN,
        ROUND_OVER
    }

    private final double bet;
    private final BlackjackHand playerHand;
    private final BlackjackHand dealerHand;
    private boolean dealerHidden;
    private Phase phase;
    private String status;

    public BlackjackTableState(double bet) {
        this.bet = bet;
        this.playerHand = new BlackjackHand();
        this.dealerHand = new BlackjackHand();
        this.dealerHidden = true;
        this.phase = Phase.PLAYER_TURN;
        this.status = "Dealing cards...";
    }

    public double getBet() {
        return bet;
    }

    public BlackjackHand getPlayerHand() {
        return playerHand;
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
}
