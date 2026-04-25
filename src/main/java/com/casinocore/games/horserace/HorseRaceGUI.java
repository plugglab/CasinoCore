package com.casinocore.games.horserace;

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
import java.util.Map;

public class HorseRaceGUI implements InventoryHolder {

    private static final int[] SELECT_SLOTS = {0, 1, 2, 3, 4};
    private static final int[] START_ROWS = {9, 18, 27, 36, 45};
    private static final Material[] HORSE_ARMORS = {
        Material.LEATHER_HORSE_ARMOR,
        Material.IRON_HORSE_ARMOR,
        Material.GOLDEN_HORSE_ARMOR,
        Material.DIAMOND_HORSE_ARMOR,
        Material.SADDLE
    };

    private final CasinoPlugin plugin;
    private final HorseRaceGame game;
    private final Player player;
    private final Inventory inventory;
    private final String[] horseKeys;

    private double betAmount;
    private String selectedHorse;

    public HorseRaceGUI(CasinoPlugin plugin, HorseRaceGame game, Player player, double betAmount, String[] horseKeys) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.betAmount = betAmount;
        this.horseKeys = horseKeys;
        this.inventory = Bukkit.createInventory(this, 54, Component.text(t("horserace.gui.title-plain")));
        render();
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_AMBIENT, 0.7f, 1.0f);
    }

    public void render() {
        HorseRaceRound round = game.getCurrentRound();

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int baseSlot = 9 + row * 9 + col;
                inventory.setItem(baseSlot, item(Material.GREEN_STAINED_GLASS_PANE, " "));
            }
        }

        inventory.setItem(5, item(statusMaterial(round), statusName(round), statusLore(round)));
        inventory.setItem(6, item(Material.SADDLE, t("horserace.gui.title"),
            f("horserace.gui.bet", "amount", plugin.getEconomyManager().format(betAmount)),
            selectedHorse == null ? t("horserace.gui.select-runner") : f("horserace.gui.selected", "horse", game.getDisplayHorseName(selectedHorse)),
            f("horserace.gui.pool", "amount", plugin.getEconomyManager().format(round.getTotalPool()))
        ));
        inventory.setItem(7, item(Material.LIME_DYE, actionName(round), actionLore(round)));
        inventory.setItem(8, item(Material.BARRIER, t("horserace.gui.back"), t("horserace.gui.back-lore")));

        for (int i = 0; i < horseKeys.length; i++) {
            boolean selected = horseKeys[i].equals(selectedHorse);
            inventory.setItem(SELECT_SLOTS[i], item(
                selected ? Material.EMERALD_BLOCK : HORSE_ARMORS[i],
                game.getDisplayHorseName(horseKeys[i]),
                f("horserace.gui.pool-on-horse", "amount", plugin.getEconomyManager().format(game.getPlayerBetForHorse(horseKeys[i]))),
                f("horserace.gui.live-payout", "value", String.format("%.2f", game.getHorseMultiplier(horseKeys[i]))),
                selected ? t("horserace.gui.selected-runner") : t("horserace.gui.click-horse")
            ));
        }

        renderTrack(round);
    }

    private Material statusMaterial(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> Material.BELL;
            case COUNTDOWN -> Material.CLOCK;
            case RACING -> Material.REDSTONE_TORCH;
            case FINISHED -> Material.GOLD_BLOCK;
        };
    }

    private String statusName(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> t("horserace.gui.status.open");
            case COUNTDOWN -> t("horserace.gui.status.countdown");
            case RACING -> t("horserace.gui.status.racing");
            case FINISHED -> t("horserace.gui.status.finished");
        };
    }

    private String statusLore(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> t("horserace.gui.status-lore.open");
            case COUNTDOWN -> f("horserace.gui.status-lore.countdown", "seconds", String.valueOf(round.getCountdownSeconds()));
            case RACING -> t("horserace.gui.status-lore.racing");
            case FINISHED -> f("horserace.gui.status-lore.finished", "winner", game.getDisplayHorseName(round.getWinner()));
        };
    }

    private String actionName(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> t("horserace.gui.action.open");
            case COUNTDOWN -> t("horserace.gui.action.countdown");
            case RACING -> t("horserace.gui.action.racing");
            case FINISHED -> t("horserace.gui.action.finished");
        };
    }

    private String actionLore(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> t("horserace.gui.action-lore.open");
            case COUNTDOWN -> t("horserace.gui.action-lore.countdown");
            case RACING -> t("horserace.gui.action-lore.racing");
            case FINISHED -> t("horserace.gui.action-lore.finished");
        };
    }

    private void renderTrack(HorseRaceRound round) {
        int[] positions = round.getPositions();
        for (int i = 0; i < horseKeys.length; i++) {
            int rowSlot = START_ROWS[i];
            for (int col = 1; col <= 8; col++) {
                inventory.setItem(rowSlot + col, item(col == 8 ? Material.GOLD_BLOCK : Material.WHITE_STAINED_GLASS_PANE, col == 8 ? t("horserace.gui.finish") : t("horserace.gui.track")));
            }
            int lanePosition = Math.max(0, Math.min(8, positions[i]));
            inventory.setItem(rowSlot + lanePosition, item(HORSE_ARMORS[i], game.getDisplayHorseName(horseKeys[i]), f("horserace.gui.lane", "index", String.valueOf(i + 1))));
        }
    }

    public String mapHorse(int slot) {
        for (int i = 0; i < SELECT_SLOTS.length; i++) {
            if (SELECT_SLOTS[i] == slot) {
                return horseKeys[i];
            }
        }
        return null;
    }

    public void selectHorse(String horse) {
        selectedHorse = horse;
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
    }

    public void showResult(String winner, boolean won, double payout, double multiplier) {
        inventory.setItem(5, item(won ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            won ? t("horserace.gui.result-win") : t("horserace.gui.result-loss"),
            f("horserace.gui.result-winner", "winner", game.getDisplayHorseName(winner)),
            won ? f("horserace.gui.result-payout", "amount", plugin.getEconomyManager().format(payout)) : f("horserace.gui.result-lost", "amount", plugin.getEconomyManager().format(betAmount))
        ));
        plugin.getMessageManager().send(
            player,
            plugin.getLocaleManager().formatText(won ? "horserace.result.win" : "horserace.result.loss", Map.of(
                "selected", game.getDisplayHorseName(selectedHorse),
                "winner", game.getDisplayHorseName(winner),
                "odds", String.format("%.2f", multiplier),
                "payout", plugin.getEconomyManager().format(payout),
                "amount", plugin.getEconomyManager().format(betAmount)
            ))
        );
    }

    public void back() {
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setPendingBet(double betAmount) {
        this.betAmount = betAmount;
    }

    public String getSelectedHorse() {
        return selectedHorse;
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
