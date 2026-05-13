package com.casinocore.games;

import com.casinocore.core.CasinoPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all casino games
 * Thread-safe game registration and access with full error handling
 */
public class GameManager {

    private final CasinoPlugin plugin;
    private final Map<String, CasinoGame> casinoGames;
    private final Map<String, Game> games; // Legacy support

    public GameManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.casinoGames = new ConcurrentHashMap<>();
        this.games = new ConcurrentHashMap<>();
    }

    /**
     * Load and register all games
     */
    public void loadGames() {
        try {
            // Register built-in games here
            // Example:
            // registerCasinoGame(new CoinFlipGame(plugin));
            // registerCasinoGame(new DiceGame(plugin));
            // registerCasinoGame(new RouletteGame(plugin));

            plugin.getPlugin().getLogger().info(
                "Loaded " + casinoGames.size() + " casino game(s)");

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error loading games", e);
        }
    }

    /**
     * Register a CasinoGame
     * @param game The casino game to register
     * @return true if registration was successful
     */
    public boolean registerCasinoGame(CasinoGame game) {
        try {
            if (game == null) {
                plugin.getPlugin().getLogger().warning("Attempted to register null game!");
                return false;
            }

            String gameName = game.getName();
            if (gameName == null || gameName.isEmpty()) {
                plugin.getPlugin().getLogger().warning(
                    "Attempted to register game with null/empty name!");
                return false;
            }

            gameName = gameName.toLowerCase();

            if (casinoGames.containsKey(gameName)) {
                plugin.getPlugin().getLogger().warning(
                    "Game '" + gameName + "' is already registered!");
                return false;
            }

            casinoGames.put(gameName, game);
            game.setEnabled(plugin.getConfigManager().getConfig().getBoolean("games." + gameName + ".enabled", true));
            game.onEnable();

            plugin.getPlugin().getLogger().info(
                "Registered casino game: " + game.getDisplayName() + " (" + gameName + ")");
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error registering casino game", e);
            return false;
        }
    }

    /**
     * Register a legacy Game
     * @param game The game to register
     * @return true if registration was successful
     */
    public boolean registerGame(Game game) {
        try {
            if (games.containsKey(game.getId())) {
                plugin.getPlugin().getLogger().warning(
                    "Game with ID '" + game.getId() + "' is already registered!"
                );
                return false;
            }

            games.put(game.getId(), game);
            game.onLoad();

            plugin.getPlugin().getLogger().info("Registered game: " + game.getName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error registering game", e);
            return false;
        }
    }

    /**
     * Unregister a casino game
     * @param gameName The game name to unregister
     * @return true if unregistration was successful
     */
    public boolean unregisterCasinoGame(String gameName) {
        try {
            if (gameName == null || gameName.isEmpty()) {
                return false;
            }

            CasinoGame game = casinoGames.remove(gameName.toLowerCase());

            if (game == null) {
                return false;
            }

            game.onDisable();
            plugin.getPlugin().getLogger().info(
                "Unregistered casino game: " + game.getDisplayName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error unregistering casino game: " + gameName, e);
            return false;
        }
    }

    /**
     * Unregister a legacy game
     * @param gameId The game ID to unregister
     * @return true if unregistration was successful
     */
    public boolean unregisterGame(String gameId) {
        try {
            Game game = games.remove(gameId);

            if (game == null) {
                return false;
            }

            game.onUnload();
            plugin.getPlugin().getLogger().info("Unregistered game: " + game.getName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error unregistering game: " + gameId, e);
            return false;
        }
    }

    /**
     * Get a casino game by name
     * @param gameName The game name
     * @return Optional containing the game if found
     */
    public Optional<CasinoGame> getCasinoGame(String gameName) {
        try {
            if (gameName == null || gameName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(casinoGames.get(gameName.toLowerCase()));
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.WARNING,
                "Error getting casino game: " + gameName, e);
            return Optional.empty();
        }
    }

    /**
     * Get a casino game by name (direct access)
     * @param gameName The game name
     * @return The game, or null if not found
     */
    public CasinoGame getCasinoGameDirect(String gameName) {
        if (gameName == null || gameName.isEmpty()) {
            return null;
        }
        return casinoGames.get(gameName.toLowerCase());
    }

    /**
     * Get a legacy game by ID
     * @param gameId The game ID
     * @return Optional containing the game if found
     */
    public Optional<Game> getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId.toLowerCase()));
    }

    /**
     * Get all registered casino games
     * @return Map of all casino games
     */
    public Map<String, CasinoGame> getAllCasinoGames() {
        return new HashMap<>(casinoGames);
    }

    /**
     * Get all enabled casino games
     * @return Map of enabled casino games
     */
    public Map<String, CasinoGame> getEnabledCasinoGames() {
        Map<String, CasinoGame> enabledGames = new HashMap<>();
        for (Map.Entry<String, CasinoGame> entry : casinoGames.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabledGames.put(entry.getKey(), entry.getValue());
            }
        }
        return enabledGames;
    }

    /**
     * Get list of all casino game names
     * @return List of game names
     */
    public List<String> getCasinoGameNames() {
        return new ArrayList<>(casinoGames.keySet());
    }

    /**
     * Check if a casino game exists
     * @param gameName The game name
     * @return true if game exists
     */
    public boolean casinoGameExists(String gameName) {
        if (gameName == null || gameName.isEmpty()) {
            return false;
        }
        return casinoGames.containsKey(gameName.toLowerCase());
    }

    /**
     * Get all registered legacy games
     * @return Map of all games
     */
    public Map<String, Game> getGames() {
        return new HashMap<>(games);
    }

    /**
     * Get all enabled legacy games
     * @return Map of enabled games
     */
    public Map<String, Game> getEnabledGames() {
        Map<String, Game> enabledGames = new HashMap<>();
        for (Map.Entry<String, Game> entry : games.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabledGames.put(entry.getKey(), entry.getValue());
            }
        }
        return enabledGames;
    }

    /**
     * Check if a legacy game exists
     * @param gameId The game ID
     * @return true if game exists
     */
    public boolean gameExists(String gameId) {
        return games.containsKey(gameId.toLowerCase());
    }

    /**
     * Enable a casino game
     * @param gameName The game name
     * @return true if successful
     */
    public boolean enableCasinoGame(String gameName) {
        try {
            CasinoGame game = casinoGames.get(gameName.toLowerCase());
            if (game == null) {
                return false;
            }

            if (game.isEnabled()) {
                return true;
            }

            game.setEnabled(true);
            plugin.getConfigManager().getConfig().set("games." + game.getName() + ".enabled", true);
            plugin.getConfigManager().saveConfig();
            game.onEnable();
            plugin.getPlugin().getLogger().info(
                "Enabled casino game: " + game.getDisplayName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error enabling casino game: " + gameName, e);
            return false;
        }
    }

    /**
     * Disable a casino game
     * @param gameName The game name
     * @return true if successful
     */
    public boolean disableCasinoGame(String gameName) {
        try {
            CasinoGame game = casinoGames.get(gameName.toLowerCase());
            if (game == null) {
                return false;
            }

            if (!game.isEnabled()) {
                return true;
            }

            game.setEnabled(false);
            plugin.getConfigManager().getConfig().set("games." + game.getName() + ".enabled", false);
            plugin.getConfigManager().saveConfig();
            game.onDisable();
            plugin.getPlugin().getLogger().info(
                "Disabled casino game: " + game.getDisplayName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error disabling casino game: " + gameName, e);
            return false;
        }
    }

    /**
     * Enable a legacy game
     * @param gameId The game ID
     * @return true if successful
     */
    public boolean enableGame(String gameId) {
        Game game = games.get(gameId.toLowerCase());
        if (game == null) {
            return false;
        }

        game.setEnabled(true);
        plugin.getPlugin().getLogger().info("Enabled game: " + game.getName());
        return true;
    }

    /**
     * Disable a legacy game
     * @param gameId The game ID
     * @return true if successful
     */
    public boolean disableGame(String gameId) {
        Game game = games.get(gameId.toLowerCase());
        if (game == null) {
            return false;
        }

        game.setEnabled(false);
        plugin.getPlugin().getLogger().info("Disabled game: " + game.getName());
        return true;
    }

    /**
     * Reload all games
     */
    public void reloadGames() {
        try {
            for (CasinoGame game : casinoGames.values()) {
                game.setEnabled(plugin.getConfigManager().getConfig().getBoolean("games." + game.getName() + ".enabled", true));
            }
            plugin.getPlugin().getLogger().info("Reloaded game configuration for " + casinoGames.size() + " casino game(s)");

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error reloading games", e);
        }
    }

    /**
     * Shutdown the game manager
     */
    public void shutdown() {
        try {
            // Unload all casino games
            for (CasinoGame game : casinoGames.values()) {
                try {
                    game.onDisable();
                } catch (Exception e) {
                    plugin.getPlugin().getLogger().log(Level.WARNING,
                        "Error disabling casino game during shutdown", e);
                }
            }

            casinoGames.clear();

            // Unload all legacy games
            for (Game game : games.values()) {
                try {
                    game.onUnload();
                } catch (Exception e) {
                    plugin.getPlugin().getLogger().log(Level.WARNING,
                        "Error unloading legacy game during shutdown", e);
                }
            }

            games.clear();
            plugin.getPlugin().getLogger().info("Game manager shutdown complete");

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error during game manager shutdown", e);
        }
    }
}
