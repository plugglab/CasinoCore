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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CasinoHubGUI implements InventoryHolder {

    private static final int[] BET_SLOTS = {46, 47, 48, 50, 51, 52};
    private static final double[] BET_VALUES = {10, 50, 100, 250, 500, 1000};

    private static final int SLOT_COINFLIP = 20;
    private static final int SLOT_DICE = 21;
    private static final int SLOT_BLACKJACK = 22;
    private static final int SLOT_ROULETTE = 23;
    private static final int SLOT_SLOTS = 24;
    private static final int SLOT_LOTTERY = 31;
    private static final int SLOT_INFO = 49;

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
            for (int i = 0; i < BET_SLOTS.length; i++) {
                if (BET_SLOTS[i] == slot) {
                    SELECTED_BETS.put(player.getUniqueId(), BET_VALUES[i]);
                    render();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                    return;
                }
            }

            switch (slot) {
                case SLOT_COINFLIP -> launchGame("coinflip");
                case SLOT_DICE -> launchGame("dice");
                case SLOT_BLACKJACK -> launchGame("blackjack");
                case SLOT_ROULETTE -> launchGame("roulette");
                case SLOT_SLOTS -> launchGame("slots");
                case SLOT_LOTTERY -> launchGame("lottery");
                default -> {
                }
            }
        }
    }

    public void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        fillAccentRow();
        setGameItem(SLOT_COINFLIP, "coinflip", Material.SUNFLOWER, "Click to create a coinflip with the selected bet");
        setGameItem(SLOT_DICE, "dice", Material.TARGET, "Click to roll with medium risk");
        setGameItem(SLOT_BLACKJACK, "blackjack", Material.PAPER, "Click to open a blackjack table");
        setGameItem(SLOT_ROULETTE, "roulette", Material.CLOCK, "Click to open roulette");
        setGameItem(SLOT_SLOTS, "slots", Material.DIAMOND, "Click to spin the slot machine");
        setGameItem(SLOT_LOTTERY, "lottery", Material.EMERALD, "Click to play lottery");

        inventory.setItem(4, item(Material.NETHER_STAR, "Casino Hub", "Select a bet, then click a game"));
        inventory.setItem(SLOT_INFO, item(
            Material.GOLD_INGOT,
            "Selected Bet",
            plugin.getEconomyManager().format(getSelectedBet()),
            "Balance: " + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player))
        ));

        for (int i = 0; i < BET_SLOTS.length; i++) {
            boolean selected = Double.compare(getSelectedBet(), BET_VALUES[i]) == 0;
            inventory.setItem(BET_SLOTS[i], item(
                selected ? Material.LIME_DYE : Material.YELLOW_DYE,
                (selected ? "> " : "") + plugin.getEconomyManager().format(BET_VALUES[i]),
                selected ? "Selected quick bet" : "Click to select"
            ));
        }
    }

    private void fillAccentRow() {
        ItemStack accent = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 17; slot++) {
            inventory.setItem(slot, accent);
        }
        for (int slot = 36; slot <= 44; slot++) {
            inventory.setItem(slot, accent);
        }
    }

    private void setGameItem(int slot, String gameName, Material material, String clickLine) {
        CasinoGame game = plugin.getGameManager().getCasinoGameDirect(gameName);
        if (game == null) {
            inventory.setItem(slot, item(Material.BARRIER, gameName, "Game unavailable"));
            return;
        }

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
            coinFlipGame.createOffer(player, bet);
        } else if (game instanceof DiceRollGame diceRollGame) {
            diceRollGame.playWithRisk(player, bet, RiskLevel.MEDIUM);
        } else {
            game.play(player, bet);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        player.closeInventory();
    }

    private double getSelectedBet() {
        return SELECTED_BETS.getOrDefault(player.getUniqueId(), plugin.getConfigManager().getMinBet());
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
