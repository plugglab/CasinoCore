package com.casinocore.core;

import com.casinocore.economy.EconomyManager;
import com.casinocore.games.GameManager;
import com.casinocore.stats.PlayerStatsManager;
import com.casinocore.utils.AntiAbuseManager;
import com.casinocore.utils.ConfigManager;
import com.casinocore.utils.CooldownManager;
import com.casinocore.utils.LocaleManager;
import com.casinocore.utils.MessageManager;
import com.casinocore.utils.ProtectionManager;
import com.casinocore.utils.UxManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Interface for accessing CasinoCore plugin components.
 * This provides a clean API for other classes to access managers.
 */
public interface CasinoPlugin {

    /**
     * Get the JavaPlugin instance
     * @return The plugin instance
     */
    JavaPlugin getPlugin();

    /**
     * Get the ConfigManager
     * @return The config manager instance
     */
    ConfigManager getConfigManager();

    /**
     * Get the MessageManager
     * @return The message manager instance
     */
    MessageManager getMessageManager();

    /**
     * Get the EconomyManager
     * @return The economy manager instance
     */
    EconomyManager getEconomyManager();

    /**
     * Get the CooldownManager
     * @return The cooldown manager instance
     */
    CooldownManager getCooldownManager();

    /**
     * Get the GameManager
     * @return The game manager instance
     */
    GameManager getGameManager();

    /**
     * Get the AntiAbuseManager
     * @return The anti-abuse manager instance
     */
    AntiAbuseManager getAntiAbuseManager();

    /**
     * Get the player stats manager.
     * @return Stats manager
     */
    PlayerStatsManager getPlayerStatsManager();

    /**
     * Get the UX manager.
     * @return UX manager
     */
    UxManager getUxManager();

    LocaleManager getLocaleManager();

    ProtectionManager getProtectionManager();

    PluginVariant getVariant();
}
