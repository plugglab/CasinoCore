package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe anti-abuse checks for game starts and GUI spam.
 */
public class AntiAbuseManager {

    private static final long SECOND_MS = 1000L;

    private final CasinoPlugin plugin;
    private final Map<UUID, Deque<Long>> commandTimestamps;
    private final Map<UUID, Deque<Long>> gameStartTimestamps;
    private final Map<UUID, Deque<BetRecord>> betWindowRecords;
    private final Map<String, Long> clickTimestamps;

    public AntiAbuseManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.commandTimestamps = new ConcurrentHashMap<>();
        this.gameStartTimestamps = new ConcurrentHashMap<>();
        this.betWindowRecords = new ConcurrentHashMap<>();
        this.clickTimestamps = new ConcurrentHashMap<>();
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("anti-abuse.enabled", true);
    }

    public boolean tryRecordCommand(Player player) {
        if (!isEnabled()) {
            return true;
        }

        int limit = plugin.getConfigManager().getConfig().getInt("anti-abuse.rate-limits.commands-per-10-seconds", 20);
        return checkAndRecord(commandTimestamps.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()),
            10 * SECOND_MS, limit);
    }

    public boolean canStartGame(Player player, String gameName, double bet) {
        if (!isEnabled()) {
            return true;
        }

        long now = System.currentTimeMillis();
        int startsPerMinute = plugin.getConfigManager().getConfig().getInt("anti-abuse.rate-limits.game-starts-per-minute", 30);
        Deque<Long> starts = gameStartTimestamps.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        if (!checkAndRecord(starts, 60 * SECOND_MS, startsPerMinute)) {
            plugin.getMessageManager().send(player, "<red>You're starting games too quickly. Slow down.</red>");
            return false;
        }

        double balance = plugin.getEconomyManager().getBalance(player);
        double maxBetPercent = plugin.getConfigManager().getConfig().getDouble("anti-abuse.max-bet-percent-of-balance", 50.0);
        double allowedSingleBet = balance * (maxBetPercent / 100.0);
        if (balance > 0 && bet > allowedSingleBet) {
            plugin.getMessageManager().send(player, "<red>This bet exceeds your anti-abuse balance limit.</red>");
            synchronized (starts) {
                if (!starts.isEmpty()) {
                    starts.removeLast();
                }
            }
            return false;
        }

        int windowSeconds = plugin.getConfigManager().getConfig().getInt("anti-abuse.bet-window.window-seconds", 60);
        double windowMaxBet = plugin.getConfigManager().getConfig().getDouble("anti-abuse.bet-window.max-total-bet", 10000.0);
        Deque<BetRecord> records = betWindowRecords.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        synchronized (records) {
            pruneBetWindow(records, now, windowSeconds * SECOND_MS);
            double total = records.stream().mapToDouble(BetRecord::amount).sum();
            if (total + bet > windowMaxBet) {
                plugin.getMessageManager().send(player, "<red>You reached the maximum total bet for the current time window.</red>");
                synchronized (starts) {
                    if (!starts.isEmpty()) {
                        starts.removeLast();
                    }
                }
                return false;
            }
            records.addLast(new BetRecord(now, bet, gameName));
        }

        return true;
    }

    public boolean tryRecordClick(Player player, String scopeKey) {
        if (!isEnabled()) {
            return true;
        }

        long now = System.currentTimeMillis();
        int minIntervalMs = plugin.getConfigManager().getConfig().getInt("anti-abuse.click-spam.min-click-interval-ms", 150);
        String key = player.getUniqueId() + ":" + scopeKey;
        Long previous = clickTimestamps.put(key, now);
        return previous == null || now - previous >= minIntervalMs;
    }

    public void clearPlayer(UUID playerId) {
        commandTimestamps.remove(playerId);
        gameStartTimestamps.remove(playerId);
        betWindowRecords.remove(playerId);
        clickTimestamps.keySet().removeIf(key -> key.startsWith(playerId.toString() + ":"));
    }

    public void shutdown() {
        commandTimestamps.clear();
        gameStartTimestamps.clear();
        betWindowRecords.clear();
        clickTimestamps.clear();
    }

    private boolean checkAndRecord(Deque<Long> timestamps, long windowMs, int limit) {
        long now = System.currentTimeMillis();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.removeFirst();
            }

            if (timestamps.size() >= limit) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    private void pruneBetWindow(Deque<BetRecord> records, long now, long windowMs) {
        while (!records.isEmpty() && now - records.peekFirst().timestamp() > windowMs) {
            records.removeFirst();
        }
    }

    private record BetRecord(long timestamp, double amount, String gameName) {
    }
}
