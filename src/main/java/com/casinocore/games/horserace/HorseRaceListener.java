package com.casinocore.games.horserace;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class HorseRaceListener implements Listener {

    private final HorseRaceGame game;

    public HorseRaceListener(HorseRaceGame game) {
        this.game = game;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HorseRaceGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            game.handleClick(player, gui, event.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HorseRaceGUI gui)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            game.handleClose(player, gui);
        }
    }
}
