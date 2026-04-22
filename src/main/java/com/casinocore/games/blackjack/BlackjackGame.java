package com.casinocore.games.blackjack;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

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
        try {
            if (!preGameValidation(player, bet)) {
                return false;
            }

            if (sessions.containsKey(player.getUniqueId())) {
                sendMessage(player, "<yellow>You already have a blackjack table open.</yellow>");
                return false;
            }

            if (!withdrawBet(player, bet)) {
                return false;
            }

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
            handleGameError(player, bet, e);
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
        if (slot == 48 && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN) {
            hit(gui);
            return;
        }

        if (slot == 50 && state.getPhase() == BlackjackTableState.Phase.PLAYER_TURN) {
            stand(gui);
            return;
        }

        if (slot == 49 && state.getPhase() == BlackjackTableState.Phase.ROUND_OVER) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
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
                    case 0 -> dealToPlayer(gui);
                    case 1 -> dealToDealer(gui);
                    case 2 -> dealToPlayer(gui);
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
        dealToPlayer(gui);

        if (state.getPlayerHand().isBust()) {
            finishRound(gui, "Bust", 0.0, false);
        } else if (state.getPlayerHand().getBestValue() == 21) {
            stand(gui);
        } else {
            state.setStatus("Choose Hit or Stand");
            gui.render();
        }
    }

    private void stand(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        state.setPhase(BlackjackTableState.Phase.DEALER_TURN);
        state.setDealerHidden(false);
        state.setStatus("Dealer turn...");
        gui.playStandSound();
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
                        finishRound(gui, "Dealer bust", getWinPayoutMultiplier(), true);
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
        BlackjackHand player = state.getPlayerHand();
        BlackjackHand dealer = state.getDealerHand();

        if (player.isBlackjack() || dealer.isBlackjack()) {
            state.setDealerHidden(false);
            gui.render();

            if (player.isBlackjack() && dealer.isBlackjack()) {
                finishRound(gui, "Push: both have blackjack", getPushPayoutMultiplier(), false);
            } else if (player.isBlackjack()) {
                finishRound(gui, "Blackjack", getBlackjackPayoutMultiplier(), true);
            } else {
                finishRound(gui, "Dealer blackjack", 0.0, false);
            }
            return;
        }

        state.setStatus("Choose Hit or Stand");
        gui.render();
    }

    private void resolveAgainstDealer(BlackjackGUI gui) {
        BlackjackTableState state = gui.getState();
        int playerScore = state.getPlayerHand().getBestValue();
        int dealerScore = state.getDealerHand().getBestValue();

        if (dealerScore > 21) {
            finishRound(gui, "Dealer bust", getWinPayoutMultiplier(), true);
        } else if (playerScore > dealerScore) {
            finishRound(gui, "Player wins", getWinPayoutMultiplier(), true);
        } else if (playerScore == dealerScore) {
            finishRound(gui, "Push", getPushPayoutMultiplier(), false);
        } else {
            finishRound(gui, "Dealer wins", 0.0, false);
        }
    }

    private void finishRound(BlackjackGUI gui, String result, double payoutMultiplier, boolean winSound) {
        BlackjackTableState state = gui.getState();
        state.setDealerHidden(false);
        state.setPhase(BlackjackTableState.Phase.ROUND_OVER);
        state.setStatus(result);

        double payout = state.getBet() * payoutMultiplier;
        if (payout > 0 && !payWinnings(gui.getPlayer(), payout)) {
            plugin.getPlugin().getLogger().warning("Failed to pay blackjack winnings to " + gui.getPlayer().getName());
            payout = 0.0;
        }

        if (payoutMultiplier > getPushPayoutMultiplier()) {
            handleWin(gui.getPlayer(), state.getBet(), payout);
        } else if (payout <= 0) {
            handleLoss(gui.getPlayer(), state.getBet());
        }

        gui.render();
        gui.playResultSound(winSound || payoutMultiplier == getPushPayoutMultiplier());
        sendRoundMessage(gui.getPlayer(), state, result, payoutMultiplier, payout);
        logGame(gui.getPlayer(), state.getBet(), true);
    }

    private void sendRoundMessage(Player player, BlackjackTableState state, String result,
                                  double payoutMultiplier, double payout) {
        BlackjackHand playerHand = state.getPlayerHand();
        BlackjackHand dealerHand = state.getDealerHand();

        StringBuilder message = new StringBuilder();
        message.append("<gold><bold>Blackjack</bold></gold>\n");
        message.append("<gray>Result:</gray> <white>").append(result).append("</white>\n");
        message.append("<gray>Your Hand:</gray> <white>").append(playerHand.getBestValue()).append("</white>\n");
        message.append("<gray>Dealer Hand:</gray> <white>").append(dealerHand.getBestValue()).append("</white>\n");
        message.append("<gray>Bet:</gray> <white>").append(plugin.getEconomyManager().format(state.getBet())).append("</white>");

        if (payout > 0) {
            message.append("\n<gray>Payout:</gray> <gold>").append(plugin.getEconomyManager().format(payout)).append("</gold>");
            message.append("\n<gray>Multiplier:</gray> <gold>").append(payoutMultiplier).append("x</gold>");
        }

        sendMessage(player, message.toString());
    }

    private void dealToPlayer(BlackjackGUI gui) {
        gui.getState().getPlayerHand().add(drawCard());
        gui.getState().setStatus("Player draws");
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
        return plugin.getConfigManager().getConfig().getDouble("games.blackjack.payouts.blackjack", 2.5);
    }

    private double getWinPayoutMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.blackjack.payouts.win", 2.0);
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
