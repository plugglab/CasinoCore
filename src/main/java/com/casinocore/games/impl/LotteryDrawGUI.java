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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LotteryDrawGUI implements InventoryHolder {

    private static final int[] BALL_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final CasinoPlugin plugin;
    private final Player player;
    private final int chosenNumber;
    private final double bet;
    private final Inventory inventory;
    private final Random random;

    private boolean drawing;

    public LotteryDrawGUI(CasinoPlugin plugin, Player player, int chosenNumber, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.chosenNumber = chosenNumber;
        this.bet = bet;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Lottery Draw"));
        this.random = new Random();
        renderBase();
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void startDraw(int winningNumber, Runnable complete) {
        drawing = true;
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                for (int slot : BALL_SLOTS) {
                    int randomNumber = random.nextInt(100) + 1;
                    inventory.setItem(slot, ball(randomNumber, Material.WHITE_CONCRETE));
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 0.8f + (ticks * 0.01f));

                if (ticks++ >= 24) {
                    cancel();
                    renderWinningBall(winningNumber);
                    drawing = false;
                    complete.run();
                }
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 4L);
    }

    public void showResult(int winningNumber, boolean won, double multiplier, double payout, int difference) {
        inventory.setItem(22, item(
            won ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            won ? "Winner" : "Miss",
            "Your pick: " + chosenNumber,
            "Winning ball: " + winningNumber,
            won ? "Paid: " + plugin.getEconomyManager().format(payout) : "Lost: " + plugin.getEconomyManager().format(bet),
            "Difference: " + difference
        ));
        inventory.setItem(26, item(Material.BARRIER, "Back", "Return to the casino hub"));
    }

    public boolean isDrawing() {
        return drawing;
    }

    public void backToHub() {
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    private void renderBase() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(4, item(Material.EMERALD, "Lottery Machine", "Bet: " + plugin.getEconomyManager().format(bet), "Your pick: " + chosenNumber));
        inventory.setItem(22, item(Material.GOLD_INGOT, "Drawing", "Lottery balls are spinning"));
        inventory.setItem(26, item(Material.BARRIER, "Back", "Available after the draw"));
    }

    private void renderWinningBall(int winningNumber) {
        renderBase();
        inventory.setItem(13, ball(winningNumber, Material.GOLD_BLOCK));
    }

    private ItemStack ball(int number, Material material) {
        return item(material, "Ball " + number, "Winning candidate");
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
