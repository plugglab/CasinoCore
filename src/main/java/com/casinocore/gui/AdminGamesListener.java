package com.casinocore.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AdminGamesListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminGamesGUI gui)) {
            return;
        }

        event.setCancelled(true);
        gui.handleClick(event.getRawSlot());
    }
}
