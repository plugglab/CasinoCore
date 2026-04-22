package com.casinocore.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for GUI events
 * Handles clicks and close events for all GUIs
 */
public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // If the inventory holder is a GUI, handle the click
        if (holder instanceof GUI) {
            ((GUI) holder).handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // Additional close handling if needed
        if (holder instanceof GUI) {
            // Can add cleanup logic here if needed
        }
    }
}
