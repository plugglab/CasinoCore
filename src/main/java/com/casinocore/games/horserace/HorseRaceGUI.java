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
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Horse Race"));
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
        inventory.setItem(6, item(Material.SADDLE, "Horse Race",
            "Bet: " + plugin.getEconomyManager().format(betAmount),
            selectedHorse == null ? "Select your runner" : "Selected: " + game.getDisplayHorseName(selectedHorse),
            "Pool: " + plugin.getEconomyManager().format(round.getTotalPool())
        ));
        inventory.setItem(7, item(Material.LIME_DYE, actionName(round), actionLore(round)));
        inventory.setItem(8, item(Material.BARRIER, "Back", "Return to the casino hub"));

        for (int i = 0; i < horseKeys.length; i++) {
            boolean selected = horseKeys[i].equals(selectedHorse);
            inventory.setItem(SELECT_SLOTS[i], item(
                selected ? Material.EMERALD_BLOCK : HORSE_ARMORS[i],
                game.getDisplayHorseName(horseKeys[i]),
                "Pool on horse: " + plugin.getEconomyManager().format(game.getPlayerBetForHorse(horseKeys[i])),
                "Live payout: " + String.format("%.2f", game.getHorseMultiplier(horseKeys[i])) + "x",
                selected ? "Selected runner" : "Click to back this horse"
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
            case OPEN -> "Race Lobby";
            case COUNTDOWN -> "Countdown";
            case RACING -> "Race Live";
            case FINISHED -> "Race Finished";
        };
    }

    private String statusLore(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> "Place a bet to start the shared countdown";
            case COUNTDOWN -> "Starts in " + round.getCountdownSeconds() + "s";
            case RACING -> "All open race GUIs are showing the same race";
            case FINISHED -> "Winner: " + game.getDisplayHorseName(round.getWinner());
        };
    }

    private String actionName(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> "Join And Start";
            case COUNTDOWN -> "Join Race";
            case RACING -> "Race Running";
            case FINISHED -> "Next Race Soon";
        };
    }

    private String actionLore(HorseRaceRound round) {
        return switch (round.getState()) {
            case OPEN -> "Locks in your horse and starts countdown";
            case COUNTDOWN -> "Join before the countdown reaches zero";
            case RACING -> "Wait for this race to finish";
            case FINISHED -> "Open a new board or wait for the reset";
        };
    }

    private void renderTrack(HorseRaceRound round) {
        int[] positions = round.getPositions();
        for (int i = 0; i < horseKeys.length; i++) {
            int rowSlot = START_ROWS[i];
            for (int col = 1; col <= 8; col++) {
                inventory.setItem(rowSlot + col, item(col == 8 ? Material.GOLD_BLOCK : Material.WHITE_STAINED_GLASS_PANE, col == 8 ? "Finish" : "Track"));
            }
            int lanePosition = Math.max(0, Math.min(8, positions[i]));
            inventory.setItem(rowSlot + lanePosition, item(HORSE_ARMORS[i], game.getDisplayHorseName(horseKeys[i]), "Lane " + (i + 1)));
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
            won ? "Photo Finish Win" : "Race Lost",
            "Winner: " + game.getDisplayHorseName(winner),
            won ? "Payout: " + plugin.getEconomyManager().format(payout) : "Lost: " + plugin.getEconomyManager().format(betAmount)
        ));
        plugin.getMessageManager().send(
            player,
            (won ? "<green><bold>Photo Finish Win</bold></green>" : "<red><bold>Race Lost</bold></red>") + "\n" +
                "<gray>Your Horse:</gray> <white>" + game.getDisplayHorseName(selectedHorse) + "</white>\n" +
                "<gray>Winner:</gray> <gold>" + game.getDisplayHorseName(winner) + "</gold>\n" +
                (won ? "<gray>Pool Odds:</gray> <gold>" + String.format("%.2f", multiplier) + "x</gold>\n<gray>Payout:</gray> <green>" +
                    plugin.getEconomyManager().format(payout) + "</green>" :
                    "<gray>Lost:</gray> <red>" + plugin.getEconomyManager().format(betAmount) + "</red>")
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
