package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.GuiNavigation;
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
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Lucky Wheel"));
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
        inventory.setItem(4, item(Material.NAUTILUS_SHELL, "Lucky Wheel", "Bet: " + plugin.getEconomyManager().format(bet)));
        inventory.setItem(13, item(Material.CLOCK, "Wheel Window", "Watch the slices move"));
        inventory.setItem(22, item(Material.LIME_DYE, "Spin", "Start the lucky wheel"));
        inventory.setItem(26, item(Material.BARRIER, "Back", spinning ? "Available after the spin" : "Return to the casino hub"));
    }

    public void setWindow(String left, String center, String right) {
        inventory.setItem(11, item(Material.YELLOW_STAINED_GLASS_PANE, left));
        inventory.setItem(13, item(Material.GOLD_BLOCK, center));
        inventory.setItem(15, item(Material.YELLOW_STAINED_GLASS_PANE, right));
    }

    public void showResult(boolean won, String label, double multiplier, double payout) {
        inventory.setItem(22, item(won ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            won ? "Winner" : "Miss",
            "Slice: " + label,
            won ? "Paid: " + plugin.getEconomyManager().format(payout) : "Lost: " + plugin.getEconomyManager().format(bet),
            won ? "Multiplier: " + multiplier + "x" : "Try again"
        ));
        inventory.setItem(26, item(Material.BARRIER, "Back", "Return to the casino hub"));
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
