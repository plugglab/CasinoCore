package com.casinocore.games.blackjack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class BlackjackListener implements Listener {

    private final BlackjackGame game;

    public BlackjackListener(BlackjackGame game) {
        this.game = game;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof BlackjackGUI gui)) {
            return;
        }

        event.setCancelled(true);
        if (!game.getCasinoPlugin().getAntiAbuseManager().tryRecordClick(player, "blackjack")) {
            return;
        }
        game.handleClick(player, gui, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlackjackGUI gui)) {
            return;
        }

        game.handleClose(gui);
    }
}
