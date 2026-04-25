package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomBetManager {

    private static final Map<UUID, PendingCustomBet> PENDING_BETS = new ConcurrentHashMap<>();

    private CustomBetManager() {
    }

    public static void prompt(CasinoPlugin plugin, Player player) {
        PENDING_BETS.put(player.getUniqueId(), new PendingCustomBet(player.getUniqueId()));
        player.closeInventory();
        plugin.getMessageManager().send(
            player,
            plugin.getUxManager().formatGameMessage(
                "gold",
                plugin.getLocaleManager().getText("custom-bet.title"),
                plugin.getLocaleManager().getText("custom-bet.prompt"),
                plugin.getLocaleManager().getText("custom-bet.cancel-hint"),
                plugin.getLocaleManager().getText("custom-bet.range-hint")
            )
        );
    }

    public static boolean isPrompting(Player player) {
        return PENDING_BETS.containsKey(player.getUniqueId());
    }

    public static void cancel(Player player) {
        PENDING_BETS.remove(player.getUniqueId());
    }

    public static void handleInput(CasinoPlugin plugin, Player player, String message) {
        PendingCustomBet pending = PENDING_BETS.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (message.equalsIgnoreCase("cancel")) {
            plugin.getMessageManager().send(player, plugin.getLocaleManager().getText("custom-bet.cancelled"));
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> new CasinoHubGUI(plugin, player).open());
            return;
        }

        double bet;
        try {
            bet = Double.parseDouble(message);
        } catch (NumberFormatException exception) {
            plugin.getMessageManager().send(player, plugin.getLocaleManager().getText("custom-bet.invalid-number"));
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> new CasinoHubGUI(plugin, player).open());
            return;
        }

        double min = plugin.getConfigManager().getMinBet();
        double max = plugin.getConfigManager().getMaxBet();
        if (bet < min || bet > max) {
            plugin.getMessageManager().send(player, plugin.getLocaleManager().formatText("custom-bet.out-of-range", Map.of(
                "min", plugin.getEconomyManager().format(min),
                "max", plugin.getEconomyManager().format(max)
            )));
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> new CasinoHubGUI(plugin, player).open());
            return;
        }

        double normalizedBet = Math.round(bet * 100.0) / 100.0;
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
            CasinoHubGUI.setSelectedBet(player.getUniqueId(), normalizedBet);
            plugin.getMessageManager().send(
                player,
                plugin.getLocaleManager().formatText("custom-bet.selected", Map.of(
                    "amount", plugin.getEconomyManager().format(normalizedBet)
                ))
            );
            new CasinoHubGUI(plugin, player).open();
        });
    }

    private record PendingCustomBet(UUID playerId) {
    }
}
