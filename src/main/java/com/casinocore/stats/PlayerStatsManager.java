package com.casinocore.stats;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsManager {

    private final CasinoPlugin plugin;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();
    private final File dataFile;
    private YamlConfiguration data;

    public PlayerStatsManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getPlugin().getDataFolder(), "player-stats.yml");
        load();
    }

    public int getWins(UUID playerId) {
        return getStats(playerId).wins();
    }

    public int getWinStreak(UUID playerId) {
        return getStats(playerId).currentStreak();
    }

    public int getBestWinStreak(UUID playerId) {
        return getStats(playerId).bestStreak();
    }

    public void recordWin(UUID playerId) {
        PlayerStats current = getStats(playerId);
        int streak = current.currentStreak() + 1;
        stats.put(playerId, new PlayerStats(
            current.wins() + 1,
            streak,
            Math.max(current.bestStreak(), streak),
            current.lastDailyClaim()
        ));
    }

    public void recordLoss(UUID playerId) {
        PlayerStats current = getStats(playerId);
        stats.put(playerId, new PlayerStats(
            current.wins(),
            0,
            current.bestStreak(),
            current.lastDailyClaim()
        ));
    }

    public boolean canClaimDaily(UUID playerId) {
        Long lastClaim = getStats(playerId).lastDailyClaim();
        if (lastClaim == null) {
            return true;
        }

        LocalDate lastDate = Instant.ofEpochMilli(lastClaim)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        return !LocalDate.now(ZoneId.systemDefault()).equals(lastDate);
    }

    public void recordDailyClaim(UUID playerId) {
        PlayerStats current = getStats(playerId);
        stats.put(playerId, new PlayerStats(
            current.wins(),
            current.currentStreak(),
            current.bestStreak(),
            System.currentTimeMillis()
        ));
    }

    public Long getLastDailyClaim(UUID playerId) {
        return getStats(playerId).lastDailyClaim();
    }

    public void shutdown() {
        save();
        stats.clear();
    }

    private PlayerStats getStats(UUID playerId) {
        return stats.computeIfAbsent(playerId, this::loadPlayerStats);
    }

    private PlayerStats loadPlayerStats(UUID playerId) {
        String path = "players." + playerId;
        return new PlayerStats(
            data.getInt(path + ".wins", 0),
            data.getInt(path + ".current-streak", 0),
            data.getInt(path + ".best-streak", 0),
            data.contains(path + ".last-daily-claim") ? data.getLong(path + ".last-daily-claim") : null
        );
    }

    private void load() {
        if (!plugin.getPlugin().getDataFolder().exists()) {
            plugin.getPlugin().getDataFolder().mkdirs();
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats value = entry.getValue();
            data.set(path + ".wins", value.wins());
            data.set(path + ".current-streak", value.currentStreak());
            data.set(path + ".best-streak", value.bestStreak());
            data.set(path + ".last-daily-claim", value.lastDailyClaim());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getPlugin().getLogger().warning("Failed to save player stats: " + e.getMessage());
        }
    }

    private record PlayerStats(int wins, int currentStreak, int bestStreak, Long lastDailyClaim) {
    }
}
