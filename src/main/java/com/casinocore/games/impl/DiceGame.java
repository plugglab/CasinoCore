package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Dice Game - Roll two dice
 * Win conditions:
 * - Double 6s (snake eyes): 10x bet
 * - Matching pair: 5x bet
 * - Sum of 7 or 11: 2x bet
 * - Any other result: Loss
 */
public class DiceGame extends BaseCasinoGame {

    private final Random random;

    // Win multipliers
    private static final double DOUBLE_SIX_MULTIPLIER = 10.0;
    private static final double MATCHING_PAIR_MULTIPLIER = 5.0;
    private static final double LUCKY_NUMBER_MULTIPLIER = 2.0;

    public DiceGame(CasinoPlugin plugin) {
        super(plugin, "dice", "Dice Roll", "Roll two dice and win big with lucky numbers!");
        this.random = new Random();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        try {
            // Roll two dice
            int die1 = random.nextInt(6) + 1;
            int die2 = random.nextInt(6) + 1;
            int sum = die1 + die2;

            logDebug("Player " + player.getName() + " rolled: " + die1 + " and " + die2 + " (sum: " + sum + ")");

            // Determine outcome
            String result;
            double multiplier = 0;
            boolean won = false;

            if (die1 == 6 && die2 == 6) {
                // Double sixes - jackpot!
                result = "<gold><bold>DOUBLE SIXES - JACKPOT!</bold></gold>";
                multiplier = DOUBLE_SIX_MULTIPLIER;
                won = true;
            } else if (die1 == die2) {
                // Matching pair
                result = "<yellow>Matching Pair!</yellow>";
                multiplier = MATCHING_PAIR_MULTIPLIER;
                won = true;
            } else if (sum == 7 || sum == 11) {
                // Lucky numbers
                result = "<green>Lucky Number!</green>";
                multiplier = LUCKY_NUMBER_MULTIPLIER;
                won = true;
            } else {
                // Loss
                result = "<red>No Win</red>";
                won = false;
            }

            if (won) {
                // Player wins
                double winnings = bet * multiplier;
                boolean paidOut = payWinnings(player, winnings);

                if (paidOut) {
                    double profit = winnings - bet;
                    sendMessage(player,
                        "<green>╔═══════════════════════════╗</green>\n" +
                        "<green>║      <gold>DICE - WINNER!</gold>      ║</green>\n" +
                        "<green>╚═══════════════════════════╝</green>\n" +
                        "<yellow>Dice: </yellow><white>[" + die1 + "] [" + die2 + "]</white> <gray>(Sum: " + sum + ")</gray>\n" +
                        "<yellow>Result: </yellow>" + result + "\n" +
                        "<yellow>Bet: </yellow><white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
                        "<yellow>Multiplier: </yellow><gold>" + multiplier + "x</gold>\n" +
                        "<yellow>Winnings: </yellow><gold>" + plugin.getEconomyManager().format(winnings) + "</gold>\n" +
                        "<yellow>Profit: </yellow><green>+" + plugin.getEconomyManager().format(profit) + "</green>"
                    );

                    logDebug("Player " + player.getName() + " won dice game (multiplier: " + multiplier + "x, winnings: " + winnings + ")");
                    return true;
                } else {
                    sendMessage(player, "<red>Error paying out winnings! Contact an administrator.</red>");
                    return false;
                }
            } else {
                // Player loses
                sendMessage(player,
                    "<red>╔═══════════════════════════╗</red>\n" +
                    "<red>║       <gray>DICE - LOSS</gray>        ║</red>\n" +
                    "<red>╚═══════════════════════════╝</red>\n" +
                    "<yellow>Dice: </yellow><white>[" + die1 + "] [" + die2 + "]</white> <gray>(Sum: " + sum + ")</gray>\n" +
                    "<yellow>Result: </yellow>" + result + "\n" +
                    "<yellow>Bet: </yellow><white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
                    "<red>You lost " + plugin.getEconomyManager().format(bet) + "</red>"
                );

                logDebug("Player " + player.getName() + " lost dice game (rolled: " + die1 + ", " + die2 + ")");
                return true;
            }

        } catch (Exception e) {
            plugin.getPlugin().getLogger().severe(
                "Error executing dice game for " + player.getName() + ": " + e.getMessage()
            );
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        logDebug("Dice game enabled with payouts: Double 6s=" + DOUBLE_SIX_MULTIPLIER + "x, Pairs=" +
                 MATCHING_PAIR_MULTIPLIER + "x, Lucky=" + LUCKY_NUMBER_MULTIPLIER + "x");
    }
}
