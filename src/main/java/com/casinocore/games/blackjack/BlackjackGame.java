package com.casinocore.games.blackjack;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class BlackjackGame extends BaseCasinoGame {

    private final Plugin bukkitPlugin;
    private final Map<UUID, BlackjackGUI> sessions;

    public BlackjackGame(CasinoPlugin plugin) {
        super(plugin, "blackjack", "Blackjack", "Play blackjack against the dealer in a turn-based GUI.");
        this.bukkitPlugin = plugin.getPlugin();
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean play(Player player, double bet) {
        boolean betWithdrawn = false;
        try {
            if (!preGameValidation(player, bet)) {
                return false;
            }

            if (sessions.containsKey(player.getUniqueId())) {
                sendLocaleMessage(player, "blackjack.already-open");
                return false;
            }

            if (!withdrawBet(player, bet)) {
                return false;
            }
            betWithdrawn = true;

            setCooldown(player);

            BlackjackTableState state = new BlackjackTableState(bet);
            BlackjackGUI gui = new BlackjackGUI(plugin, player, state);
            sessions.put(player.getUniqueId(), gui);

            Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                gui.open();
                startRound(gui);
            });
            return true;
        } catch (Exception e) {
            handleGameError(player, bet, e, betWithdrawn);
            return false;
        }
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    public CasinoPlugin getCasinoPlugin() {
        return plugin;
    }

    public void handleClick(Player player, BlackjackGUI gui, int slot) {
        if (slot < 0 || slot >= gui.getInventory().getSize()) {
            return;
        }

        BlackjackTableState state = gui.getState();
        if (slot == 47 && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN) {
            hit(gui);
            return;
        }

        if (slot == 48 && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN) {
            stand(gui);
            return;
        }

        if (slot == 50 && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN) {
            split(gui);
            return;
        }

        if (slot == 49 && state.getPhase() == BlackjackTableState.Phase.ROUND_OVER) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            GuiNavigation.openHub(plugin, player);
            return;
        }

        if (slot == 51 && state.getPhase() == BlackjackTableState.Phase.ROUND_OVER) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            play(player, state.getBaseBet());
        }
    }

    public void handleClose(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        if (state.getPhase() != BlackjackTableState.Phase.ROUND_OVER) {
            Bukkit.getScheduler().runTaskLater(bukkitPlugin, () -> {
                if (sessions.containsKey(gui.getPlayer().getUniqueId())) {
                    gui.getPlayer().openInventory(gui.getInventory());
                }
            }, 1L);
        } else {
            sessions.remove(gui.getPlayer().getUniqueId());
        }
    }

    private void startRound(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        state.setStatus("Initial deal...");
        gui.render();

        new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                switch (step) {
                    case 0 -> dealToPlayer(gui, state.getCurrentHand(), "Player draws");
                    case 1 -> dealToDealer(gui);
                    case 2 -> dealToPlayer(gui, state.getCurrentHand(), "Player draws");
                    case 3 -> dealToDealer(gui);
                    default -> {
                        cancel();
                        evaluateInitialState(gui);
                    }
                }
                step++;
            }
        }.runTaskTimer(bukkitPlugin, 0L, 10L);
    }

    private void hit(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        BlackjackHand hand = state.getCurrentHand();
        dealToPlayer(gui, hand, "Player draws");

        if (hand.isBust() || hand.getBestValue() == 21) {
            advanceAfterHand(gui, hand.isBust() ? "Hand " + (state.getActiveHandIndex() + 1) + " bust" : "Hand " + (state.getActiveHandIndex() + 1) + " stands on 21");
        } else {
            state.setStatus("Choose Hit, Stand, or Split");
            gui.render();
        }
    }

    private void stand(BlackjackGUI gui) {
        gui.playStandSound();
        advanceAfterHand(gui, "Hand " + (gui.getState().getActiveHandIndex() + 1) + " stands");
    }

    private void split(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        if (!state.canSplitCurrentHand()) {
            sendLocaleMessage(gui.getPlayer(), "blackjack.split-invalid");
            return;
        }

        if (!checkBalance(gui.getPlayer(), state.getBaseBet()) || !withdrawBet(gui.getPlayer(), state.getBaseBet())) {
            return;
        }

        state.splitCurrentHand();
        dealToPlayer(gui, state.getPlayerHands().get(0), "Split hand 1 receives a card");
        state.setActiveHandIndex(1);
        dealToPlayer(gui, state.getCurrentHand(), "Split hand 2 receives a card");
        state.setActiveHandIndex(0);
        state.setStatus("Split complete. Play hand 1");
        gui.render();
    }

    private void advanceAfterHand(BlackjackGUI gui, String status) {
        BlackjackTableState state = gui.getState();
        if (state.hasNextHand()) {
            state.moveToNextHand();
            state.setStatus(status + " | Now playing hand " + (state.getActiveHandIndex() + 1));
            gui.render();
            return;
        }

        state.setPhase(BlackjackTableState.Phase.DEALER_TURN);
        state.setDealerHidden(false);
        state.setStatus("Dealer turn...");
        gui.render();
        runDealerTurn(gui);
    }

    private void runDealerTurn(BlackjackGUI gui) {
        new BukkitRunnable() {
            @Override
            public void run() {
                BlackjackTableState state = gui.getState();
                BlackjackHand dealer = state.getDealerHand();

                if (shouldDealerHit(dealer)) {
                    dealToDealer(gui);
                    if (dealer.isBust()) {
                        cancel();
                        resolveAgainstDealer(gui);
                    }
                    return;
                }

                cancel();
                resolveAgainstDealer(gui);
            }
        }.runTaskTimer(bukkitPlugin, 12L, 12L);
    }

    private void evaluateInitialState(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        BlackjackHand player = state.getCurrentHand();
        BlackjackHand dealer = state.getDealerHand();

        if (player.isBlackjack() || dealer.isBlackjack()) {
            state.setDealerHidden(false);
            gui.render();

            if (player.isBlackjack() && dealer.isBlackjack()) {
                finishRound(gui, "Push: both have blackjack");
            } else if (player.isBlackjack()) {
                finishRound(gui, "Blackjack");
            } else {
                finishRound(gui, "Dealer blackjack");
            }
            return;
        }

        state.setStatus("Choose Hit, Stand, or Split");
        gui.render();
    }

    private void resolveAgainstDealer(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        finishRound(gui, state.getDealerHand().isBust() ? "Dealer bust" : "Dealer stands");
    }

    private void finishRound(BlackjackGUI gui, String resultLabel) {
        BlackjackTableState state = gui.getState();
        state.setDealerHidden(false);
        state.setPhase(BlackjackTableState.Phase.ROUND_OVER);
        state.setStatus(resultLabel);

        BlackjackHand dealer = state.getDealerHand();
        int dealerScore = dealer.getBestValue();
        int wins = 0;
        int losses = 0;
        int pushes = 0;
        double totalPayout = 0.0;
        StringBuilder summary = new StringBuilder();

        for (int i = 0; i < state.getPlayerHands().size(); i++) {
            BlackjackHand hand = state.getPlayerHands().get(i);
            double handBet = state.getBetForHand(i);
            double handPayout = resolveHandPayout(hand, dealer, handBet);
            totalPayout += handPayout;

            String outcome;
            if (handPayout == 0.0) {
                outcome = "Loss";
                losses++;
            } else if (Double.compare(handPayout, handBet) == 0) {
                outcome = "Push";
                pushes++;
            } else {
                outcome = hand.isBlackjack() && !dealer.isBlackjack() ? "Blackjack" : "Win";
                wins++;
            }

            summary.append("\n<gray>Hand ").append(i + 1).append(":</gray> <white>")
                .append(hand.getBestValue()).append("</white> <gray>-</gray> <")
                .append(outcome.equals("Loss") ? "red" : outcome.equals("Push") ? "yellow" : "green")
                .append(">").append(outcome).append("</")
                .append(outcome.equals("Loss") ? "red" : outcome.equals("Push") ? "yellow" : "green")
                .append(">");
        }

        if (totalPayout > 0 && !payWinnings(gui.getPlayer(), totalPayout)) {
            plugin.getPlugin().getLogger().warning("Failed to pay blackjack winnings to " + gui.getPlayer().getName());
            totalPayout = 0.0;
            wins = 0;
            pushes = 0;
            losses = state.getPlayerHands().size();
        }

        if (wins > 0) {
            handleWin(gui.getPlayer(), state.getTotalCommittedBet(), totalPayout);
        } else if (losses > 0 && pushes == 0) {
            handleLoss(gui.getPlayer(), state.getTotalCommittedBet());
        }

        gui.render();
        gui.playResultSound(wins > 0 || pushes > 0);
        sendRoundMessage(gui.getPlayer(), state, dealerScore, totalPayout, summary);
        logGame(gui.getPlayer(), state.getTotalCommittedBet(), true);
    }

    private double resolveHandPayout(BlackjackHand hand, BlackjackHand dealer, double handBet) {
        int dealerScore = dealer.getBestValue();
        int playerScore = hand.getBestValue();

        if (hand.isBust()) {
            return 0.0;
        }
        if (dealer.isBust()) {
            return handBet * getWinPayoutMultiplier();
        }
        if (hand.isBlackjack() && !dealer.isBlackjack()) {
            return handBet * getBlackjackPayoutMultiplier();
        }
        if (dealer.isBlackjack() && !hand.isBlackjack()) {
            return 0.0;
        }
        if (playerScore > dealerScore) {
            return handBet * getWinPayoutMultiplier();
        }
        if (playerScore == dealerScore) {
            return handBet * getPushPayoutMultiplier();
        }
        return 0.0;
    }

    private void sendRoundMessage(Player player, BlackjackTableState state, int dealerScore, double payout, StringBuilder handSummary) {
        StringBuilder message = new StringBuilder();
        message.append(plugin.getLocaleManager().getText("blackjack.result-title")).append("\n");
        message.append(plugin.getLocaleManager().formatText("blackjack.dealer-hand", Map.of("score", String.valueOf(dealerScore)))).append("\n");
        message.append(plugin.getLocaleManager().formatText("blackjack.total-bet", Map.of("amount", plugin.getEconomyManager().format(state.getTotalCommittedBet()))));
        message.append(handSummary);

        if (payout > 0) {
            message.append("\n").append(plugin.getLocaleManager().formatText("blackjack.total-payout", Map.of("amount", plugin.getEconomyManager().format(payout))));
        }

        sendMessage(player, message.toString());
    }

    private void dealToPlayer(BlackjackGUI gui, BlackjackHand hand, String status) {
        hand.add(drawCard());
        gui.getState().setStatus(status);
        gui.playDealSound();
        gui.render();
    }

    private void dealToDealer(BlackjackGUI gui) {
        gui.getState().getDealerHand().add(drawCard());
        gui.getState().setStatus(gui.getState().getPhase() == BlackjackTableState.Phase.PLAYER_TURN
            ? "Dealer draws"
            : "Dealer hits");
        gui.playDealSound();
        gui.render();
    }

    private BlackjackCard drawCard() {
        CardRank[] ranks = CardRank.values();
        CardSuit[] suits = CardSuit.values();
        return new BlackjackCard(
            ranks[ThreadLocalRandom.current().nextInt(ranks.length)],
            suits[ThreadLocalRandom.current().nextInt(suits.length)]
        );
    }

    private boolean shouldDealerHit(BlackjackHand dealerHand) {
        int score = dealerHand.getBestValue();
        boolean hitSoft17 = plugin.getConfigManager().getConfig().getBoolean("games.blackjack.dealer.hit-soft-17", false);
        return score < 17 || (hitSoft17 && score == 17 && dealerHand.isSoft());
    }

    private double getBlackjackPayoutMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.blackjack.payouts.blackjack", 2.2);
    }

    private double getWinPayoutMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.blackjack.payouts.win", 1.9);
    }

    private double getPushPayoutMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.blackjack.payouts.push", 1.0);
    }

    @Override
    public boolean canPlay(Player player) {
        return !sessions.containsKey(player.getUniqueId()) && super.canPlay(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        sessions.clear();
    }
}
