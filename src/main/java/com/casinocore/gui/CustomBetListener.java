package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CustomBetListener implements Listener {

    private final CasinoPlugin plugin;

    public CustomBetListener(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!CustomBetManager.isPrompting(player)) {
            return;
        }

        event.setCancelled(true);
        CustomBetManager.handleInput(plugin, player, event.getMessage());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CustomBetManager.cancel(event.getPlayer());
    }
}
