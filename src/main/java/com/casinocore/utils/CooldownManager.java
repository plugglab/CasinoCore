package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for players and games
 * Thread-safe implementation for async operations
 */
public class CooldownManager {

    private final CasinoPlugin plugin;
    // Map<PlayerUUID, Map<GameName, ExpiryTime>>
    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Set a cooldown for a player for a specific game
     *
     * @param player The player
     * @param gameName The game name
     * @param seconds The cooldown duration in seconds
     */
    public void setCooldown(Player player, String gameName, int seconds) {
        UUID uuid = player.getUniqueId();
        long expiryTime = System.currentTimeMillis() + (seconds * 1000L);

        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                 .put(gameName.toLowerCase(), expiryTime);
    }

    /**
     * Check if a player has an active cooldown for a game
     *
     * @param player The player
     * @param gameName The game name
     * @return true if player is on cooldown
     */
    public boolean hasCooldown(Player player, String gameName) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return false;
        }

        Long expiryTime = playerCooldowns.get(gameName.toLowerCase());

        if (expiryTime == null) {
            return false;
        }

        // Check if cooldown has expired
        if (System.currentTimeMillis() >= expiryTime) {
            playerCooldowns.remove(gameName.toLowerCase());
            return false;
        }

        return true;
    }

    /**
     * Get the remaining cooldown time for a player and game
     *
     * @param player The player
     * @param gameName The game name
     * @return Remaining seconds, or 0 if no cooldown
     */
    public int getRemainingCooldown(Player player, String gameName) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return 0;
        }

        Long expiryTime = playerCooldowns.get(gameName.toLowerCase());

        if (expiryTime == null) {
            return 0;
        }

        long remaining = expiryTime - System.currentTimeMillis();

        if (remaining <= 0) {
            playerCooldowns.remove(gameName.toLowerCase());
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Remove cooldown for a player and game
     *
     * @param player The player
     * @param gameName The game name
     */
    public void removeCooldown(Player player, String gameName) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns != null) {
            playerCooldowns.remove(gameName.toLowerCase());
        }
    }

    /**
     * Clear all cooldowns for a player
     *
     * @param player The player
     */
    public void clearPlayerCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * Clear all cooldowns
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * Check if player can play and set cooldown if they can
     *
     * @param player The player
     * @param gameName The game name
     * @return true if player can play (not on cooldown)
     */
    public boolean checkAndSetCooldown(Player player, String gameName) {
        if (hasCooldown(player, gameName)) {
            // Send cooldown message
            int remaining = getRemainingCooldown(player, gameName);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(remaining));
            plugin.getMessageManager().sendMessage(player, "cooldown-active", placeholders);
            return false;
        }

        // Set cooldown from config
        int cooldownSeconds = plugin.getConfigManager().getGameCooldown(gameName);
        setCooldown(player, gameName, cooldownSeconds);
        return true;
    }

    /**
     * Shutdown the cooldown manager and clear all data
     */
    public void shutdown() {
        cooldowns.clear();
    }
}
