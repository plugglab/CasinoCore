package com.casinocore.games.blackjack;

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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlackjackGUI implements InventoryHolder {

    private static final int[] DEALER_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] PLAYER_HAND_ONE_SLOTS = {28, 29, 30};
    private static final int[] PLAYER_HAND_TWO_SLOTS = {32, 33, 34};

    private final CasinoPlugin plugin;
    private final Player player;
    private final BlackjackTableState state;
    private final Inventory inventory;

    public BlackjackGUI(CasinoPlugin plugin, Player player, BlackjackTableState state) {
        this.plugin = plugin;
        this.player = player;
        this.state = state;
        this.inventory = Bukkit.createInventory(this, 54, Component.text(t("blackjack.gui.title-plain")));
        render();
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.1f);
    }

    public void render() {
        ItemStack filler = item(Material.GREEN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, item(Material.PAPER, t("blackjack.gui.title"), t("blackjack.gui.subtitle")));
        inventory.setItem(22, item(
            Material.GOLD_INGOT,
            t("blackjack.gui.status"),
            state.getStatus(),
            f("blackjack.gui.total-bet", "amount", plugin.getEconomyManager().format(state.getTotalCommittedBet()))
        ));

        inventory.setItem(19, playerHeadItem(
            t("blackjack.gui.dealer"),
            null,
            handLine(state.getDealerHand(), state.isDealerHidden()),
            scoreLine(state.getDealerHand(), state.isDealerHidden())
        ));

        renderHand(DEALER_SLOTS, state.getDealerHand(), state.isDealerHidden(), false);

        renderPlayerHandSection(0, 37, PLAYER_HAND_ONE_SLOTS);
        if (state.hasSplitHand()) {
            renderPlayerHandSection(1, 41, PLAYER_HAND_TWO_SLOTS);
        } else {
            for (int slot : PLAYER_HAND_TWO_SLOTS) {
                inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
            inventory.setItem(41, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        boolean playerTurn = state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN;
        inventory.setItem(47, item(playerTurn ? Material.LIME_DYE : Material.GRAY_DYE, t("blackjack.gui.hit"), playerTurn ? t("blackjack.gui.hit-lore") : t("blackjack.gui.unavailable")));
        inventory.setItem(48, item(playerTurn ? Material.YELLOW_DYE : Material.GRAY_DYE, t("blackjack.gui.stand"), playerTurn ? t("blackjack.gui.stand-lore") : t("blackjack.gui.unavailable")));
        inventory.setItem(50, item(canSplit() ? Material.DIAMOND : Material.GRAY_DYE, t("blackjack.gui.split"), canSplit() ? t("blackjack.gui.split-lore") : t("blackjack.gui.unavailable")));
        inventory.setItem(49, item(Material.BARRIER, t("blackjack.gui.back"), state.getPhase() == BlackjackTableState.Phase.ROUND_OVER ? t("blackjack.gui.back-ready") : t("blackjack.gui.after-round")));
        inventory.setItem(51, item(
            state.getPhase() == BlackjackTableState.Phase.ROUND_OVER ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
            t("blackjack.gui.play-again"),
            state.getPhase() == BlackjackTableState.Phase.ROUND_OVER ? t("blackjack.gui.play-again-ready") : t("blackjack.gui.after-round")
        ));
    }

    public Player getPlayer() {
        return player;
    }

    public BlackjackTableState getState() {
        return state;
    }

    public void playDealSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
    }

    public void playStandSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
    }

    public void playResultSound(boolean win) {
        player.playSound(player.getLocation(), win ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0f, win ? 1.0f : 0.8f);
    }

    private void renderPlayerHandSection(int handIndex, int infoSlot, int[] cardSlots) {
        BlackjackHand hand = state.getPlayerHands().get(handIndex);
        boolean active = state.getActiveHandIndex() == handIndex && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN;
        inventory.setItem(infoSlot, playerHeadItem(
            active ? f("blackjack.gui.player-hand-active", "index", String.valueOf(handIndex + 1)) : f("blackjack.gui.player-hand", "index", String.valueOf(handIndex + 1)),
            player,
            f("blackjack.gui.bet", "amount", plugin.getEconomyManager().format(state.getBetForHand(handIndex))),
            f("blackjack.gui.cards", "count", String.valueOf(hand.getCards().size())),
            f("blackjack.gui.score", "score", String.valueOf(hand.getBestValue()))
        ));
        renderHand(cardSlots, hand, false, active);
    }

    private boolean canSplit() {
        return state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN && state.canSplitCurrentHand();
    }

    private void renderHand(int[] slots, BlackjackHand hand, boolean hideSecondCard, boolean active) {
        for (int i = 0; i < slots.length; i++) {
            inventory.setItem(slots[i], item(active ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        List<BlackjackCard> cards = hand.getCards();
        for (int i = 0; i < cards.size() && i < slots.length; i++) {
            inventory.setItem(slots[i], hideSecondCard && i == 1 ? hiddenCardItem() : cardItem(cards.get(i)));
        }
    }

    private ItemStack cardItem(BlackjackCard card) {
        return playerHeadItem(f("blackjack.gui.card-name", "rank", card.rank().getDisplayName()), null, card.suit().getDisplayName(), f("blackjack.gui.card-value", "value", String.valueOf(card.value())));
    }

    private ItemStack hiddenCardItem() {
        return playerHeadItem(t("blackjack.gui.hidden-card"), null, t("blackjack.gui.hidden-card-lore"));
    }

    private String handLine(BlackjackHand hand, boolean hidden) {
        return hidden ? plugin.getLocaleManager().formatText("blackjack.gui.dealer-cards-hidden", Map.of("count", String.valueOf(hand.getCards().size()))) : f("blackjack.gui.cards", "count", String.valueOf(hand.getCards().size()));
    }

    private String scoreLine(BlackjackHand hand, boolean hidden) {
        if (hidden && hand.getCards().size() > 1) {
            return plugin.getLocaleManager().formatText("blackjack.gui.visible-score", Map.of("value", hand.getCards().get(0).value() + "+"));
        }
        return f("blackjack.gui.score", "score", String.valueOf(hand.getBestValue()));
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
            List<Component> loreLines = new ArrayList<>();
            for (String line : lore) {
                loreLines.add(Component.text(line));
            }
            meta.lore(loreLines);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack playerHeadItem(String name, Player headOwner, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof SkullMeta meta) {
            if (headOwner != null) {
                meta.setOwningPlayer(headOwner);
            }
            meta.displayName(Component.text(name));
            List<Component> loreLines = new ArrayList<>();
            for (String line : lore) {
                loreLines.add(Component.text(line));
            }
            meta.lore(loreLines);
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
