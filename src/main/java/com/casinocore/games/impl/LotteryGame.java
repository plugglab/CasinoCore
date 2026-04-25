package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LotteryGame extends BaseCasinoGame {

    private final Map<UUID, LotteryDrawGUI> openDraws;

    public LotteryGame(CasinoPlugin plugin) {
        super(plugin, "lottery", "Lottery", "Pick your number, watch the ball draw, and hit the winning ball.");
        this.openDraws = new ConcurrentHashMap<>();
    }

    @Override
    public boolean play(Player player, double bet) {
        if (!preGameValidation(player, bet)) {
            return false;
        }

        if (openDraws.containsKey(player.getUniqueId())) {
            sendLocaleMessage(player, "lottery.already-open");
            return false;
        }

        LotteryNumberPrompt.prompt(plugin, this, player, bet);
        return true;
    }

    public void playPickedNumber(Player player, double bet, int pickedNumber) {
        boolean betWithdrawn = false;
        try {
            if (!preGameValidation(player, bet)) {
                return;
            }

            if (!withdrawBet(player, bet)) {
                return;
            }
            betWithdrawn = true;

            setCooldown(player);
            LotteryDrawGUI gui = new LotteryDrawGUI(plugin, player, pickedNumber, bet);
            openDraws.put(player.getUniqueId(), gui);
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                gui.open();
                int winningNumber = ThreadLocalRandom.current().nextInt(1, 101);
                gui.startDraw(winningNumber, () -> finishDraw(player, gui, pickedNumber, winningNumber, bet));
            });
        } catch (Exception exception) {
            handleGameError(player, bet, exception, betWithdrawn);
        }
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    private void finishDraw(Player player, LotteryDrawGUI gui, int pickedNumber, int winningNumber, double bet) {
        int difference = Math.abs(winningNumber - pickedNumber);
        double multiplier = 0.0;
        boolean won = false;
        String result;

        if (difference == 0) {
            multiplier = getExactMatchMultiplier();
            won = true;
            result = "EXACT MATCH";
        } else if (difference <= getCloseRange()) {
            multiplier = getCloseMatchMultiplier();
            won = true;
            result = "Close Match";
        } else if (difference <= getNearRange()) {
            multiplier = getNearMatchMultiplier();
            won = true;
            result = "Near Match";
        } else {
            result = "No Match";
        }

        double payout = bet * multiplier;
        if (won) {
            if (!payWinnings(player, payout)) {
                sendLocaleMessage(player, "lottery.payout-failed");
                openDraws.remove(player.getUniqueId());
                return;
            }
            handleWin(player, bet, payout);
        } else {
            handleLoss(player, bet);
        }

        gui.showResult(winningNumber, won, multiplier, payout, difference);
        sendMessage(player, plugin.getLocaleManager().formatText(won ? "lottery.win-message" : "lottery.loss-message", Map.of(
            "picked", String.valueOf(pickedNumber),
            "winning", String.valueOf(winningNumber),
            "result", result,
            "payout", plugin.getEconomyManager().format(payout),
            "bet", plugin.getEconomyManager().format(bet)
        )));
        openDraws.remove(player.getUniqueId());
    }

    private double getExactMatchMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.lottery.payouts.exact-match", 32.0);
    }

    private double getCloseMatchMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.lottery.payouts.close-match", 4.5);
    }

    private double getNearMatchMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.lottery.payouts.near-match", 2.2);
    }

    private int getCloseRange() {
        return plugin.getConfigManager().getConfig().getInt("games.lottery.match-ranges.close", 5);
    }

    private int getNearRange() {
        return plugin.getConfigManager().getConfig().getInt("games.lottery.match-ranges.near", 10);
    }
}
