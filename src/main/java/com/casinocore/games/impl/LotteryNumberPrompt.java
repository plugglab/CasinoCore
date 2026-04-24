package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LotteryNumberPrompt {

    private static final Map<UUID, PendingLotteryEntry> PENDING = new ConcurrentHashMap<>();

    private LotteryNumberPrompt() {
    }

    public static void prompt(CasinoPlugin plugin, LotteryGame game, Player player, double bet) {
        PENDING.put(player.getUniqueId(), new PendingLotteryEntry(game, bet));
        plugin.getMessageManager().send(
            player,
            plugin.getUxManager().formatGameMessage(
                "aqua",
                "Lottery Number",
                "<white>Type your lottery number in chat.</white>",
                "<gray>Range: 1-100</gray>",
                "<gray>Type <yellow>cancel</yellow> to go back.</gray>"
            )
        );
    }

    public static boolean isWaiting(Player player) {
        return PENDING.containsKey(player.getUniqueId());
    }

    public static void cancel(Player player) {
        PENDING.remove(player.getUniqueId());
    }

    public static void handleInput(CasinoPlugin plugin, Player player, String message) {
        PendingLotteryEntry pending = PENDING.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (message.equalsIgnoreCase("cancel")) {
            plugin.getMessageManager().send(player, "<yellow>Lottery entry cancelled.</yellow>");
            GuiNavigation.openHub(plugin, player);
            return;
        }

        int number;
        try {
            number = Integer.parseInt(message);
        } catch (NumberFormatException exception) {
            plugin.getMessageManager().send(player, "<red>Enter a valid whole number.</red>");
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> prompt(plugin, pending.game(), player, pending.bet()));
            return;
        }

        if (number < 1 || number > 100) {
            plugin.getMessageManager().send(player, "<red>Lottery number must be between 1 and 100.</red>");
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> prompt(plugin, pending.game(), player, pending.bet()));
            return;
        }

        int chosenNumber = number;
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> pending.game().playPickedNumber(player, pending.bet(), chosenNumber));
    }

    private record PendingLotteryEntry(LotteryGame game, double bet) {
    }
}
