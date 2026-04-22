package com.casinocore.games.slots;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SpinAnimation extends BukkitRunnable {

    private static final int UPDATE_INTERVAL = 2;

    private final SlotMachineGUI gui;
    private final SlotSymbol[] finalResults;
    private final Runnable onComplete;
    private final Random random;
    private final double bet;

    private final int[] reelStopTicks = {40, 60, 80};
    private final boolean[] reelStopped = {false, false, false};
    private int tickCount;

    public SpinAnimation(SlotMachineGUI gui, SlotSymbol[] finalResults, double bet, Runnable onComplete) {
        this.gui = gui;
        this.finalResults = finalResults;
        this.onComplete = onComplete;
        this.random = new Random();
        this.bet = bet;
    }

    public void start() {
        runTaskTimer(gui.getPlugin().getPlugin(), 0L, UPDATE_INTERVAL);
    }

    @Override
    public void run() {
        tickCount++;
        gui.playSound(Sound.BLOCK_NOTE_BLOCK_HAT, Math.min(1.8f, 0.7f + (tickCount * 0.01f)));

        for (int i = 0; i < 3; i++) {
            if (reelStopped[i]) {
                continue;
            }

            if (tickCount >= reelStopTicks[i]) {
                reelStopped[i] = true;
                gui.updateReel(i, finalResults[i]);
                gui.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f + (i * 0.15f));
            } else {
                gui.updateReel(i, SlotSymbol.getRandomSymbol(random));
            }
        }

        if (reelStopped[0] && reelStopped[1] && reelStopped[2] && tickCount >= reelStopTicks[2] + 10) {
            cancel();
            double multiplier = SlotSymbol.calculateMultiplier(finalResults);
            double winnings = bet * multiplier;
            Bukkit.getScheduler().runTask(gui.getPlugin().getPlugin(), () -> {
                gui.showResults(finalResults, multiplier, winnings);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }
    }
}
