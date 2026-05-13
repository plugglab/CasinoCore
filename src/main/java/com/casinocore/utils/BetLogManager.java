package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BetLogManager {

    private static final DateTimeFormatter ENTRY_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS").withZone(ZoneId.systemDefault());

    private final CasinoPlugin plugin;
    private final File dataFile;
    private final Map<UUID, BetLogSnapshot> latestSnapshots = new ConcurrentHashMap<>();
    private YamlConfiguration data;

    public BetLogManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getPlugin().getDataFolder(), "logs.yml");
        load();
    }

    public synchronized void log(Player player, String game, String section, Map<String, String> details) {
        String timestamp = ENTRY_FORMAT.format(Instant.now());
        String entryKey = timestamp + "-" + System.nanoTime();
        String playerKey = sanitizeKey(player.getName());
        String gameKey = sanitizeKey(game);
        String basePath = "logs." + playerKey + "." + gameKey + "." + entryKey;

        data.set("logs." + playerKey + "._meta.nick", player.getName());
        data.set("logs." + playerKey + "._meta.uuid", player.getUniqueId().toString());
        data.set(basePath + ".details", details);
        data.set(basePath + ".section", section);
        data.set(basePath + ".timestamp", timestamp);

        latestSnapshots.put(player.getUniqueId(), new BetLogSnapshot(player.getName(), game, section, new LinkedHashMap<>(details)));
        save();
    }

    public BetLogSnapshot getLatestSnapshot(UUID playerId) {
        return latestSnapshots.get(playerId);
    }

    public PeriodStats getCurrentWeekStats(UUID playerId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        return collectStats(playerId, dateTime -> sameIsoWeek(dateTime, now));
    }

    public PeriodStats getCurrentMonthStats(UUID playerId) {
        YearMonth currentMonth = YearMonth.now(ZoneId.systemDefault());
        return collectStats(playerId, dateTime -> YearMonth.from(dateTime).equals(currentMonth));
    }

    public PeriodStats getPreviousMonthStats(UUID playerId) {
        YearMonth previousMonth = YearMonth.now(ZoneId.systemDefault()).minusMonths(1);
        return collectStats(playerId, dateTime -> YearMonth.from(dateTime).equals(previousMonth));
    }

    public synchronized void shutdown() {
        save();
    }

    private void load() {
        if (!plugin.getPlugin().getDataFolder().exists()) {
            plugin.getPlugin().getDataFolder().mkdirs();
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getPlugin().getLogger().warning("Failed to save bet logs: " + e.getMessage());
        }
    }

    private PeriodStats collectStats(UUID playerId, java.util.function.Predicate<LocalDateTime> matcher) {
        int wins = 0;
        int losses = 0;
        int gamesPlayed = 0;

        org.bukkit.configuration.ConfigurationSection logsSection = data.getConfigurationSection("logs");
        if (logsSection == null) {
            return new PeriodStats(0, 0, 0);
        }

        for (String playerKey : logsSection.getKeys(false)) {
            String basePath = "logs." + playerKey;
            String uuid = data.getString(basePath + "._meta.uuid");
            if (!playerId.toString().equals(uuid)) {
                continue;
            }

            org.bukkit.configuration.ConfigurationSection playerSection = data.getConfigurationSection(basePath);
            if (playerSection == null) {
                break;
            }

            for (String gameKey : playerSection.getKeys(false)) {
                if ("_meta".equals(gameKey)) {
                    continue;
                }

                org.bukkit.configuration.ConfigurationSection gameSection = playerSection.getConfigurationSection(gameKey);
                if (gameSection == null) {
                    continue;
                }

                for (String entryKey : gameSection.getKeys(false)) {
                    String entryPath = basePath + "." + gameKey + "." + entryKey;
                    String result = data.getString(entryPath + ".details.result");
                    if (result == null) {
                        continue;
                    }

                    String timestamp = data.getString(entryPath + ".timestamp");
                    if (timestamp == null) {
                        continue;
                    }

                    LocalDateTime dateTime;
                    try {
                        dateTime = LocalDateTime.parse(timestamp, ENTRY_FORMAT);
                    } catch (Exception ignored) {
                        continue;
                    }

                    if (!matcher.test(dateTime)) {
                        continue;
                    }

                    gamesPlayed++;
                    if ("win".equalsIgnoreCase(result)) {
                        wins++;
                    } else if ("loss".equalsIgnoreCase(result)) {
                        losses++;
                    }
                }
            }

            break;
        }

        return new PeriodStats(wins, losses, gamesPlayed);
    }

    private boolean sameIsoWeek(LocalDateTime left, LocalDateTime right) {
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        return left.getYear() == right.getYear()
            && left.get(weekFields.weekOfWeekBasedYear()) == right.get(weekFields.weekOfWeekBasedYear());
    }

    private String sanitizeKey(String value) {
        return value.replace('.', '_');
    }

    public record BetLogSnapshot(String nick, String game, String section, Map<String, String> details) {
    }

    public record PeriodStats(int wins, int losses, int gamesPlayed) {
    }
}
