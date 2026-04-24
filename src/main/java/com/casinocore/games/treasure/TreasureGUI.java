package com.casinocore.games.treasure;

import com.casinocore.core.CasinoPlugin;
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
import java.util.concurrent.ThreadLocalRandom;

public class TreasureGUI implements InventoryHolder {

    private static final int[] CHEST_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final CasinoPlugin plugin;
    private final TreasureGame game;
    private final Player player;
    private final Inventory inventory;
    private final double bet;
    private final int winningIndex;
    private boolean resolved;

    public TreasureGUI(CasinoPlugin plugin, TreasureGame game, Player player, double bet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.bet = bet;
        this.inventory = Bukkit.createInventory(this, 45, Component.text("Treasure Pick"));
        this.winningIndex = ThreadLocalRandom.current().nextInt(CHEST_SLOTS.length);
        render();
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void handleSlotClick(int slot) {
        if (resolved) {
            if (slot == 40) {
                game.finishSession(player);
                player.closeInventory();
            }
            return;
        }

        for (int i = 0; i < CHEST_SLOTS.length; i++) {
            if (CHEST_SLOTS[i] == slot) {
                revealChoice(i);
                return;
            }
        }
    }

    private void render() {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(4, item(Material.CHEST, "<gold><bold>Treasure Pick</bold></gold>",
            "<gray>How to play:</gray> <white>Choose one chest.</white>",
            "<gray>One chest wins 4.2x. The others are empty.</gray>",
            "<gray>Bet:</gray> <gold>" + plugin.getEconomyManager().format(bet) + "</gold>"
        ));
        for (int slot : CHEST_SLOTS) {
            inventory.setItem(slot, item(Material.CHEST, "<yellow>Pick Me</yellow>", "<gray>One chest hides the treasure</gray>"));
        }
        inventory.setItem(40, item(Material.BARRIER, "<red>Close</red>", resolved ? "<gray>Close this table</gray>" : "<gray>Available after reveal</gray>"));
    }

    private void revealChoice(int selectedIndex) {
        resolved = true;
        new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                if (step < CHEST_SLOTS.length) {
                    inventory.setItem(CHEST_SLOTS[step], item(Material.BARREL, "<gray>Searching...</gray>", "<gray>Checking chest " + (step + 1) + "</gray>"));
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.4f, 0.9f + step * 0.04f);
                    step++;
                    return;
                }

                cancel();
                for (int i = 0; i < CHEST_SLOTS.length; i++) {
                    boolean winner = i == winningIndex;
                    inventory.setItem(CHEST_SLOTS[i], item(
                        winner ? Material.EMERALD_BLOCK : Material.COAL_BLOCK,
                        winner ? "<green>Treasure</green>" : "<red>Empty</red>",
                        winner ? "<gold>Winner chest</gold>" : "<gray>No luck here</gray>"
                    ));
                }

                boolean won = selectedIndex == winningIndex;
                double payout = won ? bet * 4.2 : 0.0;
                inventory.setItem(4, item(
                    won ? Material.EMERALD : Material.REDSTONE,
                    won ? "<green><bold>You Found It</bold></green>" : "<red><bold>No Treasure</bold></red>",
                    won ? "<gray>Payout:</gray> <green>" + plugin.getEconomyManager().format(payout) + "</green>"
                        : "<gray>Better luck next time</gray>"
                ));
                inventory.setItem(40, item(Material.BARRIER, "<red>Close</red>", "<gray>Close this table</gray>"));
                player.playSound(player.getLocation(), won ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0f, won ? 1.1f : 0.8f);
                game.resolve(player, bet, won, payout);
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 3L);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(name));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(plugin.getMessageManager().parse(line));
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
