package com.casinocore.games.roulette;

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

public class RouletteGUI implements InventoryHolder {

    private static final int[] NUMBER_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44, 45
    };

    private final CasinoPlugin plugin;
    private final RouletteGame game;
    private final Player player;
    private final double betAmount;
    private final Inventory inventory;

    private RouletteBet selectedBet;
    private boolean spinning;

    public RouletteGUI(CasinoPlugin plugin, RouletteGame game, Player player, double betAmount) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.betAmount = betAmount;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Roulette"));
        renderBase();
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.1f);
    }

    public void renderBase() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(4, item(Material.COMPASS, "Roulette Wheel", "Select a bet, then spin"));
        inventory.setItem(46, item(Material.PAPER, "Selected Bet", selectedBet == null ? "None" : selectedBet.getDisplayLabel(), "Bet: " + plugin.getEconomyManager().format(betAmount)));
        inventory.setItem(48, item(Material.BARRIER, "Clear", "Remove current selection"));
        inventory.setItem(49, createSimpleItem("2nd 12", RouletteBetType.SECOND_DOZEN, Material.LIGHT_BLUE_WOOL));
        inventory.setItem(50, item(Material.LIME_DYE, "Spin", "Launch the wheel"));
        inventory.setItem(53, item(Material.BARRIER, "Back", spinning ? "Available after the spin" : "Return to the casino hub"));

        inventory.setItem(47, createSimpleItem("Odd", RouletteBetType.ODD, Material.COAL));
        inventory.setItem(51, createSimpleItem("Red", RouletteBetType.RED, Material.RED_WOOL));
        inventory.setItem(52, createSimpleItem("Black", RouletteBetType.BLACK, Material.BLACK_WOOL));
        inventory.setItem(45, createSimpleItem("Even", RouletteBetType.EVEN, Material.IRON_INGOT));
        inventory.setItem(0, createSimpleItem("1-18", RouletteBetType.LOW, Material.LIME_STAINED_GLASS_PANE));
        inventory.setItem(1, createSimpleItem("1st 12", RouletteBetType.FIRST_DOZEN, Material.WHITE_WOOL));
        inventory.setItem(7, createSimpleItem("3rd 12", RouletteBetType.THIRD_DOZEN, Material.PURPLE_WOOL));
        inventory.setItem(8, createSimpleItem("19-36", RouletteBetType.HIGH, Material.ORANGE_STAINED_GLASS_PANE));

        for (int i = 0; i < 5; i++) {
            inventory.setItem(i + 2, createWheelItem(RouletteNumbers.WHEEL_ORDER.get(i), i == 2));
        }

        for (int number = 0; number <= 36; number++) {
            inventory.setItem(NUMBER_SLOTS[number], createNumberItem(number, isSelectedNumber(number)));
        }
    }

    public Player getPlayer() {
        return player;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    public RouletteBet getSelectedBet() {
        return selectedBet;
    }

    public void selectNumber(int number) {
        this.selectedBet = RouletteBet.singleNumber(number);
        renderBase();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    public void selectSimple(RouletteBetType type) {
        this.selectedBet = RouletteBet.simple(type);
        renderBase();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
    }

    public void clearSelection() {
        this.selectedBet = null;
        renderBase();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
    }

    public void back() {
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    public void updateWheelWindow(List<Integer> visibleNumbers) {
        for (int slot = 0; slot < 5; slot++) {
            int value = visibleNumbers.get(slot);
            inventory.setItem(slot + 2, createWheelItem(value, slot == 2));
        }
    }

    public void showResult(int resultNumber, boolean won, double multiplier, double winnings) {
        inventory.setItem(46, item(
            won ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            won ? "WIN" : "LOSS",
            "Ball landed on " + resultNumber,
            "Bet: " + (selectedBet == null ? "-" : selectedBet.getDisplayLabel()),
            won ? "Paid " + plugin.getEconomyManager().format(winnings) + " (" + multiplier + "x)" : "No payout"
        ));
        inventory.setItem(50, item(Material.LIGHT_GRAY_DYE, "Spin Complete", "Pick another bet or go back"));
        inventory.setItem(53, item(Material.BARRIER, "Back", "Return to the casino hub"));
    }

    private boolean isSelectedNumber(int number) {
        return selectedBet != null
            && selectedBet.getType() == RouletteBetType.SINGLE_NUMBER
            && selectedBet.getNumber() != null
            && selectedBet.getNumber() == number;
    }

    private ItemStack createSimpleItem(String name, RouletteBetType type, Material baseMaterial) {
        boolean selected = selectedBet != null && selectedBet.getType() == type;
        return item(selected ? Material.EMERALD_BLOCK : baseMaterial, name, payoutLine(type), selected ? "Selected" : "Click to bet");
    }

    private ItemStack createNumberItem(int number, boolean selected) {
        Material material;
        String colorName;

        if (number == 0) {
            material = selected ? Material.EMERALD_BLOCK : Material.GREEN_WOOL;
            colorName = "Green";
        } else if (RouletteNumbers.isRed(number)) {
            material = selected ? Material.GOLD_BLOCK : Material.RED_WOOL;
            colorName = "Red";
        } else {
            material = selected ? Material.GOLD_BLOCK : Material.BLACK_WOOL;
            colorName = "Black";
        }

        return item(material, String.valueOf(number), colorName + " " + number, payoutLine(RouletteBetType.SINGLE_NUMBER), selected ? "Selected" : "Click to bet");
    }

    private ItemStack createWheelItem(int number, boolean center) {
        Material material = number == 0 ? Material.GREEN_STAINED_GLASS_PANE
            : RouletteNumbers.isRed(number) ? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
        return item(material, (center ? "> " : "") + number + (center ? " <" : ""));
    }

    private String payoutLine(RouletteBetType type) {
        return "Payout: " + game.getPayoutMultiplier(type) + "x";
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
