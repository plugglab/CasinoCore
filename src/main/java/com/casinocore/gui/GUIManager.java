package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages GUI instances and player GUI sessions
 */
public class GUIManager {

    private final CasinoPlugin plugin;
    private final Map<UUID, GUI> openGUIs;

    public GUIManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();

        // Register listener
        plugin.getPlugin().getServer().getPluginManager()
              .registerEvents(new GUIListener(), plugin.getPlugin());
    }

    /**
     * Open a GUI for a player
     * @param player The player
     * @param gui The GUI to open
     */
    public void openGUI(Player player, GUI gui) {
        // Close existing GUI if open
        if (openGUIs.containsKey(player.getUniqueId())) {
            closeGUI(player);
        }

        gui.open(player);
        openGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Close the currently open GUI for a player
     * @param player The player
     */
    public void closeGUI(Player player) {
        GUI gui = openGUIs.remove(player.getUniqueId());
        if (gui != null) {
            gui.close(player);
        }
    }

    /**
     * Get the GUI a player has open
     * @param player The player
     * @return The GUI, or null if none open
     */
    public GUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * Check if a player has a GUI open
     * @param player The player
     * @return true if player has a GUI open
     */
    public boolean hasGUIOpen(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    /**
     * Close all open GUIs
     */
    public void closeAll() {
        for (Map.Entry<UUID, GUI> entry : openGUIs.entrySet()) {
            Player player = plugin.getPlugin().getServer().getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().close(player);
            }
        }
        openGUIs.clear();
    }

    /**
     * Shutdown the GUI manager
     */
    public void shutdown() {
        closeAll();
    }
}
