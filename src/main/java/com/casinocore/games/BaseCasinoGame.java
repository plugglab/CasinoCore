package com.casinocore.games;

import com.casinocore.api.event.CasinoBigWinEvent;
import com.casinocore.api.event.CasinoGameResultEvent;
import com.casinocore.core.CasinoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Abstract base class for casino games
 * Provides common functionality: bet validation, cooldowns, economy checks, error handling
 */
public abstract class BaseCasinoGame implements CasinoGame {

    protected final CasinoPlugin plugin;
    protected final String name;
    protected final String displayName;
    protected final String description;
    protected boolean enabled;

    protected BaseCasinoGame(CasinoPlugin plugin, String name, String displayName, String description) {
        this.plugin = plugin;
        this.name = name.toLowerCase().replaceAll("\\s+", "");
        this.displayName = displayName;
        this.description = description;
        this.enabled = true;
    }

    @Override
    public boolean play(Player player, double bet) {
        try {
            // Pre-game validation
            if (!preGameValidation(player, bet)) {
                return false;
            }

            // Withdraw bet amount
            if (!withdrawBet(player, bet)) {
                return false;
            }

            // Set cooldown
            setCooldown(player);

            // Execute the actual game logic
            boolean result = executeGame(player, bet);

            // Log game execution
            logGame(player, bet, result);

            return result;

        } catch (Exception e) {
            handleGameError(player, bet, e);
            return false;
        }
    }

    /**
     * Execute the actual game logic
     * Subclasses must implement this method
     * @param player The player
     * @param bet The bet amount
     * @return true if game executed successfully
     */
    protected abstract boolean executeGame(Player player, double bet);

    /**
     * Pre-game validation - checks all requirements before playing
     * @param player The player
     * @param bet The bet amount
     * @return true if all checks pass
     */
    protected boolean preGameValidation(Player player, double bet) {
        try {
            // Check if game is enabled
            if (!isEnabled()) {
                sendMessage(player, "<red>This game is currently disabled!</red>");
                logDebug("Player " + player.getName() + " tried to play disabled game: " + name);
                return false;
            }

            // Check if player can play (permissions, cooldowns)
            if (!canPlay(player)) {
                return false;
            }

            if (!plugin.getAntiAbuseManager().canStartGame(player, name, bet)) {
                logDebug("Player " + player.getName() + " blocked by anti-abuse rules for game " + name);
                return false;
            }

            // Validate bet amount
            if (!validateBet(player, bet)) {
                return false;
            }

            // Check if player has sufficient funds
            if (!checkBalance(player, bet)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error in pre-game validation for " + name + " (player: " + player.getName() + ")", e);
            sendMessage(player, "<red>An error occurred. Please try again later.</red>");
            return false;
        }
    }

    /**
     * Validate bet amount
     * @param player The player
     * @param bet The bet amount
     * @return true if bet is valid
     */
    protected boolean validateBet(Player player, double bet) {
        try {
            if (bet < getMinBet()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("min", plugin.getEconomyManager().format(getMinBet()));
                placeholders.put("game", getDisplayName());
                sendMessage(player, "<red>Minimum bet for " + getDisplayName() + " is " +
                    plugin.getEconomyManager().format(getMinBet()) + "</red>");
                logDebug("Player " + player.getName() + " bet too low: " + bet + " (min: " + getMinBet() + ")");
                return false;
            }

            if (bet > getMaxBet()) {
                sendMessage(player, "<red>Maximum bet for " + getDisplayName() + " is " +
                    plugin.getEconomyManager().format(getMaxBet()) + "</red>");
                logDebug("Player " + player.getName() + " bet too high: " + bet + " (max: " + getMaxBet() + ")");
                return false;
            }

            if (bet <= 0) {
                sendMessage(player, "<red>Bet must be greater than zero!</red>");
                logDebug("Player " + player.getName() + " tried negative/zero bet: " + bet);
                return false;
            }

            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.WARNING,
                "Error validating bet for " + player.getName(), e);
            return false;
        }
    }

    /**
     * Check if player has sufficient balance
     * @param player The player
     * @param amount The amount to check
     * @return true if player has enough balance
     */
    protected boolean checkBalance(Player player, double amount) {
        try {
            if (!plugin.getEconomyManager().isAvailable()) {
                plugin.getMessageManager().sendMessage(player, "economy-unavailable");
                logDebug("Economy unavailable for player " + player.getName());
                return false;
            }

            if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", plugin.getEconomyManager().format(amount));
                plugin.getMessageManager().sendMessage(player, "insufficient-funds", placeholders);
                logDebug("Player " + player.getName() + " has insufficient funds. Required: " + amount);
                return false;
            }

            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error checking balance for " + player.getName(), e);
            return false;
        }
    }

    /**
     * Withdraw bet from player
     * @param player The player
     * @param amount The amount to withdraw
     * @return true if withdrawal successful
     */
    protected boolean withdrawBet(Player player, double amount) {
        try {
            boolean success = plugin.getEconomyManager().withdraw(player, amount);

            if (!success) {
                sendMessage(player, "<red>Failed to process bet. Please try again.</red>");
                plugin.getPlugin().getLogger().warning(
                    "Failed to withdraw bet from " + player.getName() + " (amount: " + amount + ")");
                return false;
            }

            logDebug("Withdrew " + amount + " from " + player.getName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error withdrawing bet from " + player.getName(), e);
            sendMessage(player, "<red>An error occurred processing your bet.</red>");
            return false;
        }
    }

    /**
     * Pay out winnings to player
     * @param player The player
     * @param amount The amount to pay
     * @return true if payout successful
     */
    protected boolean payWinnings(Player player, double amount) {
        try {
            if (amount <= 0) {
                logDebug("Skipping payout for " + player.getName() + " (amount: " + amount + ")");
                return true;
            }

            boolean success = plugin.getEconomyManager().deposit(player, amount);

            if (!success) {
                plugin.getPlugin().getLogger().severe(
                    "CRITICAL: Failed to deposit winnings to " + player.getName() +
                    " (amount: " + amount + ") - Player lost money!");
                // Attempt to refund original bet if possible
                return false;
            }

            logDebug("Paid " + amount + " to " + player.getName());
            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "CRITICAL ERROR: Failed to pay winnings to " + player.getName(), e);
            return false;
        }
    }

    /**
     * Set cooldown for player
     * @param player The player
     */
    protected void setCooldown(Player player) {
        try {
            int cooldown = getCooldownSeconds();
            if (cooldown > 0) {
                plugin.getCooldownManager().setCooldown(player, name, cooldown);
                logDebug("Set cooldown for " + player.getName() + ": " + cooldown + "s");
            }
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.WARNING,
                "Error setting cooldown for " + player.getName(), e);
        }
    }

    @Override
    public boolean canPlay(Player player) {
        try {
            // Check permission
            if (!player.hasPermission(getPermission())) {
                plugin.getMessageManager().sendMessage(player, "no-permission");
                logDebug("Player " + player.getName() + " lacks permission: " + getPermission());
                return false;
            }

            // Check cooldown
            if (plugin.getCooldownManager().hasCooldown(player, name)) {
                int remaining = plugin.getCooldownManager().getRemainingCooldown(player, name);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(remaining));
                plugin.getMessageManager().sendMessage(player, "cooldown-active", placeholders);
                logDebug("Player " + player.getName() + " on cooldown: " + remaining + "s remaining");
                return false;
            }

            // Check economy availability
            if (!plugin.getEconomyManager().isAvailable()) {
                plugin.getMessageManager().sendMessage(player, "economy-unavailable");
                logDebug("Economy unavailable for " + player.getName());
                return false;
            }

            return true;

        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error checking if player can play: " + player.getName(), e);
            return false;
        }
    }

    /**
     * Handle game execution errors
     * @param player The player
     * @param bet The bet amount
     * @param e The exception
     */
    protected void handleGameError(Player player, double bet, Exception e) {
        plugin.getPlugin().getLogger().log(Level.SEVERE,
            "Error executing game " + name + " for player " + player.getName() +
            " (bet: " + bet + ")", e);

        sendMessage(player, "<red>An error occurred during the game. Please contact an administrator.</red>");

        // Attempt to refund the bet
        try {
            boolean refunded = plugin.getEconomyManager().deposit(player, bet);
            if (refunded) {
                sendMessage(player, "<yellow>Your bet has been refunded.</yellow>");
                plugin.getPlugin().getLogger().info(
                    "Refunded bet to " + player.getName() + " (amount: " + bet + ")");
            } else {
                plugin.getPlugin().getLogger().severe(
                    "CRITICAL: Failed to refund bet to " + player.getName() +
                    " (amount: " + bet + ") after game error!");
            }
        } catch (Exception refundError) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "CRITICAL: Exception while refunding bet to " + player.getName(), refundError);
        }
    }

    /**
     * Log game execution
     * @param player The player
     * @param bet The bet amount
     * @param success Whether game executed successfully
     */
    protected void logGame(Player player, double bet, boolean success) {
        try {
            logDebug("Game " + name + " executed for " + player.getName() +
                " (bet: " + bet + ", success: " + success + ")");
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    /**
     * Send a message to player
     * @param player The player
     * @param message The message
     */
    protected void sendMessage(Player player, String message) {
        try {
            plugin.getMessageManager().send(player, message);
        } catch (Exception e) {
            // Fallback to plain message
            player.sendMessage(message);
        }
    }

    /**
     * Log debug message
     * @param message The message
     */
    protected void logDebug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getPlugin().getLogger().info("[DEBUG] [" + name + "] " + message);
        }
    }

    // CasinoGame interface implementation

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logDebug("Game " + name + " " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public double getMinBet() {
        return plugin.getConfigManager().getMinBet(name);
    }

    @Override
    public double getMaxBet() {
        return plugin.getConfigManager().getMaxBet(name);
    }

    @Override
    public int getCooldownSeconds() {
        return plugin.getConfigManager().getGameCooldown(name);
    }

    @Override
    public String getPermission() {
        return "casinocore.game." + name;
    }

    protected void handleWin(Player player, double bet, double payout) {
        plugin.getPlayerStatsManager().recordWin(player.getUniqueId());
        int streak = plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId());
        plugin.getUxManager().playWinPresentation(player, displayName, payout, payout - bet, streak);
        Bukkit.getPluginManager().callEvent(new CasinoGameResultEvent(player, name, bet, payout, true));

        boolean broadcastsEnabled = plugin.getConfigManager().getConfig().getBoolean("broadcasts.big-win.enabled", true);
        double bigWinThreshold = plugin.getConfigManager().getConfig().getDouble("broadcasts.big-win.min-payout", 1000.0);
        if (broadcastsEnabled && payout >= bigWinThreshold) {
            String message = plugin.getConfigManager().getConfig().getString(
                "broadcasts.big-win.message",
                "<gold><bold>{player}</bold></gold> won <green>{amount}</green> in <yellow>{game}</yellow>!"
            );
            message = message
                .replace("{player}", player.getName())
                .replace("{amount}", plugin.getEconomyManager().format(payout))
                .replace("{game}", displayName);
            plugin.getMessageManager().broadcast(message);
            Bukkit.getPluginManager().callEvent(new CasinoBigWinEvent(player, name, bet, payout));
        }
    }

    protected void handleLoss(Player player, double bet) {
        plugin.getPlayerStatsManager().recordLoss(player.getUniqueId());
        plugin.getUxManager().playLossPresentation(player, displayName);
        Bukkit.getPluginManager().callEvent(new CasinoGameResultEvent(player, name, bet, 0.0, false));
    }

    @Override
    public void onEnable() {
        plugin.getPlugin().getLogger().info("Enabled game: " + displayName + " (" + name + ")");
    }

    @Override
    public void onDisable() {
        plugin.getPlugin().getLogger().info("Disabled game: " + displayName + " (" + name + ")");
    }
}
