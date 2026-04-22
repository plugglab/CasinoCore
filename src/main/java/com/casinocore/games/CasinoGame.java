package com.casinocore.games;

import org.bukkit.entity.Player;

/**
 * Interface for all casino games
 * This is the core interface that all games must implement
 */
public interface CasinoGame {

    /**
     * Play the game
     * @param player The player playing the game
     * @param bet The bet amount
     * @return true if the game was played successfully
     */
    boolean play(Player player, double bet);

    /**
     * Get the unique name/identifier of the game
     * @return The game name (lowercase, no spaces)
     */
    String getName();

    /**
     * Get the display name of the game
     * @return The display name
     */
    String getDisplayName();

    /**
     * Get the minimum bet for this game
     * @return Minimum bet amount
     */
    double getMinBet();

    /**
     * Get the maximum bet for this game
     * @return Maximum bet amount
     */
    double getMaxBet();

    /**
     * Get the description of the game
     * @return Game description
     */
    String getDescription();

    /**
     * Check if the game is currently enabled
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Set the enabled state of the game
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Get the cooldown time for this game in seconds
     * @return Cooldown in seconds
     */
    int getCooldownSeconds();

    /**
     * Get the permission required to play this game
     * @return Permission string (e.g., "casinocore.game.coinflip")
     */
    String getPermission();

    /**
     * Check if a player can play this game
     * Checks permissions, cooldowns, economy, etc.
     * @param player The player
     * @return true if player can play
     */
    boolean canPlay(Player player);

    /**
     * Called when the game is loaded/registered
     */
    void onEnable();

    /**
     * Called when the game is unloaded/disabled
     */
    void onDisable();
}
