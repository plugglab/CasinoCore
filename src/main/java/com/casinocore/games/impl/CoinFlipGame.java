package com.casinocore.games.impl;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CoinFlipGame extends BaseCasinoGame {

    private final Map<UUID, CoinFlipOffer> offersByCreator;
    private final Map<UUID, UUID> playerOfferIndex;

    public CoinFlipGame(CasinoPlugin plugin) {
        super(plugin, "coinflip", "Coin Flip", "Create a wager and let another player join for a 50/50 flip.");
        this.offersByCreator = new ConcurrentHashMap<>();
        this.playerOfferIndex = new ConcurrentHashMap<>();
    }

    @Override
    public boolean play(Player player, double bet) {
        return createOffer(player, bet);
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    public boolean createOffer(Player creator, double bet) {
        try {
            if (!preGameValidation(creator, bet)) {
                return false;
            }

            if (hasActiveOffer(creator.getUniqueId())) {
                sendMessage(creator, "<yellow>You already have an active coinflip offer.</yellow>");
                return false;
            }

            if (!withdrawBet(creator, bet)) {
                return false;
            }

            setCooldown(creator);

            CoinFlipOffer offer = new CoinFlipOffer(creator.getUniqueId(), creator.getName(), bet);
            offersByCreator.put(creator.getUniqueId(), offer);
            playerOfferIndex.put(creator.getUniqueId(), creator.getUniqueId());

            long expireTicks = getOfferExpirySeconds() * 20L;
            Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> expireOffer(offer.getCreatorId()), expireTicks);

            sendMessage(creator,
                "<gold><bold>Coinflip Created</bold></gold>\n" +
                "<gray>Bet:</gray> <white>" + plugin.getEconomyManager().format(bet) + "</white>\n" +
                "<gray>Join:</gray> <white>/play coinflip join " + creator.getName() + "</white>"
            );
            return true;
        } catch (Exception e) {
            handleGameError(creator, bet, e);
            return false;
        }
    }

    public boolean joinOffer(Player joiner, String creatorName) {
        Player creator = Bukkit.getPlayerExact(creatorName);
        if (creator == null) {
            sendMessage(joiner, "<red>That player is not online.</red>");
            return false;
        }

        CoinFlipOffer offer = offersByCreator.get(creator.getUniqueId());
        if (offer == null || offer.isLocked()) {
            sendMessage(joiner, "<red>No joinable coinflip offer found for that player.</red>");
            return false;
        }

        if (creator.getUniqueId().equals(joiner.getUniqueId())) {
            sendMessage(joiner, "<red>You cannot join your own coinflip.</red>");
            return false;
        }

        if (hasActiveOffer(joiner.getUniqueId())) {
            sendMessage(joiner, "<yellow>You already have a pending coinflip offer.</yellow>");
            return false;
        }

        if (!canPlay(joiner)) {
            return false;
        }

        if (!validateBet(joiner, offer.getBet()) || !checkBalance(joiner, offer.getBet())) {
            return false;
        }

        synchronized (offer) {
            if (offer.isLocked() || offer.isResolved()) {
                sendMessage(joiner, "<red>That coinflip is no longer available.</red>");
                return false;
            }

            if (!withdrawBet(joiner, offer.getBet())) {
                return false;
            }

            offer.lock(joiner.getUniqueId(), joiner.getName());
            playerOfferIndex.put(joiner.getUniqueId(), offer.getCreatorId());
        }

        resolveOffer(offer);
        return true;
    }

    public boolean cancelOffer(Player creator) {
        CoinFlipOffer offer = offersByCreator.get(creator.getUniqueId());
        if (offer == null) {
            sendMessage(creator, "<red>You do not have an active coinflip offer.</red>");
            return false;
        }

        synchronized (offer) {
            if (offer.isLocked()) {
                sendMessage(creator, "<red>This coinflip is already being resolved.</red>");
                return false;
            }

            offer.setResolved(true);
            clearOfferIndexes(offer);
        }

        payWinnings(creator, offer.getBet());
        sendMessage(creator, "<yellow>Your coinflip offer was cancelled and refunded.</yellow>");
        return true;
    }

    public void showOpenOffers(Player viewer) {
        List<CoinFlipOffer> openOffers = new ArrayList<>();
        for (CoinFlipOffer offer : offersByCreator.values()) {
            if (!offer.isLocked() && !offer.isResolved()) {
                openOffers.add(offer);
            }
        }

        if (openOffers.isEmpty()) {
            sendMessage(viewer, "<yellow>No open coinflip offers right now.</yellow>");
            return;
        }

        StringBuilder message = new StringBuilder("<gold><bold>Open Coinflips</bold></gold>");
        for (CoinFlipOffer offer : openOffers) {
            message.append("\n<gray>- </gray><white>")
                .append(offer.getCreatorName())
                .append("</white> <gray>for</gray> <white>")
                .append(plugin.getEconomyManager().format(offer.getBet()))
                .append("</white> <gray>(/play coinflip join ")
                .append(offer.getCreatorName())
                .append(")</gray>");
        }
        sendMessage(viewer, message.toString());
    }

    public void handleQuit(Player player) {
        UUID ownerId = playerOfferIndex.get(player.getUniqueId());
        if (ownerId == null) {
            return;
        }

        CoinFlipOffer offer = offersByCreator.get(ownerId);
        if (offer == null) {
            playerOfferIndex.remove(player.getUniqueId());
            return;
        }

        synchronized (offer) {
            if (offer.isResolved()) {
                clearOfferIndexes(offer);
                return;
            }

            if (player.getUniqueId().equals(offer.getCreatorId()) && !offer.isLocked()) {
                offer.setResolved(true);
                clearOfferIndexes(offer);
                payWinnings(player, offer.getBet());
                return;
            }

            if (offer.isLocked() && offer.getJoinerId() != null) {
                offer.setResolved(true);
                clearOfferIndexes(offer);

                Player creator = Bukkit.getPlayer(offer.getCreatorId());
                Player joiner = Bukkit.getPlayer(offer.getJoinerId());

                if (creator != null) {
                    payWinnings(creator, offer.getBet());
                    sendMessage(creator, "<yellow>Coinflip refunded because a player left.</yellow>");
                }
                if (joiner != null) {
                    payWinnings(joiner, offer.getBet());
                    sendMessage(joiner, "<yellow>Coinflip refunded because a player left.</yellow>");
                }
            }
        }
    }

    private void resolveOffer(CoinFlipOffer offer) {
        offer.setResolved(true);

        Player creator = Bukkit.getPlayer(offer.getCreatorId());
        Player joiner = offer.getJoinerId() == null ? null : Bukkit.getPlayer(offer.getJoinerId());

        if (creator == null || joiner == null) {
            if (creator != null) {
                payWinnings(creator, offer.getBet());
                sendMessage(creator, "<yellow>Coinflip refunded because the opponent was unavailable.</yellow>");
            }
            if (joiner != null) {
                payWinnings(joiner, offer.getBet());
                sendMessage(joiner, "<yellow>Coinflip refunded because the opponent was unavailable.</yellow>");
            }
            clearOfferIndexes(offer);
            return;
        }

        boolean creatorWins = ThreadLocalRandom.current().nextBoolean();
        Player winner = creatorWins ? creator : joiner;
        Player loser = creatorWins ? joiner : creator;
        double payout = offer.getBet() * getWinnerPayoutMultiplier();

        boolean paid = payWinnings(winner, payout);
        if (!paid) {
            payWinnings(creator, offer.getBet());
            payWinnings(joiner, offer.getBet());
            sendMessage(creator, "<red>Coinflip payout failed. Both bets were refunded.</red>");
            sendMessage(joiner, "<red>Coinflip payout failed. Both bets were refunded.</red>");
            clearOfferIndexes(offer);
            return;
        }

        handleWin(winner, offer.getBet(), payout);
        handleLoss(loser, offer.getBet());

        String result = creatorWins ? "HEADS" : "TAILS";
        String summary =
            "<gold><bold>Coinflip Result</bold></gold>\n" +
            "<gray>Creator:</gray> <white>" + creator.getName() + "</white>\n" +
            "<gray>Joiner:</gray> <white>" + joiner.getName() + "</white>\n" +
            "<gray>Flip:</gray> <white>" + result + "</white>\n" +
            "<gray>Winner:</gray> <green>" + winner.getName() + "</green>\n" +
            "<gray>Pot:</gray> <gold>" + plugin.getEconomyManager().format(payout) + "</gold>";

        sendMessage(creator, summary);
        sendMessage(joiner, summary);
        sendMessage(loser, "<red>You lost " + plugin.getEconomyManager().format(offer.getBet()) + "</red>");
        clearOfferIndexes(offer);
        logGame(creator, offer.getBet(), true);
        logGame(joiner, offer.getBet(), true);
    }

    private void expireOffer(UUID creatorId) {
        CoinFlipOffer offer = offersByCreator.get(creatorId);
        if (offer == null) {
            return;
        }

        synchronized (offer) {
            if (offer.isResolved() || offer.isLocked()) {
                return;
            }

            offer.setResolved(true);
            clearOfferIndexes(offer);
        }

        Player creator = Bukkit.getPlayer(creatorId);
        if (creator != null) {
            payWinnings(creator, offer.getBet());
            sendMessage(creator, "<yellow>Your coinflip expired and was refunded.</yellow>");
        }
    }

    private boolean hasActiveOffer(UUID playerId) {
        return playerOfferIndex.containsKey(playerId);
    }

    private void clearOfferIndexes(CoinFlipOffer offer) {
        offersByCreator.remove(offer.getCreatorId());
        playerOfferIndex.remove(offer.getCreatorId());
        if (offer.getJoinerId() != null) {
            playerOfferIndex.remove(offer.getJoinerId());
        }
    }

    private int getOfferExpirySeconds() {
        return plugin.getConfigManager().getConfig().getInt("games.coinflip.offer-expiry-seconds", 60);
    }

    private double getWinnerPayoutMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.coinflip.multipliers.winner-payout", 1.9);
    }

    @Override
    public boolean canPlay(Player player) {
        return !hasActiveOffer(player.getUniqueId()) && super.canPlay(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (CoinFlipOffer offer : new ArrayList<>(offersByCreator.values())) {
            Player creator = Bukkit.getPlayer(offer.getCreatorId());
            if (creator != null) {
                payWinnings(creator, offer.getBet());
            }
            if (offer.getJoinerId() != null) {
                Player joiner = Bukkit.getPlayer(offer.getJoinerId());
                if (joiner != null) {
                    payWinnings(joiner, offer.getBet());
                }
            }
        }
        offersByCreator.clear();
        playerOfferIndex.clear();
    }

    private static final class CoinFlipOffer {
        private final UUID creatorId;
        private final String creatorName;
        private final double bet;
        private UUID joinerId;
        private String joinerName;
        private boolean locked;
        private boolean resolved;

        private CoinFlipOffer(UUID creatorId, String creatorName, double bet) {
            this.creatorId = creatorId;
            this.creatorName = creatorName;
            this.bet = bet;
        }

        public UUID getCreatorId() {
            return creatorId;
        }

        public String getCreatorName() {
            return creatorName;
        }

        public double getBet() {
            return bet;
        }

        public UUID getJoinerId() {
            return joinerId;
        }

        public boolean isLocked() {
            return locked;
        }

        public boolean isResolved() {
            return resolved;
        }

        public void setResolved(boolean resolved) {
            this.resolved = resolved;
        }

        public void lock(UUID joinerId, String joinerName) {
            this.joinerId = joinerId;
            this.joinerName = joinerName;
            this.locked = true;
        }
    }
}
