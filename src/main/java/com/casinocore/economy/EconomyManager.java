package com.casinocore.economy;

import com.casinocore.core.CasinoPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all economy operations for the casino.
 * Player-scoped synchronization prevents overlapping withdraw/deposit races.
 */
public class EconomyManager {

    private final CasinoPlugin plugin;
    private final Map<UUID, Object> accountLocks;
    private Economy economy;
    private boolean vaultAvailable;

    public EconomyManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.accountLocks = new ConcurrentHashMap<>();
        this.vaultAvailable = false;
    }

    public boolean setupEconomy() {
        if (!plugin.getConfigManager().isVaultEnabled()) {
            plugin.getPlugin().getLogger().info("Vault integration is disabled in config");
            return false;
        }

        if (plugin.getPlugin().getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getPlugin().getLogger().warning("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
            plugin.getPlugin().getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getPlugin().getLogger().warning("No economy plugin found!");
            return false;
        }

        economy = rsp.getProvider();
        vaultAvailable = true;
        plugin.getPlugin().getLogger().info("Hooked into " + economy.getName() + " via Vault");
        return true;
    }

    public boolean isAvailable() {
        return vaultAvailable && economy != null;
    }

    public double getBalance(Player player) {
        if (!isAvailable()) {
            return 0.0;
        }

        synchronized (getLock(player.getUniqueId())) {
            try {
                return economy.getBalance(player);
            } catch (Exception e) {
                plugin.getPlugin().getLogger().severe(
                    "Error getting balance for " + player.getName() + ": " + e.getMessage()
                );
                return 0.0;
            }
        }
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0.0;
        }

        synchronized (getLock(player.getUniqueId())) {
            try {
                return economy.getBalance(player);
            } catch (Exception e) {
                plugin.getPlugin().getLogger().severe(
                    "Error getting balance for " + player.getName() + ": " + e.getMessage()
                );
                return 0.0;
            }
        }
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isAvailable() || amount < 0) {
            return false;
        }

        synchronized (getLock(player.getUniqueId())) {
            try {
                return economy.has(player, amount);
            } catch (Exception e) {
                plugin.getPlugin().getLogger().severe(
                    "Error checking balance for " + player.getName() + ": " + e.getMessage()
                );
                return false;
            }
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) {
            plugin.getPlugin().getLogger().warning(
                "Attempted to withdraw from " + player.getName() + " but economy is unavailable"
            );
            return false;
        }

        if (amount < 0) {
            plugin.getPlugin().getLogger().warning(
                "Attempted to withdraw negative amount from " + player.getName()
            );
            return false;
        }

        synchronized (getLock(player.getUniqueId())) {
            if (!hasBalanceUnlocked(player, amount)) {
                return false;
            }

            try {
                EconomyResponse response = economy.withdrawPlayer(player, amount);
                if (!response.transactionSuccess()) {
                    plugin.getPlugin().getLogger().warning(
                        "Failed to withdraw " + amount + " from " + player.getName() + ": " + response.errorMessage
                    );
                    return false;
                }

                return true;
            } catch (Exception e) {
                plugin.getPlugin().getLogger().severe(
                    "Error withdrawing from " + player.getName() + ": " + e.getMessage()
                );
                return false;
            }
        }
    }

    public boolean deposit(Player player, double amount) {
        if (!isAvailable()) {
            plugin.getPlugin().getLogger().warning(
                "Attempted to deposit to " + player.getName() + " but economy is unavailable"
            );
            return false;
        }

        if (amount < 0) {
            plugin.getPlugin().getLogger().warning(
                "Attempted to deposit negative amount to " + player.getName()
            );
            return false;
        }

        synchronized (getLock(player.getUniqueId())) {
            try {
                EconomyResponse response = economy.depositPlayer(player, amount);
                if (!response.transactionSuccess()) {
                    plugin.getPlugin().getLogger().warning(
                        "Failed to deposit " + amount + " to " + player.getName() + ": " + response.errorMessage
                    );
                    return false;
                }

                return true;
            } catch (Exception e) {
                plugin.getPlugin().getLogger().severe(
                    "Error depositing to " + player.getName() + ": " + e.getMessage()
                );
                return false;
            }
        }
    }

    public String format(double amount) {
        if (isAvailable()) {
            try {
                return economy.format(amount);
            } catch (Exception e) {
                plugin.getPlugin().getLogger().warning("Error formatting amount: " + e.getMessage());
            }
        }

        return plugin.getConfigManager().getCurrencySymbol() + String.format("%.2f", amount);
    }

    public String getEconomyName() {
        return isAvailable() ? economy.getName() : "None";
    }

    public void shutdown() {
        this.economy = null;
        this.vaultAvailable = false;
        this.accountLocks.clear();
    }

    private Object getLock(UUID playerId) {
        return accountLocks.computeIfAbsent(playerId, ignored -> new Object());
    }

    private boolean hasBalanceUnlocked(Player player, double amount) {
        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            plugin.getPlugin().getLogger().severe(
                "Error checking balance for " + player.getName() + ": " + e.getMessage()
            );
            return false;
        }
    }
}
