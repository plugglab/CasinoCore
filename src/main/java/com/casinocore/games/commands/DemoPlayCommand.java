package com.casinocore.games.commands;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.CasinoGame;
import com.casinocore.games.impl.CoinFlipGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DemoPlayCommand implements CommandExecutor, TabCompleter {

    private final CasinoPlugin plugin;

    public DemoPlayCommand(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        try {
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().sendMessage(sender, "player-only");
                return true;
            }

            if (!plugin.getAntiAbuseManager().tryRecordCommand(player)) {
                player.sendMessage(plugin.getLocaleManager().getText("command.too-fast"));
                return true;
            }

            if (args.length == 0) {
                sendUsage(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                listGames(player);
                return true;
            }

            String gameName = args[0].toLowerCase();
            CasinoGame game = plugin.getGameManager().getCasinoGameDirect(gameName);
            if (game == null || (!"coinflip".equals(gameName) && !"slots".equals(gameName))) {
                player.sendMessage(plugin.getLocaleManager().formatText(
                    "command.game-not-found",
                    plugin.getLocaleManager().placeholders("game", gameName)
                ));
                return true;
            }

            if (game instanceof CoinFlipGame coinFlipGame) {
                return handleCoinFlipCommand(player, args, coinFlipGame);
            }

            if (args.length < 2) {
                sendUsage(player);
                return true;
            }

            double bet;
            try {
                bet = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLocaleManager().getText("command.invalid-bet"));
                return true;
            }

            game.play(player, bet);
            return true;
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Error handling /play in demo", e);
            sender.sendMessage(plugin.getLocaleManager().getText("command.error"));
            return true;
        }
    }

    private boolean handleCoinFlipCommand(Player player, String[] args, CoinFlipGame game) {
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String subcommand = args[1].toLowerCase();
        switch (subcommand) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage("Usage: /play coinflip create <bet>");
                    return true;
                }
                try {
                    return game.createOffer(player, Double.parseDouble(args[2]));
                } catch (NumberFormatException exception) {
                    player.sendMessage(plugin.getLocaleManager().getText("command.invalid-bet"));
                    return true;
                }
            }
            case "join" -> {
                if (args.length < 3) {
                    player.sendMessage("Usage: /play coinflip join <player>");
                    return true;
                }
                return game.joinOffer(player, args[2]);
            }
            case "cancel" -> {
                return game.cancelOffer(player);
            }
            case "list" -> {
                game.showOpenOffers(player);
                return true;
            }
            default -> {
                player.sendMessage("Coinflip usage: /play coinflip <create|join|cancel|list>");
                return true;
            }
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.getLocaleManager().getText("command.usage-header"));
        player.sendMessage("/play list");
        player.sendMessage("/play slots <bet>");
        player.sendMessage("/play coinflip create <bet>");
        player.sendMessage("/play coinflip join <player>");
        player.sendMessage("/play coinflip cancel");
        player.sendMessage("/play coinflip list");
    }

    private void listGames(Player player) {
        try {
            Map<String, CasinoGame> games = plugin.getGameManager().getEnabledCasinoGames();
            if (games.isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().getText("command.no-games"));
                return;
            }

            player.sendMessage(plugin.getLocaleManager().getText("command.games-header"));
            for (CasinoGame game : games.values()) {
                player.sendMessage("- " + game.getDisplayName() + ": " + game.getDescription());
                if (game instanceof CoinFlipGame) {
                    player.sendMessage("  /play coinflip create <bet>");
                    player.sendMessage("  /play coinflip join <player>");
                    player.sendMessage("  /play coinflip list");
                } else {
                    player.sendMessage("  /play " + game.getName() + " <bet>");
                }
            }
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Error listing demo games", e);
            player.sendMessage(plugin.getLocaleManager().getText("command.error"));
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("slots");
            completions.add("coinflip");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("coinflip")) {
            completions.add("create");
            completions.add("join");
            completions.add("cancel");
            completions.add("list");
        } else if (args.length == 2) {
            completions.add("10");
            completions.add("50");
            completions.add("100");
            completions.add("500");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("coinflip") && args[1].equalsIgnoreCase("create")) {
            completions.add("10");
            completions.add("50");
            completions.add("100");
            completions.add("500");
        }

        return completions;
    }
}
