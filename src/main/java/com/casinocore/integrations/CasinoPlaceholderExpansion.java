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
        String key = params.toLowerCase();
        if (player == null && !key.equals("available_games") && !key.equals("economy")) {
            return "";
        }

        return switch (key) {
            case "balance" -> plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player));
            case "balance_raw" -> String.format("%.2f", plugin.getEconomyManager().getBalance(player));
            case "wins" -> String.valueOf(plugin.getPlayerStatsManager().getWins(player.getUniqueId()));
            case "losses" -> String.valueOf(plugin.getPlayerStatsManager().getLosses(player.getUniqueId()));
            case "games_played" -> String.valueOf(plugin.getPlayerStatsManager().getGamesPlayed(player.getUniqueId()));
            case "streak" -> String.valueOf(plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId()));
            case "best_streak" -> String.valueOf(plugin.getPlayerStatsManager().getBestWinStreak(player.getUniqueId()));
            case "daily_ready" -> String.valueOf(plugin.getPlayerStatsManager().canClaimDaily(player.getUniqueId()));
            case "next_daily" -> plugin.getUxManager().formatNextDailyClaim(player);
            case "available_games" -> String.valueOf(plugin.getGameManager().getEnabledCasinoGames().size());
            case "economy" -> plugin.getEconomyManager().getEconomyName();
            default -> null;
        };
    }
}
