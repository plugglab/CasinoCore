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

    private static final int LEVER_SLOT = 13;
    private static final int SPIN_AGAIN_SLOT = 23;
    private static final int BACK_SLOT = 26;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SlotMachineGUI) {
            // Cancel all clicks to prevent item manipulation
            event.setCancelled(true);

            SlotMachineGUI gui = (SlotMachineGUI) holder;
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            if (event.getRawSlot() == LEVER_SLOT || event.getRawSlot() == SPIN_AGAIN_SLOT) {
                gui.getGame().pullLever(player, gui);
                return;
            }

            if (event.getRawSlot() == BACK_SLOT && !gui.isSpinning()) {
                gui.getGame().backToHub(player);
                gui.backToHub();
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
            } else if (event.getPlayer() instanceof Player player) {
                gui.getGame().backToHub(player);
            }
        }
    }
}
