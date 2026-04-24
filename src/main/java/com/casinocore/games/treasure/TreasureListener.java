package com.casinocore.games.treasure;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class TreasureListener implements Listener {

    private final TreasureGame game;

    public TreasureListener(TreasureGame game) {
        this.game = game;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TreasureGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            game.handleClick(player, gui, event.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TreasureGUI gui) {
            game.handleClose(gui);
        }
    }
}
