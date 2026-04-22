package com.casinocore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Functional interface for handling GUI clicks
 */
@FunctionalInterface
public interface ClickHandler {

    /**
     * Handle a click event
     * @param player The player who clicked
     * @param event The click event
     */
    void handle(Player player, InventoryClickEvent event);
}
