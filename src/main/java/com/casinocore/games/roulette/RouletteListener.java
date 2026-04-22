package com.casinocore.games.roulette;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class RouletteListener implements Listener {

    private final RouletteGame game;

    public RouletteListener(RouletteGame game) {
        this.game = game;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof RouletteGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        if (!game.getCasinoPlugin().getAntiAbuseManager().tryRecordClick(player, "roulette")) {
            return;
        }

        game.handleClick(player, gui, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RouletteGUI gui)) {
            return;
        }

        game.handleClose(gui.getPlayer());
    }
}
