package com.casinocore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CasinoGameResultEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String gameName;
    private final double bet;
    private final double payout;
    private final boolean won;

    public CasinoGameResultEvent(Player player, String gameName, double bet, double payout, boolean won) {
        this.player = player;
        this.gameName = gameName;
        this.bet = bet;
        this.payout = payout;
        this.won = won;
    }

    public Player getPlayer() {
        return player;
    }

    public String getGameName() {
        return gameName;
    }

    public double getBet() {
        return bet;
    }

    public double getPayout() {
        return payout;
    }

    public boolean isWon() {
        return won;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
