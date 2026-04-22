package com.casinocore.games.impl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class CoinFlipListener implements Listener {

    private final CoinFlipGame game;

    public CoinFlipListener(CoinFlipGame game) {
        this.game = game;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        game.handleQuit(event.getPlayer());
    }
}
