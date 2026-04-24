package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.CasinoGame;
import com.casinocore.games.diceroll.DiceRollGame;
import com.casinocore.games.diceroll.RiskLevel;
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
    private static final int SLOT_CUSTOM_BET = 45;
    private static final int SLOT_BET_MINUS_100 = 46;
    private static final int SLOT_BET_MINUS_10 = 47;
    private static final int SLOT_BET_MIN = 48;
    private static final int SLOT_BET_INFO = 49;
    private static final int SLOT_BET_MAX = 50;
    private static final int SLOT_BET_PLUS_10 = 51;
    private static final int SLOT_BET_PLUS_100 = 52;
    private static final int SLOT_REFRESH = 53;
    private static final int[] GAME_SLOTS = {10, 11, 12, 14, 15, 16, 28, 34};
    private static final Map<String, Material> GAME_MATERIALS = Map.of(
        "coinflip", Material.SUNFLOWER,
        "dice", Material.TARGET,
        "blackjack", Material.PAPER,
        "roulette", Material.CLOCK,
        "slots", Material.DIAMOND,
        "lottery", Material.EMERALD,
        "horserace", Material.SADDLE,
        "wheel", Material.NAUTILUS_SHELL
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
            "Casino Hub",
            "Balance: " + plugin.getEconomyManager().format(balance),
            "Wins: " + plugin.getPlayerStatsManager().getWins(player.getUniqueId()),
            "Losses: " + plugin.getPlayerStatsManager().getLosses(player.getUniqueId()),
            "Daily: " + (plugin.getPlayerStatsManager().canClaimDaily(player.getUniqueId()) ? "Ready" : "Claimed")
        ));
        inventory.setItem(SLOT_STATUS, item(
            Material.GOLD_BLOCK,
            "Selected Bet",
            plugin.getEconomyManager().format(getSelectedBet()),
            "Games Played: " + plugin.getPlayerStatsManager().getGamesPlayed(player.getUniqueId()),
            "Streak: " + plugin.getPlayerStatsManager().getWinStreak(player.getUniqueId()),
            "Best Streak: " + plugin.getPlayerStatsManager().getBestWinStreak(player.getUniqueId())
        ));
    }

    private void renderGameGrid() {
        List<CasinoGame> games = new ArrayList<>(plugin.getGameManager().getEnabledCasinoGames().values());
        games.sort(Comparator.comparing(CasinoGame::getName));
        for (int i = 0; i < GAME_SLOTS.length; i++) {
            if (i >= games.size()) {
                inventory.setItem(GAME_SLOTS[i], item(Material.BARRIER, "Coming Soon", "Reserved for future casino games"));
                continue;
            }

            CasinoGame game = games.get(i);
            Material material = GAME_MATERIALS.getOrDefault(game.getName(), Material.GOLD_INGOT);
            setGameItem(GAME_SLOTS[i], game, material, getClickHint(game.getName()));
        }
    }

    private void renderBetControls() {
        inventory.setItem(SLOT_CUSTOM_BET, item(Material.WRITABLE_BOOK, "Custom Bet", "Enter any value in chat"));
        inventory.setItem(SLOT_BET_MINUS_100, item(Material.REDSTONE, "-100", "Lower the selected bet"));
        inventory.setItem(SLOT_BET_MINUS_10, item(Material.RED_DYE, "-10", "Fine-tune your bet"));
        inventory.setItem(SLOT_BET_MIN, item(Material.BARRIER, "Set Min", plugin.getEconomyManager().format(plugin.getConfigManager().getMinBet())));
        inventory.setItem(SLOT_BET_INFO, item(
            Material.GOLD_INGOT,
            "Current Bet",
            plugin.getEconomyManager().format(getSelectedBet()),
            "Use quick controls or custom chat input"
        ));
        inventory.setItem(SLOT_BET_MAX, item(Material.EMERALD_BLOCK, "Set Max", plugin.getEconomyManager().format(plugin.getConfigManager().getMaxBet())));
        inventory.setItem(SLOT_BET_PLUS_10, item(Material.LIME_DYE, "+10", "Fine-tune your bet"));
        inventory.setItem(SLOT_BET_PLUS_100, item(Material.EMERALD, "+100", "Raise the selected bet"));
        inventory.setItem(SLOT_REFRESH, item(Material.COMPASS, "Refresh", "Rebuild the menu and sync your balance"));
    }

    private void setGameItem(int slot, CasinoGame game, Material material, String clickLine) {
        List<String> lore = new ArrayList<>();
        lore.add(game.getDescription());
        lore.add("Min Bet: " + plugin.getEconomyManager().format(game.getMinBet()));
        lore.add("Max Bet: " + plugin.getEconomyManager().format(game.getMaxBet()));
        lore.add(clickLine);

        if (game instanceof DiceRollGame) {
            lore.add("Default from hub: medium risk");
        } else if (game instanceof CoinFlipGame) {
            lore.add("Creates a public wager offer");
        }

        inventory.setItem(slot, item(material, game.getDisplayName(), lore.toArray(new String[0])));
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
            case "dice" -> "Click to roll medium risk instantly";
            case "blackjack" -> "Click to open a blackjack table";
            case "roulette" -> "Click to open roulette";
            case "slots" -> "Click to spin the slot machine";
            case "lottery" -> "Click to draw lottery numbers";
            case "horserace" -> "Click to open the race board";
            case "wheel" -> "Click to spin the lucky wheel";
            default -> "Click to play";
        };
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Component.text(line));
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
