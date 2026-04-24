package com.casinocore.games.diceroll;

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

public class DiceRiskGUI implements InventoryHolder {

    private final CasinoPlugin plugin;
    private final DiceRollGame game;
    private final Player player;
    private final double bet;
    private final Inventory inventory;
    private RiskLevel selectedRisk;

    public DiceRiskGUI(CasinoPlugin plugin, DiceRollGame game, Player player, double bet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.bet = bet;
        this.selectedRisk = RiskLevel.MEDIUM;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Dice Risk"));
        render();
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void handleClick(int slot) {
        switch (slot) {
            case 10 -> select(RiskLevel.LOW);
            case 13 -> select(RiskLevel.MEDIUM);
            case 16 -> select(RiskLevel.HIGH);
            case 22 -> {
                player.closeInventory();
                game.playWithRisk(player, bet, selectedRisk);
            }
            case 26 -> {
                player.closeInventory();
                GuiNavigation.openHub(plugin, player);
            }
            default -> {
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    private void select(RiskLevel risk) {
        selectedRisk = risk;
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
    }

    private void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, PlayerHeadFactory.createPlayerHead(
            player.getUniqueId(),
            player.getName(),
            "Dice Table",
            "Bet: " + plugin.getEconomyManager().format(bet),
            "Pick your risk before rolling"
        ));
        inventory.setItem(10, riskItem(RiskLevel.LOW, Material.LIME_WOOL));
        inventory.setItem(13, riskItem(RiskLevel.MEDIUM, Material.YELLOW_WOOL));
        inventory.setItem(16, riskItem(RiskLevel.HIGH, Material.RED_WOOL));
        inventory.setItem(22, item(Material.LIME_DYE, "Start Roll", "Selected: " + selectedRisk.getDisplayName()));
        inventory.setItem(26, item(Material.BARRIER, "Back", "Return to the casino hub"));
    }

    private ItemStack riskItem(RiskLevel riskLevel, Material base) {
        boolean selected = riskLevel == selectedRisk;
        return PlayerHeadFactory.createPlayerHead(
            player.getUniqueId(),
            player.getName(),
            riskLevel.getDisplayName(),
            "Threshold: " + game.getThresholdPreview(riskLevel),
            "Payout: " + game.getMultiplierPreview(riskLevel) + "x",
            selected ? "Selected risk" : "Click to select"
        );
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> loreLines = new ArrayList<>();
            for (String line : lore) {
                loreLines.add(Component.text(line));
            }
            meta.lore(loreLines);
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
