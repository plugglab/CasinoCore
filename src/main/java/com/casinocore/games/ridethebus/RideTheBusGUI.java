package com.casinocore.games.ridethebus;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RideTheBusGUI implements InventoryHolder {

    private static final int SLOT_STATUS = 4;
    private static final int SLOT_CARD_ONE = 19;
    private static final int SLOT_CARD_TWO = 21;
    private static final int SLOT_CARD_THREE = 23;
    private static final int SLOT_CARD_FOUR = 25;
    private static final int SLOT_ACTION_LEFT = 47;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_ACTION_RIGHT = 51;
    private static final int SLOT_REPLAY = 53;

    private final CasinoPlugin plugin;
    private final RideTheBusGame game;
    private final Player player;
    private final Inventory inventory;
    private final double bet;
    private final Card firstCard;
    private Card secondCard;
    private Card thirdCard;
    private Card fourthCard;
    private int stage;
    private boolean resolved;
    private boolean won;

    public RideTheBusGUI(CasinoPlugin plugin, RideTheBusGame game, Player player, double bet) {
        this.plugin = plugin;
        this.game = game;
        this.player = player;
        this.bet = bet;
        this.firstCard = drawCard();
        this.inventory = Bukkit.createInventory(this, 54, Component.text(t("ridethebus.gui.title-plain")));
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

    public void handleClick(int slot) {
        if (slot == SLOT_BACK && resolved) {
            game.backToHub(player);
            return;
        }
        if (slot == SLOT_REPLAY && resolved) {
            game.replay(player, bet);
            return;
        }
        if (resolved) {
            return;
        }
        if (slot == SLOT_ACTION_LEFT) {
            handleLeftAction();
        } else if (slot == SLOT_ACTION_RIGHT) {
            handleRightAction();
        }
    }

    private void handleLeftAction() {
        switch (stage) {
            case 0 -> resolveColor(true);
            case 1 -> resolveHighLow(false);
            case 2 -> resolveInsideOutside(true);
            case 3 -> resolveSuit(CardSuit.HEARTS, CardSuit.DIAMONDS);
            default -> {
            }
        }
    }

    private void handleRightAction() {
        switch (stage) {
            case 0 -> resolveColor(false);
            case 1 -> resolveHighLow(true);
            case 2 -> resolveInsideOutside(false);
            case 3 -> resolveSuit(CardSuit.SPADES, CardSuit.CLUBS);
            default -> {
            }
        }
    }

    private void resolveColor(boolean red) {
        secondCard = drawCard();
        boolean success = secondCard.isRed() == red;
        finishStep(success);
    }

    private void resolveHighLow(boolean higher) {
        thirdCard = drawCard();
        boolean success = higher ? thirdCard.value() > secondCard.value() : thirdCard.value() < secondCard.value();
        finishStep(success);
    }

    private void resolveInsideOutside(boolean inside) {
        fourthCard = drawCard();
        int min = Math.min(secondCard.value(), thirdCard.value());
        int max = Math.max(secondCard.value(), thirdCard.value());
        boolean actualInside = fourthCard.value() > min && fourthCard.value() < max;
        boolean success = inside ? actualInside : !actualInside;
        finishStep(success);
    }

    private void resolveSuit(CardSuit left, CardSuit right) {
        Card target = fourthCard != null ? fourthCard : drawCard();
        fourthCard = target;
        boolean success = target.suit() == left || target.suit() == right;
        finishStep(success);
    }

    private void finishStep(boolean success) {
        if (!success) {
            resolved = true;
            won = false;
            render();
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            game.resolve(player, bet, false, 0.0, Map.of(
                "bet", plugin.getEconomyManager().format(bet),
                "multiplier", String.format("%.2f", getCurrentMultiplier()) + "x"
            ));
            return;
        }

        stage++;
        if (stage >= 4) {
            resolved = true;
            won = true;
            double payout = bet * getCurrentMultiplier();
            render();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.05f);
            game.resolve(player, bet, true, payout, Map.of(
                "bet", plugin.getEconomyManager().format(bet),
                "payout", plugin.getEconomyManager().format(payout),
                "multiplier", String.format("%.2f", getCurrentMultiplier()) + "x"
            ));
            return;
        }

        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.15f);
    }

    private void render() {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(SLOT_STATUS, item(
            won ? Material.EMERALD : resolved ? Material.REDSTONE : Material.MAP,
            t("ridethebus.gui.title"),
            f("ridethebus.gui.bet", "amount", plugin.getEconomyManager().format(bet)),
            f("ridethebus.gui.multiplier", "value", String.format("%.2f", getCurrentMultiplier()) + "x"),
            t(statusKey())
        ));

        inventory.setItem(SLOT_CARD_ONE, cardItem(firstCard, t("ridethebus.gui.card-one")));
        inventory.setItem(SLOT_CARD_TWO, secondCard == null ? hiddenCard(t("ridethebus.gui.card-two")) : cardItem(secondCard, t("ridethebus.gui.card-two")));
        inventory.setItem(SLOT_CARD_THREE, thirdCard == null ? hiddenCard(t("ridethebus.gui.card-three")) : cardItem(thirdCard, t("ridethebus.gui.card-three")));
        inventory.setItem(SLOT_CARD_FOUR, fourthCard == null ? hiddenCard(t("ridethebus.gui.card-four")) : cardItem(fourthCard, t("ridethebus.gui.card-four")));

        if (resolved) {
            inventory.setItem(SLOT_ACTION_LEFT, item(Material.GRAY_DYE, t("ridethebus.gui.locked"), t("ridethebus.gui.round-finished")));
            inventory.setItem(SLOT_ACTION_RIGHT, item(Material.GRAY_DYE, t("ridethebus.gui.locked"), t("ridethebus.gui.round-finished")));
            inventory.setItem(SLOT_BACK, item(Material.BARRIER, t("ridethebus.gui.back"), t("ridethebus.gui.back-lore")));
            inventory.setItem(SLOT_REPLAY, item(Material.LIME_DYE, t("ridethebus.gui.play-again"), t("ridethebus.gui.play-again-lore")));
            return;
        }

        inventory.setItem(SLOT_BACK, item(Material.GRAY_DYE, t("ridethebus.gui.back"), t("ridethebus.gui.after-round")));
        inventory.setItem(SLOT_REPLAY, item(Material.GRAY_DYE, t("ridethebus.gui.play-again"), t("ridethebus.gui.after-round")));

        switch (stage) {
            case 0 -> {
                inventory.setItem(SLOT_ACTION_LEFT, item(Material.RED_DYE, t("ridethebus.gui.red"), t("ridethebus.gui.red-lore")));
                inventory.setItem(SLOT_ACTION_RIGHT, item(Material.COAL, t("ridethebus.gui.black"), t("ridethebus.gui.black-lore")));
            }
            case 1 -> {
                inventory.setItem(SLOT_ACTION_LEFT, item(Material.BLUE_DYE, t("ridethebus.gui.lower"), t("ridethebus.gui.lower-lore")));
                inventory.setItem(SLOT_ACTION_RIGHT, item(Material.RED_DYE, t("ridethebus.gui.higher"), t("ridethebus.gui.higher-lore")));
            }
            case 2 -> {
                inventory.setItem(SLOT_ACTION_LEFT, item(Material.LIME_DYE, t("ridethebus.gui.inside"), t("ridethebus.gui.inside-lore")));
                inventory.setItem(SLOT_ACTION_RIGHT, item(Material.ORANGE_DYE, t("ridethebus.gui.outside"), t("ridethebus.gui.outside-lore")));
            }
            case 3 -> {
                inventory.setItem(SLOT_ACTION_LEFT, item(Material.PINK_DYE, t("ridethebus.gui.hearts-diamonds"), t("ridethebus.gui.hearts-diamonds-lore")));
                inventory.setItem(SLOT_ACTION_RIGHT, item(Material.GRAY_DYE, t("ridethebus.gui.spades-clubs"), t("ridethebus.gui.spades-clubs-lore")));
            }
            default -> {
            }
        }
    }

    private String statusKey() {
        if (won) {
            return "ridethebus.gui.status.won";
        }
        if (resolved) {
            return "ridethebus.gui.status.lost";
        }
        return switch (stage) {
            case 0 -> "ridethebus.gui.status.color";
            case 1 -> "ridethebus.gui.status.highlow";
            case 2 -> "ridethebus.gui.status.insideoutside";
            case 3 -> "ridethebus.gui.status.suit";
            default -> "ridethebus.gui.status.color";
        };
    }

    private double getCurrentMultiplier() {
        if (stage >= 4) {
            return game.getStageMultiplier("final", 7.5);
        }
        if (stage == 3) {
            return game.getStageMultiplier("suit-preview", 7.5);
        }
        if (stage == 2) {
            return game.getStageMultiplier("inside-outside-preview", 4.0);
        }
        if (stage == 1) {
            return game.getStageMultiplier("highlow-preview", 2.2);
        }
        return game.getStageMultiplier("color-preview", 1.3);
    }

    private Card drawCard() {
        CardSuit[] suits = CardSuit.values();
        return new Card(ThreadLocalRandom.current().nextInt(1, 14), suits[ThreadLocalRandom.current().nextInt(suits.length)]);
    }

    private ItemStack hiddenCard(String label) {
        return item(Material.GRAY_STAINED_GLASS_PANE, label, t("ridethebus.gui.hidden-lore"));
    }

    private ItemStack cardItem(Card card, String label) {
        return item(card.isRed() ? Material.RED_TERRACOTTA : Material.BLACK_TERRACOTTA, label,
            f("ridethebus.gui.card-value", "value", card.display()),
            f("ridethebus.gui.card-suit", "value", card.suit().displayName()));
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

    private record Card(int value, CardSuit suit) {
        private boolean isRed() {
            return suit == CardSuit.HEARTS || suit == CardSuit.DIAMONDS;
        }

        private String display() {
            return switch (value) {
                case 1 -> "A";
                case 11 -> "J";
                case 12 -> "Q";
                case 13 -> "K";
                default -> String.valueOf(value);
            };
        }
    }

    private enum CardSuit {
        HEARTS("Hearts"),
        DIAMONDS("Diamonds"),
        SPADES("Spades"),
        CLUBS("Clubs");

        private final String displayName;

        CardSuit(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
