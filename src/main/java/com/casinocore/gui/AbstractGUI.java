package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for GUI menus
 * Provides common functionality for all GUIs
 */
public abstract class AbstractGUI implements GUI {

    protected final CasinoPlugin plugin;
    protected final String title;
    protected final int size;
    protected Inventory inventory;
    protected final Map<Integer, ClickHandler> clickHandlers;

    protected AbstractGUI(CasinoPlugin plugin, String title, int size) {
        this.plugin = plugin;
        this.title = title;
        this.size = size;
        this.clickHandlers = new HashMap<>();
        createInventory();
    }

    /**
     * Create the inventory instance
     */
    protected void createInventory() {
        Component titleComponent = plugin.getMessageManager().parse(title);
        this.inventory = Bukkit.createInventory(null, size, titleComponent);
    }

    @Override
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void close(Player player) {
        player.closeInventory();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // Cancel by default

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Check if slot is in our inventory
        if (slot < 0 || slot >= size) {
            return;
        }

        // Execute click handler if exists
        ClickHandler handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.handle(player, event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < size) {
            inventory.setItem(slot, item);
        }
    }

    @Override
    public void setItem(int slot, ItemStack item, ClickHandler handler) {
        setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    /**
     * Fill empty slots with a filler item
     * @param filler The filler item
     */
    protected void fillEmpty(ItemStack filler) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, filler);
            }
        }
    }

    /**
     * Fill border slots with an item
     * @param border The border item
     */
    protected void fillBorder(ItemStack border) {
        int rows = size / 9;

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            setItem(i, border);
            setItem(size - 9 + i, border);
        }

        // Left and right columns
        for (int i = 1; i < rows - 1; i++) {
            setItem(i * 9, border);
            setItem(i * 9 + 8, border);
        }
    }

    /**
     * Clear all items from the GUI
     */
    protected void clear() {
        inventory.clear();
        clickHandlers.clear();
    }

    @Override
    public void refresh() {
        clear();
        // Subclasses should override to repopulate
    }
}
