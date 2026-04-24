package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.GuiNavigation;
import com.casinocore.utils.PlayerHeadFactory;
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

public class CoinFlipGUI implements InventoryHolder {

    private static final int SLOT_CREATE = 45;
    private static final int SLOT_MINUS_100 = 46;
    private static final int SLOT_MINUS_10 = 47;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_PLUS_10 = 51;
    private static final int SLOT_PLUS_100 = 52;
    private static final int SLOT_BACK = 53;
    private static final int[] OFFER_SLOTS = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32, 33, 34};

    private final CasinoPlugin plugin;
    private final CoinFlipGame game;
    private final Player player;
    private final Inventory inventory;
    private double selectedBet;
    private List<CoinFlipGame.CoinFlipOfferView> visibleOffers;

    public CoinFlipGUI(CasinoPlugin plugin, CoinFlipGame game, Player player, double startingBet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.selectedBet = startingBet;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Coin Flip"));
        this.visibleOffers = List.of();
        render();
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void handleClick(int slot) {
        switch (slot) {
            case SLOT_CREATE -> {
                player.closeInventory();
                game.createOffer(player, selectedBet);
            }
            case SLOT_MINUS_100 -> adjustBet(-100.0);
            case SLOT_MINUS_10 -> adjustBet(-10.0);
            case SLOT_PLUS_10 -> adjustBet(10.0);
            case SLOT_PLUS_100 -> adjustBet(100.0);
            case SLOT_BACK -> {
                player.closeInventory();
                GuiNavigation.openHub(plugin, player);
            }
            default -> {
                int index = offerIndex(slot);
                if (index >= 0 && index < visibleOffers.size()) {
                    player.closeInventory();
                    game.joinOffer(player, visibleOffers.get(index).creatorName());
                }
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        visibleOffers = game.getOpenOfferViews();
        inventory.setItem(4, item(Material.SUNFLOWER, "Coin Flip", "Create or join a public wager", "Open offers: " + visibleOffers.size()));
        inventory.setItem(SLOT_CREATE, item(Material.LIME_DYE, "Create Offer", "Bet: " + plugin.getEconomyManager().format(selectedBet)));
        inventory.setItem(SLOT_MINUS_100, item(Material.REDSTONE, "-100", "Lower selected bet"));
        inventory.setItem(SLOT_MINUS_10, item(Material.RED_DYE, "-10", "Lower selected bet"));
        inventory.setItem(SLOT_INFO, item(Material.GOLD_INGOT, "Selected Bet", plugin.getEconomyManager().format(selectedBet), "Balance: " + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player))));
        inventory.setItem(SLOT_PLUS_10, item(Material.LIME_DYE, "+10", "Raise selected bet"));
        inventory.setItem(SLOT_PLUS_100, item(Material.EMERALD, "+100", "Raise selected bet"));
        inventory.setItem(SLOT_BACK, item(Material.BARRIER, "Back", "Return to the casino hub"));

        for (int i = 0; i < OFFER_SLOTS.length; i++) {
            if (i >= visibleOffers.size()) {
                inventory.setItem(OFFER_SLOTS[i], item(Material.BLACK_STAINED_GLASS_PANE, "No Offer", "Waiting for players"));
                continue;
            }

            CoinFlipGame.CoinFlipOfferView offer = visibleOffers.get(i);
            inventory.setItem(OFFER_SLOTS[i], PlayerHeadFactory.createPlayerHead(
                offer.creatorId(),
                offer.creatorName(),
                offer.creatorName(),
                "Bet: " + plugin.getEconomyManager().format(offer.bet()),
                "Click to join"
            ));
        }
    }

    private void adjustBet(double delta) {
        double min = plugin.getConfigManager().getMinBet("coinflip");
        double max = plugin.getConfigManager().getMaxBet("coinflip");
        selectedBet = Math.max(min, Math.min(max, Math.round((selectedBet + delta) * 100.0) / 100.0));
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
    }

    private int offerIndex(int slot) {
        for (int i = 0; i < OFFER_SLOTS.length; i++) {
            if (OFFER_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
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
