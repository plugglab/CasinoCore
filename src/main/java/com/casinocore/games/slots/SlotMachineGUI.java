package com.casinocore.games.slots;

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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class SlotMachineGUI implements InventoryHolder {

    private static final int RESULT_1 = 19;
    private static final int RESULT_2 = 22;
    private static final int RESULT_3 = 25;
    private static final int LEVER_SLOT = 13;
    private static final int SPIN_AGAIN_SLOT = 23;
    private static final int BACK_SLOT = 26;

    private final CasinoPlugin plugin;
    private final SlotMachineGame game;
    private final Player player;
    private final double bet;
    private final Inventory inventory;
    private final Random random;

    private boolean spinning;
    private SlotSymbol[] finalResults;

    public SlotMachineGUI(CasinoPlugin plugin, SlotMachineGame game, Player player, double bet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.bet = bet;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(t("slots.gui.title-plain")));
        this.random = new Random();
        setupGUI();
    }

    private void setupGUI() {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack border = createItem(Material.YELLOW_STAINED_GLASS_PANE, t("slots.gui.casino"), "");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(18 + i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        ItemStack questionMark = createItem(Material.PAPER, t("slots.gui.waiting"), t("slots.gui.pull-lever-hint"));
        inventory.setItem(RESULT_1, questionMark);
        inventory.setItem(RESULT_2, questionMark);
        inventory.setItem(RESULT_3, questionMark);

        inventory.setItem(4, createItem(
            Material.GOLD_NUGGET,
            t("slots.gui.bet-title"),
            f("slots.gui.amount", "amount", plugin.getEconomyManager().format(bet)),
            t("slots.gui.pull-lever-hint")
        ));
        inventory.setItem(LEVER_SLOT, createItem(Material.LEVER, t("slots.gui.pull-lever"), t("slots.gui.start-reels")));
        inventory.setItem(SPIN_AGAIN_SLOT, createItem(Material.LIME_DYE, t("slots.gui.spin-again"), t("slots.gui.available-after-spin")));
        inventory.setItem(BACK_SLOT, createItem(Material.BARRIER, t("slots.gui.back"), t("slots.gui.back-lore")));
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public void spin(Runnable onComplete) {
        if (spinning) {
            return;
        }

        spinning = true;
        inventory.setItem(LEVER_SLOT, createItem(Material.LEVER, t("slots.gui.lever-locked"), t("slots.gui.spin-in-progress")));
        finalResults = new SlotSymbol[] {
            SlotSymbol.getRandomSymbol(random),
            SlotSymbol.getRandomSymbol(random),
            SlotSymbol.getRandomSymbol(random)
        };

        new SpinAnimation(this, finalResults, bet, onComplete).start();
    }

    public void updateReel(int reelIndex, SlotSymbol symbol) {
        int slot = switch (reelIndex) {
            case 0 -> RESULT_1;
            case 1 -> RESULT_2;
            case 2 -> RESULT_3;
            default -> -1;
        };

        if (slot != -1) {
            inventory.setItem(slot, symbol.createItem());
        }
    }

    public void showResults(SlotSymbol[] results, double multiplier, double winnings) {
        for (int i = 0; i < 3; i++) {
            updateReel(i, results[i]);
        }

        inventory.setItem(4, createItem(
            multiplier > 0 ? Material.EMERALD : Material.REDSTONE,
            multiplier > 0 ? t("slots.gui.you-win") : t("slots.gui.no-match"),
            f("slots.gui.outcome", "value", SlotSymbol.getOutcomeDescription(results)),
            f("slots.gui.bet-line", "amount", plugin.getEconomyManager().format(bet)),
            multiplier > 0
                ? f("slots.gui.profit", "amount", plugin.getEconomyManager().format(winnings - bet))
                : t("slots.gui.another-shot")
        ));
        inventory.setItem(LEVER_SLOT, createItem(Material.LEVER, t("slots.gui.pull-lever"), t("slots.gui.spin-another-round")));
        inventory.setItem(SPIN_AGAIN_SLOT, createItem(Material.LIME_DYE, t("slots.gui.spin-again"), t("slots.gui.same-bet")));
        inventory.setItem(BACK_SLOT, createItem(Material.BARRIER, t("slots.gui.back"), t("slots.gui.back-lore")));
        spinning = false;
    }

    public void backToHub() {
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    public void playSound(Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.55f, pitch);
    }

    public Player getPlayer() {
        return player;
    }

    public CasinoPlugin getPlugin() {
        return plugin;
    }

    public SlotMachineGame getGame() {
        return game;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public double getBet() {
        return bet;
    }

    public SlotSymbol[] getFinalResults() {
        return finalResults == null ? null : finalResults.clone();
    }

    private String t(String key) {
        return plugin.getLocaleManager().getText(key);
    }

    private String f(String key, String placeholder, String value) {
        return plugin.getLocaleManager().formatText(key, Map.of(placeholder, value));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(name));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore).map(plugin.getMessageManager()::parse).toList());
            }
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
