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
import java.util.Map;
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
        this.inventory = Bukkit.createInventory(this, 54, Component.text(t("doubleup.gui.title-plain")));
        render(t("doubleup.gui.status.choose"));
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
        render(t("doubleup.gui.status.cashout"));
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
                    inventory.setItem(22, item(Material.GLOWSTONE_DUST, t("doubleup.gui.risk-meter"), t("doubleup.gui.rolling")));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.9f + ticks * 0.03f);
                    return;
                }
                cancel();
                boolean won = ThreadLocalRandom.current().nextDouble(100.0) < game.getRoundWinChance();
                if (won) {
                    currentPot *= 2.0;
                    streak++;
                    resolving = false;
                    render(t("doubleup.gui.status.success"));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    roundOver = true;
                    resolving = false;
                    render(t("doubleup.gui.status.bust"));
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

        inventory.setItem(4, item(Material.BLAZE_POWDER, t("doubleup.gui.title"),
            t("doubleup.gui.help-1"),
            t("doubleup.gui.help-2")
        ));
        inventory.setItem(22, item(Material.SUNFLOWER, t("doubleup.gui.current-pot"),
            f("doubleup.gui.starting-bet", "amount", plugin.getEconomyManager().format(baseBet)),
            f("doubleup.gui.pot", "amount", plugin.getEconomyManager().format(currentPot)),
            f("doubleup.gui.streak", "value", String.valueOf(streak)),
            status
        ));
        inventory.setItem(47, item(roundOver ? Material.GRAY_DYE : Material.LIME_DYE, t("doubleup.gui.cashout"),
            roundOver ? t("doubleup.gui.round-finished") : t("doubleup.gui.cashout-lore")));
        inventory.setItem(49, item(roundOver ? Material.BARRIER : Material.GRAY_DYE, t("doubleup.gui.back"),
            roundOver ? t("doubleup.gui.back-lore") : t("doubleup.gui.after-round")));
        inventory.setItem(51, item(roundOver ? Material.GRAY_DYE : Material.RED_DYE, t("doubleup.gui.double"),
            roundOver ? t("doubleup.gui.round-finished") : t("doubleup.gui.double-lore")));
        inventory.setItem(53, item(roundOver ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE, t("doubleup.gui.play-again"),
            roundOver ? t("doubleup.gui.play-again-lore") : t("doubleup.gui.after-round")));
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
