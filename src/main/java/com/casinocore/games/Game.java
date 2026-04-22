package com.casinocore.games;

import org.bukkit.entity.Player;

/**
 * Interface for all casino games
 */
public interface Game {

    /**
     * Get the unique identifier for this game
     * @return The game ID (lowercase, no spaces)
     */
    String getId();

    /**
     * Get the display name of the game
     * @return The game display name
     */
    String getName();

    /**
     * Get the description of the game
     * @return The game description
     */
    String getDescription();

    /**
     * Check if the game is enabled
     * @return true if game is enabled
     */
    boolean isEnabled();

    /**
     * Set whether the game is enabled
     * @param enabled true to enable the game
     */
    void setEnabled(boolean enabled);

    /**
     * Start the game for a player
     * @param player The player starting the game
     * @param betAmount The bet amount (if applicable)
     */
    void start(Player player, double betAmount);

    /**
     * Check if a player can play this game
     * @param player The player to check
     * @return true if player meets all requirements
     */
    boolean canPlay(Player player);

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
     * Get the cooldown time for this game in seconds
     * @return Cooldown time in seconds
     */
    int getCooldown();

    /**
     * Called when the game is loaded/registered
     */
    void onLoad();

    /**
     * Called when the game is unloaded/disabled
     */
    void onUnload();
}
