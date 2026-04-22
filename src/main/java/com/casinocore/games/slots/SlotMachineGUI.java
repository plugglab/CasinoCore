package com.casinocore.games.slots;

import com.casinocore.core.CasinoPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Random;

public class SlotMachineGUI implements InventoryHolder {

    private static final int RESULT_1 = 19;
    private static final int RESULT_2 = 22;
    private static final int RESULT_3 = 25;

    private final CasinoPlugin plugin;
    private final Player player;
    private final double bet;
    private final Inventory inventory;
    private final Random random;

    private boolean spinning;
    private SlotSymbol[] finalResults;

    public SlotMachineGUI(CasinoPlugin plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Slot Machine"));
        this.random = new Random();
        setupGUI();
    }

    private void setupGUI() {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack border = createItem(Material.YELLOW_STAINED_GLASS_PANE, "Casino", "");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(18 + i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        ItemStack questionMark = createItem(Material.PAPER, "Spinning", "Reels are warming up");
        inventory.setItem(RESULT_1, questionMark);
        inventory.setItem(RESULT_2, questionMark);
        inventory.setItem(RESULT_3, questionMark);

        inventory.setItem(4, createItem(
            Material.GOLD_NUGGET,
            "Bet",
            "Amount: " + plugin.getEconomyManager().format(bet),
            "Watch the reels closely"
        ));
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

        String summary = multiplier > 0
            ? "Profit: +" + plugin.getEconomyManager().format(winnings - bet)
            : "Try another spin";
        inventory.setItem(4, createItem(
            multiplier > 0 ? Material.EMERALD : Material.REDSTONE,
            multiplier > 0 ? "You Win" : "No Match",
            "Outcome: " + SlotSymbol.getOutcomeDescription(results),
            "Bet: " + plugin.getEconomyManager().format(bet),
            summary
        ));
        inventory.setItem(22, createItem(Material.BARRIER, "Close", "Leave the machine"));
        spinning = false;
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

    public boolean isSpinning() {
        return spinning;
    }

    public SlotSymbol[] getFinalResults() {
        return finalResults == null ? null : finalResults.clone();
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
