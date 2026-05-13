package com.casinocore.core.commands;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.AdminGamesGUI;
import com.casinocore.gui.CasinoHubGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
public class CasinoCommand implements CommandExecutor, TabCompleter {

    private final CasinoPlugin plugin;

    public CasinoCommand(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
                if (sender instanceof Player player) {
                    if (!plugin.getRegionAccessManager().canUseCasino(player)) {
                        plugin.getRegionAccessManager().sendBlockedMessage(player);
                        return true;
                    }
                    if (!plugin.getAntiAbuseManager().tryRecordCommand(player)) {
                        player.sendMessage(plugin.getLocaleManager().getText("command.too-fast"));
                        return true;
                    }
                    new CasinoHubGUI(plugin, player).open();
            } else {
                sendHelp(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            case "menu", "hub" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().sendMessage(sender, "player-only");
                    return true;
                }
                if (!plugin.getRegionAccessManager().canUseCasino(player)) {
                    plugin.getRegionAccessManager().sendBlockedMessage(player);
                    return true;
                }
                if (!plugin.getAntiAbuseManager().tryRecordCommand(player)) {
                    player.sendMessage(plugin.getLocaleManager().getText("command.too-fast"));
                    return true;
                }
                new CasinoHubGUI(plugin, player).open();
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("casinocore.reload")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }

                ((com.casinocore.core.CasinoCore) plugin).reloadPlugin();
                plugin.getMessageManager().sendMessage(sender, "reload-success");
                return true;
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().sendMessage(sender, "player-only");
                    return true;
                }
                if (!sender.hasPermission("casinocore.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                new AdminGamesGUI(plugin, player).open();
                return true;
            }
            case "balance", "bal" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().sendMessage(sender, "player-only");
                    return true;
                }
                if (!plugin.getAntiAbuseManager().tryRecordCommand(player)) {
                    player.sendMessage(plugin.getLocaleManager().getText("command.too-fast"));
                    return true;
                }

                if (!plugin.getEconomyManager().isAvailable()) {
                    plugin.getMessageManager().sendMessage(sender, "economy-unavailable");
                    return true;
                }

                double balance = plugin.getEconomyManager().getBalance(player);
                plugin.getMessageManager().send(
                    sender,
                    plugin.getUxManager().formatGameMessage(
                        "gold",
                        "Casino Wallet",
                        "<white>Balance:</white> <gold>" + plugin.getEconomyManager().format(balance) + "</gold>",
                        "<white>Wins:</white> <green>" + plugin.getPlayerStatsManager().getWins(player.getUniqueId()) + "</green>",
                        "<white>Streak:</white> <aqua>" + plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId()) + "</aqua>",
                        plugin.getUxManager().formatDailyRewardStatus(player)
                    )
                );
                return true;
            }
            case "daily", "reward", "freespin" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().sendMessage(sender, "player-only");
                    return true;
                }
                claimDailyReward(player);
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageManager().send(
            sender,
            plugin.getUxManager().formatGameMessage(
                "gold",
                "Casino Commands",
                "<white>/casino</white> <gray>- open the casino hub</gray>",
                "<white>/casino balance</white> <gray>- wallet, streak, and reward status</gray>",
                "<white>/casino daily</white> <gray>- claim your free daily spin</gray>",
                sender.hasPermission("casinocore.admin")
                    ? "<white>/casino admin</white> <gray>- enable or disable games</gray>"
                    : "<white>/play list</white> <gray>- browse game shortcuts</gray>",
                sender.hasPermission("casinocore.reload")
                    ? "<white>/casino reload</white> <gray>- reload the plugin</gray>"
                    : "<white>/play list</white> <gray>- browse game shortcuts</gray>"
            )
        );
    }

    private void claimDailyReward(Player player) {
        if (!plugin.getPlayerStatsManager().canClaimDaily(player.getUniqueId())) {
            plugin.getMessageManager().send(
                player,
                plugin.getUxManager().formatGameMessage(
                    "yellow",
                    "Daily Reward",
                    "<red>Already claimed today.</red>",
                    plugin.getUxManager().formatDailyRewardStatus(player)
                )
            );
            return;
        }

        double freeSpinValue = plugin.getConfigManager().getConfig().getDouble("daily-rewards.free-spin-value", 100.0);
        if (!plugin.getEconomyManager().deposit(player, freeSpinValue)) {
            plugin.getMessageManager().send(player, "<red>Could not credit your free spin right now.</red>");
            return;
        }

        plugin.getPlayerStatsManager().recordDailyClaim(player.getUniqueId());
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
        plugin.getUxManager().showBossBarSequence(player, "Daily reward claimed", org.bukkit.boss.BarColor.BLUE, 50L);
        plugin.getMessageManager().send(
            player,
            plugin.getUxManager().formatGameMessage(
                "aqua",
                "Daily Free Spin",
                "<white>Reward:</white> <green>" + plugin.getEconomyManager().format(freeSpinValue) + "</green>",
                "<white>Status:</white> <gray>Spend it on any game you want.</gray>"
            )
        );
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            completions.add("menu");
            completions.add("balance");
            completions.add("daily");

            if (sender.hasPermission("casinocore.admin")) {
                completions.add("admin");
            }

            if (sender.hasPermission("casinocore.reload")) {
                completions.add("reload");
            }
        }

        return completions;
    }
}
