package com.casinocore.games.highlow;

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

public class HighLowGUI implements InventoryHolder {

    private final CasinoPlugin plugin;
    private final HighLowGame game;
    private final Player player;
    private final Inventory inventory;
    private final double bet;
    private final int currentCard;
    private int nextCard;
    private boolean roundOver;
    private boolean resolving;

    public HighLowGUI(CasinoPlugin plugin, HighLowGame game, Player player, double bet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.bet = bet;
        this.currentCard = drawCard();
        this.inventory = Bukkit.createInventory(this, 54, Component.text("High Low"));
        render("Pick higher or lower");
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public Player getPlayer() {
        return player;
    }

    public double getBet() {
        return bet;
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public void reveal(boolean guessHigher) {
        if (roundOver || resolving) {
            return;
        }
        resolving = true;
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (ticks++ < 10) {
                    inventory.setItem(31, cardItem(drawCard(), "Revealing..."));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f + ticks * 0.03f);
                    return;
                }

                cancel();
                nextCard = drawCard();
                boolean won = guessHigher ? nextCard > currentCard : nextCard < currentCard;
                double payout = won ? bet * game.getWinMultiplier() : 0.0;
                roundOver = true;
                resolving = false;
                render(won ? "You won" : "You lost");
                player.playSound(player.getLocation(), won ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0f, won ? 1.1f : 0.8f);
                game.resolveRound(
                    player,
                    bet,
                    won,
                    payout,
                    "<gold><bold>High Low</bold></gold>\n" +
                        "<gray>Current:</gray> <white>" + currentCard + "</white>\n" +
                        "<gray>Next:</gray> <white>" + nextCard + "</white>\n" +
                        (won
                            ? "<gray>Payout:</gray> <green>" + plugin.getEconomyManager().format(payout) + "</green>"
                            : "<gray>Lost:</gray> <red>" + plugin.getEconomyManager().format(bet) + "</red>")
                );
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 2L);
    }

    private void render(String status) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, item(Material.PAPER, "High Low", "Bet: " + plugin.getEconomyManager().format(bet), status));
        inventory.setItem(22, cardItem(currentCard, "Current card"));
        inventory.setItem(31, roundOver ? cardItem(nextCard, "Next card") : item(Material.BLACK_STAINED_GLASS_PANE, "Hidden", "Revealed after your guess"));
        inventory.setItem(47, item(roundOver ? Material.GRAY_DYE : Material.BLUE_DYE, "Lower", roundOver ? "Round finished" : "Guess the next card is lower"));
        inventory.setItem(49, item(roundOver ? Material.BARRIER : Material.GRAY_DYE, "Close", roundOver ? "Close this table" : "Available after result"));
        inventory.setItem(51, item(roundOver ? Material.GRAY_DYE : Material.RED_DYE, "Higher", roundOver ? "Round finished" : "Guess the next card is higher"));
        inventory.setItem(53, item(roundOver ? Material.LIME_DYE : Material.GRAY_DYE, "Play Again", roundOver ? "Play another round with same bet" : "Available after result"));
    }

    private int drawCard() {
        return ThreadLocalRandom.current().nextInt(1, 14);
    }

    private ItemStack cardItem(int value, String subtitle) {
        return item(Material.PAPER, "Card " + value, subtitle);
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
