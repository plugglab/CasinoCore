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
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> new CoinFlipGUI(plugin, this, player, bet).open());
        return true;
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    public boolean createOffer(Player creator, double bet) {
        boolean betWithdrawn = false;
        try {
            if (!preGameValidation(creator, bet)) {
                return false;
            }

            if (hasActiveOffer(creator.getUniqueId())) {
                sendLocaleMessage(creator, "coinflip.already-active");
                return false;
            }

            if (!withdrawBet(creator, bet)) {
                return false;
            }
            betWithdrawn = true;

            setCooldown(creator);

            CoinFlipOffer offer = new CoinFlipOffer(creator.getUniqueId(), creator.getName(), bet);
            offersByCreator.put(creator.getUniqueId(), offer);
            playerOfferIndex.put(creator.getUniqueId(), creator.getUniqueId());

            long expireTicks = getOfferExpirySeconds() * 20L;
            Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> expireOffer(offer.getCreatorId()), expireTicks);

            sendLocaleMessage(creator, "coinflip.created", Map.of(
                "bet", plugin.getEconomyManager().format(bet),
                "creator", creator.getName()
            ));
            return true;
        } catch (Exception e) {
            handleGameError(creator, bet, e, betWithdrawn);
            return false;
        }
    }

    public boolean joinOffer(Player joiner, String creatorName) {
        Player creator = Bukkit.getPlayerExact(creatorName);
        if (creator == null) {
            sendLocaleMessage(joiner, "coinflip.player-offline");
            return false;
        }

        CoinFlipOffer offer = offersByCreator.get(creator.getUniqueId());
        if (offer == null || offer.isLocked()) {
            sendLocaleMessage(joiner, "coinflip.offer-not-found");
            return false;
        }

        if (creator.getUniqueId().equals(joiner.getUniqueId())) {
            sendLocaleMessage(joiner, "coinflip.cannot-join-own");
            return false;
        }

        if (hasActiveOffer(joiner.getUniqueId())) {
            sendLocaleMessage(joiner, "coinflip.pending-offer");
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
                sendLocaleMessage(joiner, "coinflip.no-longer-available");
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
            sendLocaleMessage(creator, "coinflip.no-active-offer");
            return false;
        }

        synchronized (offer) {
            if (offer.isLocked()) {
                sendLocaleMessage(creator, "coinflip.already-resolving");
                return false;
            }

            offer.setResolved(true);
            clearOfferIndexes(offer);
        }

        payWinnings(creator, offer.getBet());
        sendLocaleMessage(creator, "coinflip.cancelled");
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
            sendLocaleMessage(viewer, "coinflip.no-open-offers");
            return;
        }

        StringBuilder message = new StringBuilder(plugin.getLocaleManager().getText("coinflip.open-title"));
        for (CoinFlipOffer offer : openOffers) {
            message.append("\n").append(plugin.getLocaleManager().formatText("coinflip.open-entry", Map.of(
                "creator", offer.getCreatorName(),
                "bet", plugin.getEconomyManager().format(offer.getBet())
            )));
        }
        sendMessage(viewer, message.toString());
    }

    public List<CoinFlipOfferView> getOpenOfferViews() {
        List<CoinFlipOfferView> openOffers = new ArrayList<>();
        for (CoinFlipOffer offer : offersByCreator.values()) {
            if (!offer.isLocked() && !offer.isResolved()) {
                openOffers.add(new CoinFlipOfferView(offer.getCreatorId(), offer.getCreatorName(), offer.getBet()));
            }
        }
        openOffers.sort((left, right) -> left.creatorName().compareToIgnoreCase(right.creatorName()));
        return openOffers;
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
                    sendLocaleMessage(creator, "coinflip.refunded-player-left");
                }
                if (joiner != null) {
                    payWinnings(joiner, offer.getBet());
                    sendLocaleMessage(joiner, "coinflip.refunded-player-left");
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
                sendLocaleMessage(creator, "coinflip.refunded-opponent-unavailable");
            }
            if (joiner != null) {
                payWinnings(joiner, offer.getBet());
                sendLocaleMessage(joiner, "coinflip.refunded-opponent-unavailable");
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
            sendLocaleMessage(creator, "coinflip.payout-failed");
            sendLocaleMessage(joiner, "coinflip.payout-failed");
            clearOfferIndexes(offer);
            return;
        }

        handleWin(winner, offer.getBet(), payout);
        handleLoss(loser, offer.getBet());

        String result = creatorWins
            ? plugin.getLocaleManager().getText("coinflip.result.heads")
            : plugin.getLocaleManager().getText("coinflip.result.tails");
        String summary = plugin.getLocaleManager().formatText("coinflip.summary", Map.of(
            "creator", creator.getName(),
            "joiner", joiner.getName(),
            "flip", result,
            "winner", winner.getName(),
            "pot", plugin.getEconomyManager().format(payout)
        ));

        sendMessage(creator, summary);
        sendMessage(joiner, summary);
        sendLocaleMessage(loser, "coinflip.lost", Map.of("amount", plugin.getEconomyManager().format(offer.getBet())));
        plugin.getMessageManager().broadcast(
            plugin.getLocaleManager().formatText("coinflip.broadcast-win", Map.of(
                "winner", winner.getName(),
                "loser", loser.getName(),
                "amount", plugin.getEconomyManager().format(payout)
            ))
        );
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
            sendLocaleMessage(creator, "coinflip.expired");
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

    public record CoinFlipOfferView(UUID creatorId, String creatorName, double bet) {
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
