package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UxManager {

    private static final DateTimeFormatter DAILY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CasinoPlugin plugin;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public UxManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public void showBossBarSequence(Player player, String title, BarColor color, long durationTicks) {
        clearBossBar(player);
        BossBar bossBar = Bukkit.createBossBar(title, color, BarStyle.SEGMENTED_10);
        bossBar.addPlayer(player);
        activeBossBars.put(player.getUniqueId(), bossBar);

        new BukkitRunnable() {
            private long ticksLeft = durationTicks;

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft <= 0L) {
                    clearBossBar(player);
                    cancel();
                    return;
                }

                bossBar.setProgress(Math.max(0.0, (double) ticksLeft / durationTicks));
                ticksLeft--;
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 1L);
    }

    public void playWinPresentation(Player player, String gameName, double payout, double profit, int streak) {
        Title title = Title.title(
            Component.text(streak >= 3
                ? plugin.getLocaleManager().formatText("ux.win-streak-title", Map.of("streak", String.valueOf(streak)))
                : plugin.getLocaleManager().getText("ux.win-title")),
            Component.text(plugin.getLocaleManager().formatText("ux.win-subtitle", Map.of(
                "game", gameName,
                "profit", plugin.getEconomyManager().format(profit)
            ))),
            Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1300), Duration.ofMillis(350))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), streak >= 3 ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        showBossBarSequence(
            player,
            plugin.getLocaleManager().formatText("ux.win-bossbar", Map.of(
                "streak", String.valueOf(streak),
                "payout", plugin.getEconomyManager().format(payout)
            )),
            streak >= 5 ? BarColor.PURPLE : BarColor.GREEN,
            60L
        );
    }

    public void playLossPresentation(Player player, String gameName) {
        player.showTitle(Title.title(
            Component.text(plugin.getLocaleManager().getText("ux.loss-title")),
            Component.text(plugin.getLocaleManager().formatText("ux.loss-subtitle", Map.of("game", gameName))),
            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(250))
        ));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        showBossBarSequence(player, plugin.getLocaleManager().getText("ux.round-ended"), BarColor.RED, 30L);
    }

    public String formatGameMessage(String accent, String heading, String... bodyLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(accent).append("><bold>").append(heading).append("</bold></").append(accent).append(">");
        for (String line : bodyLines) {
            builder.append("\n<gray>-</gray> ").append(line);
        }
        return builder.toString();
    }

    public String formatDailyRewardStatus(Player player) {
        Long lastClaim = plugin.getPlayerStatsManager().getLastDailyClaim(player.getUniqueId());
        if (lastClaim == null) {
            return plugin.getLocaleManager().getText("ux.daily-ready");
        }

        Instant nextClaim = Instant.ofEpochMilli(lastClaim).plus(Duration.ofDays(1));
        return plugin.getLocaleManager().formatText("ux.daily-next", Map.of(
            "time", DAILY_FORMAT.format(nextClaim.atZone(ZoneId.systemDefault()))
        ));
    }

    public String formatNextDailyClaim(Player player) {
        Long lastClaim = plugin.getPlayerStatsManager().getLastDailyClaim(player.getUniqueId());
        if (lastClaim == null) {
            return plugin.getLocaleManager().getText("ux.ready-now");
        }

        Instant nextClaim = Instant.ofEpochMilli(lastClaim).plus(Duration.ofDays(1));
        return DAILY_FORMAT.format(nextClaim.atZone(ZoneId.systemDefault()));
    }

    public String formatNextDailyClaim(OfflinePlayer player) {
        Long lastClaim = plugin.getPlayerStatsManager().getLastDailyClaim(player.getUniqueId());
        if (lastClaim == null) {
            return plugin.getLocaleManager().getText("ux.ready-now");
        }

        Instant nextClaim = Instant.ofEpochMilli(lastClaim).plus(Duration.ofDays(1));
        return DAILY_FORMAT.format(nextClaim.atZone(ZoneId.systemDefault()));
    }

    public void clearBossBar(Player player) {
        BossBar existing = activeBossBars.remove(player.getUniqueId());
        if (existing != null) {
            existing.removeAll();
        }
    }
}
