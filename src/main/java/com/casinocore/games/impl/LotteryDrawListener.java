package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class LotteryDrawListener implements Listener {

    private final CasinoPlugin plugin;

    public LotteryDrawListener(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LotteryDrawGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() == 26 && !gui.isDrawing() && event.getWhoClicked() instanceof Player) {
            gui.backToHub();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof LotteryDrawGUI gui)) {
            return;
        }

        if (gui.isDrawing() && event.getPlayer() instanceof Player player) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> player.openInventory(gui.getInventory()),
                1L
            );
        }
    }
}
