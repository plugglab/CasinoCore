package com.casinocore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for GUI events
 * Handles clicks and close events for all GUIs
 */
public class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();

        if (isCasinoCoreInventory(holder)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && shouldResync(event)) {
                player.updateInventory();
            }
        }

        // If the inventory holder is a GUI, handle the click
        if (holder instanceof GUI) {
            ((GUI) holder).handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!isCasinoCoreInventory(holder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.updateInventory();
                }
                return;
            }
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

    private boolean isCasinoCoreInventory(InventoryHolder holder) {
        return holder != null && holder.getClass().getName().startsWith("com.casinocore.");
    }

    private boolean shouldResync(InventoryClickEvent event) {
        if (event.isShiftClick()) {
            return true;
        }

        InventoryAction action = event.getAction();
        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY
            || action == InventoryAction.HOTBAR_MOVE_AND_READD
            || action == InventoryAction.HOTBAR_SWAP
            || action == InventoryAction.COLLECT_TO_CURSOR;
    }
}
