package com.casinocore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Interface for GUI menus
 */
public interface GUI extends InventoryHolder {

    /**
     * Open the GUI for a player
     * @param player The player to open for
     */
    void open(Player player);

    /**
     * Close the GUI for a player
     * @param player The player to close for
     */
    void close(Player player);

    /**
     * Handle inventory click event
     * @param event The click event
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Get the inventory instance
     * @return The inventory
     */
    Inventory getInventory();

    /**
     * Get the GUI title
     * @return The title
     */
    String getTitle();

    /**
     * Get the GUI size
     * @return The size (must be multiple of 9)
     */
    int getSize();

    /**
     * Set an item in the GUI
     * @param slot The slot index
     * @param item The item to set
     */
    void setItem(int slot, ItemStack item);

    /**
     * Set an item with a click handler
     * @param slot The slot index
     * @param item The item to set
     * @param handler The click handler
     */
    void setItem(int slot, ItemStack item, ClickHandler handler);

    /**
     * Refresh the GUI contents
     */
    void refresh();
}
