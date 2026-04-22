package com.casinocore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CasinoBigWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String gameName;
    private final double bet;
    private final double payout;

    public CasinoBigWinEvent(Player player, String gameName, double bet, double payout) {
        this.player = player;
        this.gameName = gameName;
        this.bet = bet;
        this.payout = payout;
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
