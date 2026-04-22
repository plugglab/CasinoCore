package com.casinocore.games.slots;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Event listener for slot machine GUI
 * Prevents item manipulation and handles close events
 */
public class SlotMachineListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SlotMachineGUI) {
            // Cancel all clicks to prevent item manipulation
            event.setCancelled(true);

            SlotMachineGUI gui = (SlotMachineGUI) holder;

            // If clicked the barrier (close button), close inventory
            if (event.getCurrentItem() != null &&
                event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {

                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();

                    // Only allow closing if not spinning
                    if (!gui.isSpinning()) {
                        player.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SlotMachineGUI) {
            SlotMachineGUI gui = (SlotMachineGUI) holder;

            // Prevent closing while spinning
            if (gui.isSpinning() && event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();

                // Reopen the inventory after a tick
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    gui.getPlugin().getPlugin(),
                    () -> player.openInventory(gui.getInventory()),
                    1L
                );
            }
        }
    }
}
