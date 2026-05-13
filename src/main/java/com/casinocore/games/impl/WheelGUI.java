package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class WheelGUI implements InventoryHolder {

    private final CasinoPlugin plugin;
    private final Player player;
    private final double bet;
    private final Inventory inventory;
    private boolean spinning;

    public WheelGUI(CasinoPlugin plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.inventory = Bukkit.createInventory(this, 27, plugin.getMessageManager().parse(t("wheel.gui.title-plain")));
        renderBase();
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void renderBase() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(4, item(Material.NAUTILUS_SHELL, t("wheel.gui.title"), f("wheel.gui.bet", "amount", plugin.getEconomyManager().format(bet))));
        inventory.setItem(13, item(Material.CLOCK, t("wheel.gui.window"), t("wheel.gui.window-lore")));
        inventory.setItem(22, item(Material.LIME_DYE, t("wheel.gui.spin"), t("wheel.gui.spin-lore")));
        inventory.setItem(26, item(Material.BARRIER, t("wheel.gui.back"), spinning ? t("wheel.gui.after-spin") : t("wheel.gui.back-lore")));
    }

    public void setWindow(String left, String center, String right) {
        inventory.setItem(11, item(Material.YELLOW_STAINED_GLASS_PANE, left));
        inventory.setItem(13, item(Material.GOLD_BLOCK, center));
        inventory.setItem(15, item(Material.YELLOW_STAINED_GLASS_PANE, right));
    }

    public void showResult(boolean won, String label, double multiplier, double payout) {
        inventory.setItem(22, item(won ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            won ? t("wheel.gui.result-win") : t("wheel.gui.result-loss"),
            f("wheel.gui.slice", "value", label),
            won ? f("wheel.gui.paid", "amount", plugin.getEconomyManager().format(payout)) : f("wheel.gui.lost", "amount", plugin.getEconomyManager().format(bet)),
            won ? f("wheel.gui.multiplier", "value", String.valueOf(multiplier)) : t("wheel.gui.try-again")
        ));
        inventory.setItem(26, item(Material.BARRIER, t("wheel.gui.back"), t("wheel.gui.back-lore")));
    }

    public void back() {
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    public Player getPlayer() {
        return player;
    }

    public double getBet() {
        return bet;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    private String t(String key) {
        return plugin.getLocaleManager().getText(key);
    }

    private String f(String key, String placeholder, String value) {
        return plugin.getLocaleManager().formatText(key, Map.of(placeholder, value));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(name));
            meta.lore(java.util.Arrays.stream(lore).map(plugin.getMessageManager()::parse).toList());
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
