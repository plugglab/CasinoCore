package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WheelGame extends BaseCasinoGame {

    private static final List<WheelSegment> DEFAULT_SEGMENTS = List.of(
        new WheelSegment("Bust", 0.0, 34.0),
        new WheelSegment("Safe Return", 1.1, 28.0),
        new WheelSegment("Hot Table", 1.8, 18.0),
        new WheelSegment("Lucky Strike", 3.0, 12.0),
        new WheelSegment("Jackpot Slice", 5.0, 8.0)
    );

    private final Map<UUID, WheelGUI> openWheels;

    public WheelGame(CasinoPlugin plugin) {
        super(plugin, "wheel", "Lucky Wheel", "Spin a fast multi-segment prize wheel with weighted outcomes.");
        this.openWheels = new ConcurrentHashMap<>();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        WheelGUI gui = new WheelGUI(plugin, player, bet);
        openWheels.put(player.getUniqueId(), gui);
        Bukkit.getScheduler().runTask(plugin.getPlugin(), gui::open);
        return true;
    }

    public void startSpin(Player player, WheelGUI gui) {
        if (gui.isSpinning()) {
            return;
        }

        double bet = gui.getBet();
        if (!validateBet(player, bet) || !checkBalance(player, bet)) {
            return;
        }
        if (!withdrawBet(player, bet)) {
            return;
        }
        setCooldown(player);

        List<WheelSegment> segments = getSegments();
        WheelSegment result = rollSegment(segments);
        int resultIndex = segments.indexOf(result);
        gui.setSpinning(true);

        new BukkitRunnable() {
            private int ticks;
            private int index;
            private final int totalSteps = segments.size() * 4 + resultIndex;

            @Override
            public void run() {
                String left = segments.get(Math.floorMod(index - 1, segments.size())).label();
                String center = segments.get(Math.floorMod(index, segments.size())).label();
                String right = segments.get(Math.floorMod(index + 1, segments.size())).label();
                gui.setWindow(left, center, right);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.8f + (ticks * 0.01f));

                if (ticks++ >= totalSteps) {
                    cancel();
                    finishSpin(player, gui, result);
                    return;
                }
                index = (index + 1) % segments.size();
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 3L);
    }

    public void close(Player player) {
        openWheels.remove(player.getUniqueId());
    }

    public CasinoPlugin getPlugin() {
        return plugin;
    }

    private void finishSpin(Player player, WheelGUI gui, WheelSegment result) {
        double bet = gui.getBet();
        gui.setSpinning(false);
        if (result.multiplier() > 0.0) {
            double payout = bet * result.multiplier();
            if (!payWinnings(player, payout)) {
                sendMessage(player, "<red>Wheel payout failed. Contact an administrator.</red>");
                return;
            }

            handleWin(player, bet, payout);
            gui.showResult(true, result.label(), result.multiplier(), payout);
            sendMessage(player,
                "<green><bold>Lucky Wheel Win</bold></green>\n" +
                    "<gray>Slice:</gray> <gold>" + result.label() + "</gold>\n" +
                    "<gray>Multiplier:</gray> <gold>" + result.multiplier() + "x</gold>\n" +
                    "<gray>Payout:</gray> <green>" + plugin.getEconomyManager().format(payout) + "</green>"
            );
        } else {
            handleLoss(player, bet);
            gui.showResult(false, result.label(), 0.0, 0.0);
            sendMessage(player,
                "<red><bold>Lucky Wheel Loss</bold></red>\n" +
                    "<gray>Slice:</gray> <white>" + result.label() + "</white>\n" +
                    "<gray>Lost:</gray> <red>" + plugin.getEconomyManager().format(bet) + "</red>"
            );
        }
    }

    private WheelSegment rollSegment(List<WheelSegment> segments) {
        double total = 0.0;
        for (WheelSegment segment : segments) {
            total += segment.weight();
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0.0;
        for (WheelSegment segment : segments) {
            cumulative += segment.weight();
            if (roll <= cumulative) {
                return segment;
            }
        }
        return segments.get(0);
    }

    private List<WheelSegment> getSegments() {
        ConfigurationSection section = plugin.getConfigManager().getConfig().getConfigurationSection("games.wheel.segments");
        if (section == null) {
            return DEFAULT_SEGMENTS;
        }

        List<WheelSegment> segments = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String label = section.getString(key + ".label", key);
            double multiplier = section.getDouble(key + ".multiplier", 0.0);
            double weight = section.getDouble(key + ".weight", 1.0);
            segments.add(new WheelSegment(label, multiplier, weight));
        }
        return segments.isEmpty() ? DEFAULT_SEGMENTS : segments;
    }

    @Override
    public boolean canPlay(Player player) {
        return !openWheels.containsKey(player.getUniqueId()) && super.canPlay(player);
    }

    private record WheelSegment(String label, double multiplier, double weight) {
    }
}
