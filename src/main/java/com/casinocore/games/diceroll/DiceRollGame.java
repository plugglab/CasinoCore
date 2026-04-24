package com.casinocore.games.diceroll;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;

/**
 * Fast dice game where the player rolls 1-100 and wins by beating a threshold.
 */
public class DiceRollGame extends BaseCasinoGame {

    private static final long RESULT_DELAY_TICKS = 20L;

    private final Random random;

    public DiceRollGame(CasinoPlugin plugin) {
        super(plugin, "dice", "Dice", "Roll 1-100 and beat the risk threshold.");
        this.random = new Random();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return playWithRisk(player, bet, RiskLevel.MEDIUM, false);
    }

    public void openRiskSelection(Player player, double bet) {
        new DiceRiskGUI(plugin, this, player, bet).open();
    }

    public boolean playWithRisk(Player player, double bet, RiskLevel riskLevel) {
        return playWithRisk(player, bet, riskLevel, true);
    }

    private boolean playWithRisk(Player player, double bet, RiskLevel riskLevel, boolean runValidation) {
        try {
            if (runValidation) {
                if (!preGameValidation(player, bet)) {
                    return false;
                }

                if (!withdrawBet(player, bet)) {
                    return false;
                }

                setCooldown(player);
            }

            int threshold = getThreshold(riskLevel);
            double multiplier = getMultiplier(riskLevel);
            int roll = random.nextInt(100) + 1;
            boolean won = roll > threshold;

            showRollingActionBar(player, riskLevel);

            Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> {
                showResultTitle(player, roll, threshold, won);
                showResultActionBar(player, roll, threshold, multiplier, won);

                if (won) {
                    processWin(player, bet, roll, threshold, riskLevel, multiplier);
                } else {
                    processLoss(player, bet, roll, threshold, riskLevel);
                }
            }, RESULT_DELAY_TICKS);

            logGame(player, bet, true);
            logDebug("Player " + player.getName() + " rolled " + roll + " on " + riskLevel.name());
            return true;
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error executing dice roll for " + player.getName(), e);

            if (runValidation) {
                handleGameError(player, bet, e);
                return false;
            }

            throw e;
        }
    }

    private void showRollingActionBar(Player player, RiskLevel riskLevel) {
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (ticks >= RESULT_DELAY_TICKS || !player.isOnline()) {
                    cancel();
                    return;
                }

                int displayRoll = random.nextInt(100) + 1;
                player.sendActionBar(Component.text(
                    "Rolling " + riskLevel.name().toLowerCase(Locale.ROOT) + ": " + displayRoll
                ));

                if (ticks % 4 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 1.0f + (ticks * 0.03f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 1L);
    }

    private void showResultTitle(Player player, int roll, int threshold, boolean won) {
        String titleText = won ? "WIN" : "LOSS";
        String subtitleText = won
            ? "Rolled " + roll + " > " + threshold
            : "Rolled " + roll + " <= " + threshold;

        player.showTitle(Title.title(
            Component.text(titleText),
            Component.text(subtitleText),
            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(300))
        ));

        player.playSound(
            player.getLocation(),
            won ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO,
            1.0f,
            won ? 1.1f : 0.8f
        );
    }

    private void showResultActionBar(Player player, int roll, int threshold, double multiplier, boolean won) {
        String result = won
            ? "Win: " + roll + " > " + threshold + " (" + multiplier + "x)"
            : "Lose: " + roll + " <= " + threshold;
        player.sendActionBar(Component.text(result));
    }

    private void processWin(Player player, double bet, int roll, int threshold, RiskLevel riskLevel, double multiplier) {
        double winnings = bet * multiplier;
        if (!payWinnings(player, winnings)) {
            sendMessage(player, "<red>Failed to pay winnings. Contact an administrator.</red>");
            return;
        }

        handleWin(player, bet, winnings);

        double profit = winnings - bet;
        sendMessage(player,
            "<green><bold>Dice Win</bold></green>\n" +
            "<gray>Risk:</gray> " + riskLevel.getDisplayName() + "\n" +
            "<gray>Roll:</gray> <green>" + roll + "</green> <gray>(need > " + threshold + ")</gray>\n" +
            "<gray>Multiplier:</gray> <gold>" + multiplier + "x</gold>\n" +
            "<gray>Bet:</gray> <white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
            "<gray>Winnings:</gray> <gold>" + plugin.getEconomyManager().format(winnings) + "</gold>\n" +
            "<gray>Profit:</gray> <green>+" + plugin.getEconomyManager().format(profit) + "</green>"
        );
    }

    private void processLoss(Player player, double bet, int roll, int threshold, RiskLevel riskLevel) {
        handleLoss(player, bet);
        sendMessage(player,
            "<red><bold>Dice Loss</bold></red>\n" +
            "<gray>Risk:</gray> " + riskLevel.getDisplayName() + "\n" +
            "<gray>Roll:</gray> <red>" + roll + "</red> <gray>(need > " + threshold + ")</gray>\n" +
            "<gray>Bet:</gray> <white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
            "<red>You lost " + plugin.getEconomyManager().format(bet) + "</red>"
        );
    }

    public void showRiskLevels(Player player) {
        sendMessage(player, "<gold><bold>Dice Risk Levels</bold></gold>");

        for (RiskLevel level : RiskLevel.values()) {
            int threshold = getThreshold(level);
            double multiplier = getMultiplier(level);
            sendMessage(player,
                level.getDisplayName() +
                "<gray>: roll > " + threshold +
                ", win " + multiplier + "x, command /play dice <bet> " + level.getConfigKey() + "</gray>"
            );
        }
    }

    public String getRiskSummary() {
        return "Risk: low, medium, high";
    }

    public int getThresholdPreview(RiskLevel riskLevel) {
        return getThreshold(riskLevel);
    }

    public double getMultiplierPreview(RiskLevel riskLevel) {
        return getMultiplier(riskLevel);
    }

    private int getThreshold(RiskLevel riskLevel) {
        return plugin.getConfigManager().getConfig().getInt(
            "games.dice.risk." + riskLevel.getConfigKey() + ".threshold",
            riskLevel.getDefaultThreshold()
        );
    }

    private double getMultiplier(RiskLevel riskLevel) {
        return plugin.getConfigManager().getConfig().getDouble(
            "games.dice.risk." + riskLevel.getConfigKey() + ".multiplier",
            riskLevel.getDefaultMultiplier()
        );
    }
}
