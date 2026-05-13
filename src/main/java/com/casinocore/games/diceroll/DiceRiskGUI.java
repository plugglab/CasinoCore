package com.casinocore.games.diceroll;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.gui.GuiNavigation;
import com.casinocore.utils.PlayerHeadFactory;
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
        this.inventory = Bukkit.createInventory(this, 27, plugin.getMessageManager().parse(t("dice.gui.title-plain")));
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
            plugin,
            player.getUniqueId(),
            player.getName(),
            t("dice.gui.header-title"),
            f("dice.gui.bet", "amount", plugin.getEconomyManager().format(bet)),
            t("dice.gui.header-lore")
        ));
        inventory.setItem(10, riskItem(RiskLevel.LOW, Material.LIME_WOOL));
        inventory.setItem(13, riskItem(RiskLevel.MEDIUM, Material.YELLOW_WOOL));
        inventory.setItem(16, riskItem(RiskLevel.HIGH, Material.RED_WOOL));
        inventory.setItem(22, item(Material.LIME_DYE, t("dice.gui.start"), f("dice.gui.selected", "risk", selectedRisk.getDisplayName())));
        inventory.setItem(26, item(Material.BARRIER, t("dice.gui.back"), t("dice.gui.back-lore")));
    }

    private ItemStack riskItem(RiskLevel riskLevel, Material base) {
        boolean selected = riskLevel == selectedRisk;
        return item(
            selected ? Material.EMERALD_BLOCK : base,
            riskLevel.getDisplayName(),
            f("dice.gui.threshold", "value", String.valueOf(game.getThresholdPreview(riskLevel))),
            f("dice.gui.payout", "value", String.valueOf(game.getMultiplierPreview(riskLevel))),
            selected ? t("dice.gui.selected-risk") : t("dice.gui.click-select")
        );
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
