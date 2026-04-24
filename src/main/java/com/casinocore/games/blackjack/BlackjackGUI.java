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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Blackjack"));
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

        inventory.setItem(4, item(Material.PAPER, "Blackjack", "Player vs Dealer"));
        inventory.setItem(22, item(
            Material.GOLD_INGOT,
            "Table Status",
            state.getStatus(),
            "Total Bet: " + plugin.getEconomyManager().format(state.getTotalCommittedBet())
        ));

        inventory.setItem(19, playerHeadItem(
            "Dealer",
            null,
            handLine(state.getDealerHand(), state.isDealerHidden()),
            scoreLine(state.getDealerHand(), state.isDealerHidden())
        ));

        renderHand(DEALER_SLOTS, state.getDealerHand(), state.isDealerHidden(), false);

        renderPlayerHandSection(0, 37, PLAYER_HAND_ONE_SLOTS, "Hand 1");
        if (state.hasSplitHand()) {
            renderPlayerHandSection(1, 41, PLAYER_HAND_TWO_SLOTS, "Hand 2");
        } else {
            for (int slot : PLAYER_HAND_TWO_SLOTS) {
                inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
            inventory.setItem(41, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        boolean playerTurn = state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN;
        inventory.setItem(47, item(playerTurn ? Material.LIME_DYE : Material.GRAY_DYE, "Hit",
            playerTurn ? "Draw one more card" : "Unavailable"));
        inventory.setItem(48, item(playerTurn ? Material.YELLOW_DYE : Material.GRAY_DYE, "Stand",
            playerTurn ? "End this hand" : "Unavailable"));
        inventory.setItem(50, item(canSplit() ? Material.DIAMOND : Material.GRAY_DYE, "Split",
            canSplit() ? "Split into two hands" : "Unavailable"));
        inventory.setItem(49, item(Material.BARRIER, "Back", state.getPhase() == BlackjackTableState.Phase.ROUND_OVER
            ? "Return to the casino hub"
            : "Available after the round"));
        inventory.setItem(51, item(
            state.getPhase() == BlackjackTableState.Phase.ROUND_OVER ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
            "Play Again",
            state.getPhase() == BlackjackTableState.Phase.ROUND_OVER
                ? "Start a new round with the same base bet"
                : "Available after the round"
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
        player.playSound(player.getLocation(),
            win ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO,
            1.0f,
            win ? 1.0f : 0.8f);
    }

    private void renderPlayerHandSection(int handIndex, int infoSlot, int[] cardSlots, String label) {
        BlackjackHand hand = state.getPlayerHands().get(handIndex);
        boolean active = state.getActiveHandIndex() == handIndex && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN;
        inventory.setItem(infoSlot, playerHeadItem(
            active ? "Player Hand " + (handIndex + 1) + " (Active)" : "Player Hand " + (handIndex + 1),
            player,
            "Bet: " + plugin.getEconomyManager().format(state.getBetForHand(handIndex)),
            "Cards: " + hand.getCards().size(),
            "Score: " + hand.getBestValue()
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
            if (hideSecondCard && i == 1) {
                inventory.setItem(slots[i], hiddenCardItem());
            } else {
                inventory.setItem(slots[i], cardItem(cards.get(i)));
            }
        }
    }

    private ItemStack cardItem(BlackjackCard card) {
        return playerHeadItem("Card: " + card.rank().getDisplayName(),
            null,
            card.suit().getDisplayName(),
            "Value: " + card.value());
    }

    private ItemStack hiddenCardItem() {
        return playerHeadItem("Hidden Card", null, "Revealed on dealer turn");
    }

    private String handLine(BlackjackHand hand, boolean hidden) {
        return "Cards: " + hand.getCards().size() + (hidden ? " (1 hidden)" : "");
    }

    private String scoreLine(BlackjackHand hand, boolean hidden) {
        if (hidden && hand.getCards().size() > 1) {
            return "Visible: " + hand.getCards().get(0).value() + "+";
        }
        return "Score: " + hand.getBestValue();
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
