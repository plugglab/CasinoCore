package com.casinocore.games.ridethebus;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class RideTheBusListener implements Listener {

    private final RideTheBusGame game;

    public RideTheBusListener(RideTheBusGame game) {
        this.game = game;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RideTheBusGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            game.handleClick(player, gui, event.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof RideTheBusGUI gui) {
            game.handleClose(gui);
        }
    }
}
