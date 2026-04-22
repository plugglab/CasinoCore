package com.casinocore.integrations;

import com.casinocore.core.CasinoPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class CasinoPlaceholderExpansion extends PlaceholderExpansion {

    private final CasinoPlugin plugin;

    public CasinoPlaceholderExpansion(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "casino";
    }

    @Override
    public String getAuthor() {
        return "CasinoCore Team";
    }

    @Override
    public String getVersion() {
        return plugin.getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "balance" -> plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player));
            case "wins" -> String.valueOf(plugin.getPlayerStatsManager().getWins(player.getUniqueId()));
            case "streak" -> String.valueOf(plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId()));
            case "best_streak" -> String.valueOf(plugin.getPlayerStatsManager().getBestWinStreak(player.getUniqueId()));
            default -> null;
        };
    }
}
