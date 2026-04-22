package com.casinocore.games.roulette;

import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class RouletteSpinAnimation extends BukkitRunnable {

    private final RouletteGame game;
    private final RouletteGUI gui;
    private final int finalNumber;
    private final Runnable onComplete;

    private int centerIndex;
    private int ticks;
    private final int totalTicks;

    public RouletteSpinAnimation(RouletteGame game, RouletteGUI gui, int finalNumber, Runnable onComplete) {
        this.game = game;
        this.gui = gui;
        this.finalNumber = finalNumber;
        this.onComplete = onComplete;

        int finalIndex = RouletteNumbers.WHEEL_ORDER.indexOf(finalNumber);
        int startIndex = 4;
        int wheelSize = RouletteNumbers.WHEEL_ORDER.size();
        int extraLoops = 3 * wheelSize;
        int delta = (finalIndex - startIndex + wheelSize) % wheelSize;

        this.centerIndex = startIndex;
        this.totalTicks = extraLoops + delta;
    }

    public void start() {
        runTaskTimer(game.getPlugin(), 0L, 2L);
    }

    @Override
    public void run() {
        updateWindow();
        gui.getPlayer().playSound(gui.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 0.9f + (ticks * 0.01f));

        if (ticks >= totalTicks) {
            cancel();
            updateWindow();
            gui.getPlayer().playSound(gui.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
            onComplete.run();
            return;
        }

        centerIndex = (centerIndex + 1) % RouletteNumbers.WHEEL_ORDER.size();
        ticks++;
    }

    private void updateWindow() {
        List<Integer> window = new ArrayList<>();
        int wheelSize = RouletteNumbers.WHEEL_ORDER.size();
        for (int offset = -4; offset <= 4; offset++) {
            int index = (centerIndex + offset + wheelSize) % wheelSize;
            window.add(RouletteNumbers.WHEEL_ORDER.get(index));
        }
        gui.updateWheelWindow(window);
    }
}
