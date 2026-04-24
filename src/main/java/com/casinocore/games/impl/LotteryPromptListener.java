package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LotteryPromptListener implements Listener {

    private final CasinoPlugin plugin;

    public LotteryPromptListener(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!LotteryNumberPrompt.isWaiting(player)) {
            return;
        }

        event.setCancelled(true);
        LotteryNumberPrompt.handleInput(plugin, player, event.getMessage());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        LotteryNumberPrompt.cancel(event.getPlayer());
    }
}
