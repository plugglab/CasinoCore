package com.casinocore.games.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class WheelListener implements Listener {

    private final WheelGame game;

    public WheelListener(WheelGame game) {
        this.game = game;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WheelGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == 22) {
            game.startSpin(player, gui);
        } else if (event.getRawSlot() == 26 && !gui.isSpinning()) {
            game.close(player);
            gui.back();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WheelGUI gui)) {
            return;
        }
        if (gui.isSpinning() && event.getPlayer() instanceof Player player) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(game.getPlugin().getPlugin(), () -> player.openInventory(gui.getInventory()), 1L);
        } else if (event.getPlayer() instanceof Player player) {
            game.close(player);
        }
    }
}
