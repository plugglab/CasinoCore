package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.CasinoGame;
import com.casinocore.games.diceroll.DiceRollGame;
import com.casinocore.games.impl.CoinFlipGame;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CasinoHubGUI implements InventoryHolder {

    private static final int SLOT_WALLET = 4;
    private static final int SLOT_STATUS = 13;
    private static final int SLOT_HELP = 40;
    private static final int SLOT_FOOTER = 41;
    private static final int SLOT_CUSTOM_BET = 45;
    private static final int SLOT_BET_MINUS_100 = 46;
    private static final int SLOT_BET_MINUS_10 = 47;
    private static final int SLOT_BET_MIN = 48;
    private static final int SLOT_BET_INFO = 49;
    private static final int SLOT_BET_MAX = 50;
    private static final int SLOT_BET_PLUS_10 = 51;
    private static final int SLOT_BET_PLUS_100 = 52;
    private static final int SLOT_REFRESH = 53;
    private static final int[] GAME_SLOTS = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32, 33, 34};
    private static final Map<String, Material> GAME_MATERIALS = Map.ofEntries(
        Map.entry("coinflip", Material.SUNFLOWER),
        Map.entry("dice", Material.TARGET),
        Map.entry("blackjack", Material.PAPER),
        Map.entry("highlow", Material.REDSTONE_TORCH),
        Map.entry("doubleup", Material.BLAZE_POWDER),
        Map.entry("treasure", Material.CHEST),
        Map.entry("roulette", Material.CLOCK),
        Map.entry("slots", Material.DIAMOND),
        Map.entry("lottery", Material.EMERALD),
        Map.entry("horserace", Material.SADDLE),
        Map.entry("wheel", Material.NAUTILUS_SHELL)
    );

    private static final Map<UUID, Double> SELECTED_BETS = new ConcurrentHashMap<>();

    private final CasinoPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public CasinoHubGUI(CasinoPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Casino Hub"));
        SELECTED_BETS.putIfAbsent(player.getUniqueId(), plugin.getConfigManager().getMinBet());
        render();
    }

    public static void setSelectedBet(UUID playerId, double bet) {
        SELECTED_BETS.put(playerId, bet);
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void handleClick(int slot) {
        if (!plugin.getAntiAbuseManager().tryRecordClick(player, "casino-hub")) {
            return;
        }

        if (slot >= 0 && slot < inventory.getSize()) {
            switch (slot) {
                case SLOT_CUSTOM_BET -> CustomBetManager.prompt(plugin, player);
                case SLOT_BET_MINUS_100 -> adjustBet(-100.0);
                case SLOT_BET_MINUS_10 -> adjustBet(-10.0);
                case SLOT_BET_MIN -> setBet(plugin.getConfigManager().getMinBet());
                case SLOT_BET_MAX -> setBet(plugin.getConfigManager().getMaxBet());
                case SLOT_BET_PLUS_10 -> adjustBet(10.0);
                case SLOT_BET_PLUS_100 -> adjustBet(100.0);
                case SLOT_REFRESH -> render();
                default -> {
                    String gameName = getGameForSlot(slot);
                    if (gameName != null) {
                        launchGame(gameName);
                    }
                }
            }
        }
    }

    public void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        fillAccentRows();
        renderSummary();
        renderGameGrid();
        renderBetControls();
    }

    private void fillAccentRows() {
        ItemStack accent = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 17; slot++) {
            inventory.setItem(slot, accent);
        }
        for (int slot = 36; slot <= 44; slot++) {
            inventory.setItem(slot, accent);
        }
    }

    private void renderSummary() {
        double balance = plugin.getEconomyManager().getBalance(player);
        inventory.setItem(SLOT_WALLET, item(
            Material.NETHER_STAR,
            "<gold><bold>Casino Hub</bold></gold>",
            "<gray>Balance:</gray> <green>" + plugin.getEconomyManager().format(balance) + "</green>",
            "<gray>Wins:</gray> <white>" + plugin.getPlayerStatsManager().getWins(player.getUniqueId()) + "</white>",
            "<gray>Losses:</gray> <white>" + plugin.getPlayerStatsManager().getLosses(player.getUniqueId()) + "</white>",
            "<gray>Daily:</gray> <yellow>" + (plugin.getPlayerStatsManager().canClaimDaily(player.getUniqueId()) ? "Ready" : "Claimed") + "</yellow>"
        ));
        inventory.setItem(SLOT_STATUS, item(
            Material.GOLD_BLOCK,
            "<gold><bold>Selected Bet</bold></gold>",
            "<yellow>" + plugin.getEconomyManager().format(getSelectedBet()) + "</yellow>",
            "<gray>Games Played:</gray> <white>" + plugin.getPlayerStatsManager().getGamesPlayed(player.getUniqueId()) + "</white>",
            "<gray>Streak:</gray> <white>" + plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId()) + "</white>",
            "<gray>Best Streak:</gray> <white>" + plugin.getPlayerStatsManager().getBestWinStreak(player.getUniqueId()) + "</white>"
        ));
    }

    private void renderGameGrid() {
        List<CasinoGame> games = new ArrayList<>(plugin.getGameManager().getEnabledCasinoGames().values());
        games.sort(Comparator.comparing(CasinoGame::getName));
        for (int i = 0; i < GAME_SLOTS.length; i++) {
            if (i >= games.size()) {
                inventory.setItem(GAME_SLOTS[i], item(Material.BARRIER, "<gray>Coming Soon</gray>", "<dark_gray>Reserved for future casino games</dark_gray>"));
                continue;
            }

            CasinoGame game = games.get(i);
            Material material = GAME_MATERIALS.getOrDefault(game.getName(), Material.GOLD_INGOT);
            setGameItem(GAME_SLOTS[i], game, material, getClickHint(game.getName()));
        }

        inventory.setItem(SLOT_HELP, item(
            Material.BOOK,
            "<aqua><bold>How To Play</bold></aqua>",
            "<gray>1.</gray> <white>Set your bet in the bottom row.</white>",
            "<gray>2.</gray> <white>Hover a game to read the rules.</white>",
            "<gray>3.</gray> <white>Click to open that game's GUI.</white>"
        ));
        inventory.setItem(SLOT_FOOTER, item(
            Material.AMETHYST_SHARD,
            "<light_purple>Hub Tips</light_purple>",
            "<gray>Games are color-coded by style and risk.</gray>",
            "<gray>Use Custom Bet if the quick buttons are too coarse.</gray>"
        ));
    }

    private void renderBetControls() {
        inventory.setItem(SLOT_CUSTOM_BET, item(Material.WRITABLE_BOOK, "<aqua>Custom Bet</aqua>", "<gray>Enter any value in chat</gray>"));
        inventory.setItem(SLOT_BET_MINUS_100, item(Material.REDSTONE, "<red>-100</red>", "<gray>Lower the selected bet</gray>"));
        inventory.setItem(SLOT_BET_MINUS_10, item(Material.RED_DYE, "<red>-10</red>", "<gray>Fine-tune your bet</gray>"));
        inventory.setItem(SLOT_BET_MIN, item(Material.BARRIER, "<gray>Set Min</gray>", "<white>" + plugin.getEconomyManager().format(plugin.getConfigManager().getMinBet()) + "</white>"));
        inventory.setItem(SLOT_BET_INFO, item(
            Material.GOLD_INGOT,
            "<gold><bold>Current Bet</bold></gold>",
            "<yellow>" + plugin.getEconomyManager().format(getSelectedBet()) + "</yellow>",
            "<gray>Use quick controls or custom chat input</gray>"
        ));
        inventory.setItem(SLOT_BET_MAX, item(Material.EMERALD_BLOCK, "<green>Set Max</green>", "<white>" + plugin.getEconomyManager().format(plugin.getConfigManager().getMaxBet()) + "</white>"));
        inventory.setItem(SLOT_BET_PLUS_10, item(Material.LIME_DYE, "<green>+10</green>", "<gray>Fine-tune your bet</gray>"));
        inventory.setItem(SLOT_BET_PLUS_100, item(Material.EMERALD, "<green>+100</green>", "<gray>Raise the selected bet</gray>"));
        inventory.setItem(SLOT_REFRESH, item(Material.COMPASS, "<aqua>Refresh</aqua>", "<gray>Rebuild the menu and sync your balance</gray>"));
    }

    private void setGameItem(int slot, CasinoGame game, Material material, String clickLine) {
        List<String> lore = new ArrayList<>();
        lore.add(getThemeLine(game.getName()));
        lore.add("<gray>" + game.getDescription() + "</gray>");
        lore.add(getHowToPlay(game.getName()));
        lore.add("<gray>Min Bet:</gray> <green>" + plugin.getEconomyManager().format(game.getMinBet()) + "</green>");
        lore.add("<gray>Max Bet:</gray> <red>" + plugin.getEconomyManager().format(game.getMaxBet()) + "</red>");
        lore.add("<yellow>" + clickLine + "</yellow>");

        if (game instanceof DiceRollGame) {
            lore.add("<aqua>Default from hub: medium risk</aqua>");
        } else if (game instanceof CoinFlipGame) {
            lore.add("<gold>Creates a public wager offer</gold>");
        }

        inventory.setItem(slot, item(material, getGameTitle(game.getName(), game.getDisplayName()), lore.toArray(new String[0])));
    }

    private void launchGame(String gameName) {
        CasinoGame game = plugin.getGameManager().getCasinoGameDirect(gameName);
        if (game == null) {
            player.sendMessage("Game unavailable.");
            return;
        }

        double bet = getSelectedBet();
        if (game instanceof CoinFlipGame coinFlipGame) {
            coinFlipGame.play(player, bet);
        } else if (game instanceof DiceRollGame diceRollGame) {
            diceRollGame.openRiskSelection(player, bet);
        } else {
            game.play(player, bet);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        player.closeInventory();
    }

    private void adjustBet(double delta) {
        setBet(getSelectedBet() + delta);
    }

    private void setBet(double value) {
        double min = plugin.getConfigManager().getMinBet();
        double max = plugin.getConfigManager().getMaxBet();
        double clamped = Math.max(min, Math.min(max, value));
        SELECTED_BETS.put(player.getUniqueId(), Math.round(clamped * 100.0) / 100.0);
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private double getSelectedBet() {
        return SELECTED_BETS.getOrDefault(player.getUniqueId(), plugin.getConfigManager().getMinBet());
    }

    private String getGameForSlot(int slot) {
        List<CasinoGame> games = new ArrayList<>(plugin.getGameManager().getEnabledCasinoGames().values());
        games.sort(Comparator.comparing(CasinoGame::getName));
        for (int i = 0; i < GAME_SLOTS.length && i < games.size(); i++) {
            if (GAME_SLOTS[i] == slot) {
                return games.get(i).getName();
            }
        }
        return null;
    }

    private String getClickHint(String gameName) {
        return switch (gameName) {
            case "coinflip" -> "Click to create a coinflip with the selected bet";
            case "dice" -> "Click to open risk selection";
            case "blackjack" -> "Click to open a blackjack table";
            case "highlow" -> "Click to guess the next card";
            case "doubleup" -> "Click to risk your pot for a bigger payout";
            case "treasure" -> "Click to pick from hidden treasure chests";
            case "roulette" -> "Click to open roulette";
            case "slots" -> "Click to spin the slot machine";
            case "lottery" -> "Click to draw lottery numbers";
            case "horserace" -> "Click to open the race board";
            case "wheel" -> "Click to spin the lucky wheel";
            default -> "Click to play";
        };
    }

    private String getGameTitle(String gameName, String fallback) {
        return switch (gameName) {
            case "coinflip" -> "<yellow><bold>Coin Flip</bold></yellow>";
            case "dice" -> "<gold><bold>Dice</bold></gold>";
            case "blackjack" -> "<dark_green><bold>Blackjack</bold></dark_green>";
            case "highlow" -> "<red><bold>High Low</bold></red>";
            case "doubleup" -> "<gold><bold>Double Up</bold></gold>";
            case "treasure" -> "<aqua><bold>Treasure Pick</bold></aqua>";
            case "roulette" -> "<red><bold>Roulette</bold></red>";
            case "slots" -> "<light_purple><bold>Slots</bold></light_purple>";
            case "lottery" -> "<green><bold>Lottery</bold></green>";
            case "horserace" -> "<yellow><bold>Horse Race</bold></yellow>";
            case "wheel" -> "<aqua><bold>Lucky Wheel</bold></aqua>";
            default -> "<gold><bold>" + fallback + "</bold></gold>";
        };
    }

    private String getThemeLine(String gameName) {
        return switch (gameName) {
            case "coinflip" -> "<yellow>Theme:</yellow> <white>Player vs player coin toss</white>";
            case "dice" -> "<gold>Theme:</gold> <white>Risk tiers and thresholds</white>";
            case "blackjack" -> "<green>Theme:</green> <white>Beat the dealer without busting</white>";
            case "highlow" -> "<red>Theme:</red> <white>Predict the next card</white>";
            case "doubleup" -> "<gold>Theme:</gold> <white>Press your luck for bigger pots</white>";
            case "treasure" -> "<aqua>Theme:</aqua> <white>One chest hides the win</white>";
            case "roulette" -> "<red>Theme:</red> <white>Table bets and wheel results</white>";
            case "slots" -> "<light_purple>Theme:</light_purple> <white>Line up matching symbols</white>";
            case "lottery" -> "<green>Theme:</green> <white>Chase the winning number</white>";
            case "horserace" -> "<yellow>Theme:</yellow> <white>Shared betting pool and live race</white>";
            case "wheel" -> "<aqua>Theme:</aqua> <white>Weighted prize slices</white>";
            default -> "<gray>Theme:</gray> <white>Casino table</white>";
        };
    }

    private String getHowToPlay(String gameName) {
        return switch (gameName) {
            case "coinflip" -> "<gray>How:</gray> <white>Create an offer, then wait for another player to join.</white>";
            case "dice" -> "<gray>How:</gray> <white>Pick a risk tier. Higher risk means lower odds and higher payout.</white>";
            case "blackjack" -> "<gray>How:</gray> <white>Hit, stand, and split to beat the dealer's hand.</white>";
            case "highlow" -> "<gray>How:</gray> <white>Guess whether the next card will be higher or lower.</white>";
            case "doubleup" -> "<gray>How:</gray> <white>Cash out safely or double again on a 50/50 chance.</white>";
            case "treasure" -> "<gray>How:</gray> <white>Choose one chest. Only one contains the treasure.</white>";
            case "roulette" -> "<gray>How:</gray> <white>Pick number, color, or parity and watch the wheel.</white>";
            case "slots" -> "<gray>How:</gray> <white>Pull the lever and line up matching symbols across the reels.</white>";
            case "lottery" -> "<gray>How:</gray> <white>Choose your number and try to land on the winning roll.</white>";
            case "horserace" -> "<gray>How:</gray> <white>Back one horse before the shared countdown ends.</white>";
            case "wheel" -> "<gray>How:</gray> <white>Spin the weighted wheel and hope it lands on a hot slice.</white>";
            default -> "<gray>How:</gray> <white>Click to play.</white>";
        };
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(name));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(plugin.getMessageManager().parse(line));
            }
            meta.lore(lines);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
