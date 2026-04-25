package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages plugin configuration with reloadable support
 */
public class ConfigManager {

    private final CasinoPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load the configuration file
     */
    public void loadConfig() {
        plugin.getPlugin().saveDefaultConfig();
        this.config = plugin.getPlugin().getConfig();
    }

    /**
     * Reload the configuration file
     */
    public void reloadConfig() {
        plugin.getPlugin().reloadConfig();
        this.config = plugin.getPlugin().getConfig();
    }

    /**
     * Get the FileConfiguration instance
     * @return The config instance
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Check if debug mode is enabled
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    /**
     * Get the currency symbol
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return config.getString("settings.currency-symbol", "$");
    }

    public String getConfiguredLocale() {
        return config.getString("settings.language", "en");
    }

    /**
     * Check if Vault integration is enabled
     * @return true if Vault should be used
     */
    public boolean isVaultEnabled() {
        return config.getBoolean("settings.use-vault", true);
    }

    /**
     * Get minimum bet amount
     * @return Minimum bet amount
     */
    public double getMinBet() {
        return config.getDouble("economy.min-bet", 10.0);
    }

    /**
     * Get minimum bet amount for a specific game.
     * Falls back to the global minimum bet.
     * @param gameName The game name
     * @return Minimum bet amount
     */
    public double getMinBet(String gameName) {
        return config.getDouble("games." + gameName + ".limits.min-bet", getMinBet());
    }

    /**
     * Get maximum bet amount
     * @return Maximum bet amount
     */
    public double getMaxBet() {
        return config.getDouble("economy.max-bet", 10000.0);
    }

    /**
     * Get maximum bet amount for a specific game.
     * Falls back to the global maximum bet.
     * @param gameName The game name
     * @return Maximum bet amount
     */
    public double getMaxBet(String gameName) {
        return config.getDouble("games." + gameName + ".limits.max-bet", getMaxBet());
    }

    /**
     * Get default starting balance
     * @return Default balance
     */
    public double getDefaultBalance() {
        return config.getDouble("economy.default-balance", 1000.0);
    }

    /**
     * Get global cooldown time in seconds
     * @return Global cooldown time
     */
    public int getGlobalCooldown() {
        return config.getInt("cooldowns.global", 5);
    }

    /**
     * Get cooldown for a specific game
     * @param gameName The game name
     * @return The cooldown in seconds, or global cooldown if not set
     */
    public int getGameCooldown(String gameName) {
        return config.getInt("cooldowns.games." + gameName, getGlobalCooldown());
    }

    public boolean isAntiAbuseEnabled() {
        return config.getBoolean("anti-abuse.enabled", true);
    }

    /**
     * Get a message from config
     * @param key The message key
     * @return The message, or empty string if not found
     */
    public String getMessage(String key) {
        return plugin.getLocaleManager().getMessage(key);
    }

    /**
     * Get the message prefix
     * @return The message prefix
     */
    public String getPrefix() {
        return config.getString("messages.prefix", "<gold>[Casino]</gold> ");
    }
}
