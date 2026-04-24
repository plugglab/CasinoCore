package com.casinocore.games.doubleup;

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

public class DoubleUpGUI implements InventoryHolder {

    private final CasinoPlugin plugin;
    private final DoubleUpGame game;
    private final Player player;
    private final Inventory inventory;
    private final double baseBet;
    private double currentPot;
    private int streak;
    private boolean roundOver;
    private boolean resolving;

    public DoubleUpGUI(CasinoPlugin plugin, DoubleUpGame game, Player player, double baseBet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.baseBet = baseBet;
        this.currentPot = baseBet * plugin.getConfigManager().getConfig().getDouble("games.doubleup.starting-multiplier", 1.6);
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Double Up"));
        render("<yellow>Choose cash out or double</yellow>");
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public Player getPlayer() {
        return player;
    }

    public double getBaseBet() {
        return baseBet;
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public void cashOut() {
        if (roundOver || resolving) {
            return;
        }
        roundOver = true;
        render("<green>Cash out locked in</green>");
        game.payCashout(player, baseBet, currentPot, streak);
    }

    public void tryDouble() {
        if (roundOver || resolving) {
            return;
        }
        resolving = true;
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (ticks++ < 12) {
                    inventory.setItem(22, item(Material.GLOWSTONE_DUST, "<gold>Risk Meter</gold>", "<yellow>Rolling...</yellow>"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.9f + ticks * 0.03f);
                    return;
                }
                cancel();
                boolean won = ThreadLocalRandom.current().nextDouble(100.0) < game.getRoundWinChance();
                if (won) {
                    currentPot *= 2.0;
                    streak++;
                    resolving = false;
                    render("<green>Success. Pot doubled.</green>");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    roundOver = true;
                    resolving = false;
                    render("<red>Bust. Pot lost.</red>");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    game.lose(player, baseBet, currentPot);
                }
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 2L);
    }

    private void render(String status) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, item(Material.BLAZE_POWDER, "<gold><bold>Double Up</bold></gold>",
            "<gray>How to play:</gray> <white>Cash out safely or try to double your pot.</white>",
            "<gray>Every double is a 50/50 risk.</gray>"
        ));
        inventory.setItem(22, item(Material.SUNFLOWER, "<yellow>Current Pot</yellow>",
            "<gray>Starting Bet:</gray> <white>" + plugin.getEconomyManager().format(baseBet) + "</white>",
            "<gray>Current Pot:</gray> <gold>" + plugin.getEconomyManager().format(currentPot) + "</gold>",
            "<gray>Win Streak:</gray> <white>" + streak + "</white>",
            status
        ));
        inventory.setItem(47, item(roundOver ? Material.GRAY_DYE : Material.LIME_DYE, "<green>Cash Out</green>",
            roundOver ? "<gray>Round finished</gray>" : "<gray>Take the current pot now</gray>"));
        inventory.setItem(49, item(roundOver ? Material.BARRIER : Material.GRAY_DYE, "<red>Close</red>",
            roundOver ? "<gray>Close this table</gray>" : "<gray>Available after the round</gray>"));
        inventory.setItem(51, item(roundOver ? Material.GRAY_DYE : Material.RED_DYE, "<red>Double</red>",
            roundOver ? "<gray>Round finished</gray>" : "<gray>50/50 chance to double the pot</gray>"));
        inventory.setItem(53, item(roundOver ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE, "<aqua>Play Again</aqua>",
            roundOver ? "<gray>Start a new table with the same bet</gray>" : "<gray>Available after the round</gray>"));
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
