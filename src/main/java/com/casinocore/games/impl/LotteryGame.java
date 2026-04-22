package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Lottery Game - Pick a number between 1-100
 * Win chances:
 * - Exact match: 50x bet (1% chance)
 * - Within 5: 10x bet (~10% chance)
 * - Within 10: 3x bet (~20% chance)
 * - Else: Loss
 */
public class LotteryGame extends BaseCasinoGame {

    private final Random random;

    public LotteryGame(CasinoPlugin plugin) {
        super(plugin, "lottery", "Lottery", "Pick a number and win big if you're lucky!");
        this.random = new Random();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        try {
            // Generate winning number (1-100)
            int winningNumber = random.nextInt(100) + 1;

            // Generate player's number (1-100)
            int playerNumber = random.nextInt(100) + 1;

            logDebug("Player " + player.getName() + " lottery: player=" + playerNumber +
                     ", winning=" + winningNumber);

            // Calculate difference
            int difference = Math.abs(winningNumber - playerNumber);

            // Determine outcome
            String result;
            double multiplier = 0;
            boolean won = false;

            double exactMatchMultiplier = getExactMatchMultiplier();
            double closeMatchMultiplier = getCloseMatchMultiplier();
            double nearMatchMultiplier = getNearMatchMultiplier();
            int closeRange = getCloseRange();
            int nearRange = getNearRange();

            if (difference == 0) {
                // Exact match - jackpot!
                result = "<gold><bold>EXACT MATCH - JACKPOT!</bold></gold>";
                multiplier = exactMatchMultiplier;
                won = true;
            } else if (difference <= closeRange) {
                // Close match
                result = "<yellow>Close Match!</yellow>";
                multiplier = closeMatchMultiplier;
                won = true;
            } else if (difference <= nearRange) {
                // Near match
                result = "<green>Near Match!</green>";
                multiplier = nearMatchMultiplier;
                won = true;
            } else {
                // No match
                result = "<red>No Match</red>";
                won = false;
            }

            if (won) {
                // Player wins
                double winnings = bet * multiplier;
                boolean paidOut = payWinnings(player, winnings);

                if (paidOut) {
                    handleWin(player, bet, winnings);
                    double profit = winnings - bet;
                    sendMessage(player,
                        "<green>╔═══════════════════════════╗</green>\n" +
                        "<green>║    <gold>LOTTERY - WINNER!</gold>    ║</green>\n" +
                        "<green>╚═══════════════════════════╝</green>\n" +
                        "<yellow>Your Number: </yellow><white>" + playerNumber + "</white>\n" +
                        "<yellow>Winning Number: </yellow><gold>" + winningNumber + "</gold>\n" +
                        "<yellow>Difference: </yellow><white>" + difference + "</white>\n" +
                        "<yellow>Result: </yellow>" + result + "\n" +
                        "<yellow>Bet: </yellow><white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
                        "<yellow>Multiplier: </yellow><gold>" + multiplier + "x</gold>\n" +
                        "<yellow>Winnings: </yellow><gold>" + plugin.getEconomyManager().format(winnings) + "</gold>\n" +
                        "<yellow>Profit: </yellow><green>+" + plugin.getEconomyManager().format(profit) + "</green>"
                    );

                    logDebug("Player " + player.getName() + " won lottery (multiplier: " +
                             multiplier + "x, diff: " + difference + ")");
                    return true;
                } else {
                    sendMessage(player, "<red>Error paying out winnings! Contact an administrator.</red>");
                    return false;
                }
            } else {
                // Player loses
                handleLoss(player, bet);
                sendMessage(player,
                    "<red>╔═══════════════════════════╗</red>\n" +
                    "<red>║      <gray>LOTTERY - LOSS</gray>      ║</red>\n" +
                    "<red>╚═══════════════════════════╝</red>\n" +
                    "<yellow>Your Number: </yellow><white>" + playerNumber + "</white>\n" +
                    "<yellow>Winning Number: </yellow><gold>" + winningNumber + "</gold>\n" +
                    "<yellow>Difference: </yellow><white>" + difference + "</white>\n" +
                    "<yellow>Result: </yellow>" + result + "\n" +
                    "<yellow>Bet: </yellow><white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
                    "<red>You lost " + plugin.getEconomyManager().format(bet) + "</red>"
                );

                logDebug("Player " + player.getName() + " lost lottery (diff: " + difference + ")");
                return true;
            }

        } catch (Exception e) {
            plugin.getPlugin().getLogger().severe(
                "Error executing lottery game for " + player.getName() + ": " + e.getMessage()
            );
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        logDebug("Lottery game enabled with configured payout table");
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
