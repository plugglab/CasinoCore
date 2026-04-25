package com.casinocore.games;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Abstract base class for casino games
 * Provides common functionality for all games
 */
public abstract class AbstractGame implements Game {

    protected final CasinoPlugin plugin;
    protected final String id;
    protected final String name;
    protected final String description;
    protected boolean enabled;

    protected AbstractGame(CasinoPlugin plugin, String id, String name, String description) {
        this.plugin = plugin;
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
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
    }

    @Override
    public boolean canPlay(Player player) {
        if (!isEnabled()) {
            return false;
        }

        if (!player.hasPermission("casinocore.game." + id)) {
            return false;
        }

        if (!plugin.getEconomyManager().isAvailable()) {
            plugin.getMessageManager().sendMessage(player, "economy-unavailable");
            return false;
        }

        return true;
    }

    @Override
    public double getMinBet() {
        return plugin.getConfigManager().getMinBet();
    }

    @Override
    public double getMaxBet() {
        return plugin.getConfigManager().getMaxBet();
    }

    @Override
    public int getCooldown() {
        return plugin.getConfigManager().getGameCooldown(id);
    }

    @Override
    public void onLoad() {
        plugin.getPlugin().getLogger().info("Loaded game: " + name);
    }

    @Override
    public void onUnload() {
        plugin.getPlugin().getLogger().info("Unloaded game: " + name);
    }

    protected boolean validateBet(Player player, double amount) {
        if (amount < getMinBet()) {
            plugin.getMessageManager().send(
                player,
                plugin.getLocaleManager().formatText("game.min-bet-generic", Map.of(
                    "min", plugin.getEconomyManager().format(getMinBet())
                ))
            );
            return false;
        }

        if (amount > getMaxBet()) {
            plugin.getMessageManager().send(
                player,
                plugin.getLocaleManager().formatText("game.max-bet-generic", Map.of(
                    "max", plugin.getEconomyManager().format(getMaxBet())
                ))
            );
            return false;
        }

        if (!plugin.getEconomyManager().hasBalance(player, amount)) {
            Map<String, String> placeholders = plugin.getMessageManager().createPlaceholderMap();
            placeholders.put("amount", plugin.getEconomyManager().format(amount));
            plugin.getMessageManager().sendMessage(player, "insufficient-funds", placeholders);
            return false;
        }

        return true;
    }

    protected boolean checkCooldown(Player player) {
        return plugin.getCooldownManager().checkAndSetCooldown(player, id);
    }
}
