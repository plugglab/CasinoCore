package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.CasinoGame;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AdminGamesGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 54;
    private static final int[] GAME_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int SLOT_ENABLE_ALL = 46;
    private static final int SLOT_RELOAD = 49;
    private static final int SLOT_DISABLE_ALL = 52;
    private static final int SLOT_BACK = 45;

    private static final Map<String, Material> GAME_MATERIALS = Map.ofEntries(
        Map.entry("coinflip", Material.SUNFLOWER),
        Map.entry("dice", Material.TARGET),
        Map.entry("blackjack", Material.PAPER),
        Map.entry("highlow", Material.REDSTONE_TORCH),
        Map.entry("ridethebus", Material.MINECART),
        Map.entry("doubleup", Material.BLAZE_POWDER),
        Map.entry("treasure", Material.CHEST),
        Map.entry("roulette", Material.CLOCK),
        Map.entry("slots", Material.DIAMOND),
        Map.entry("lottery", Material.EMERALD),
        Map.entry("horserace", Material.SADDLE),
        Map.entry("wheel", Material.NAUTILUS_SHELL)
    );

    private final CasinoPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public AdminGamesGUI(CasinoPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, Component.text("Casino Admin"));
        render();
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    public void handleClick(int slot) {
        if (slot == SLOT_BACK) {
            new CasinoHubGUI(plugin, player).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            return;
        }
        if (slot == SLOT_ENABLE_ALL) {
            updateAllGames(true);
            return;
        }
        if (slot == SLOT_RELOAD) {
            plugin.reloadPlugin();
            render();
            plugin.getMessageManager().send(player, "<green>CasinoCore configuration reloaded.</green>");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.15f);
            return;
        }
        if (slot == SLOT_DISABLE_ALL) {
            updateAllGames(false);
            return;
        }

        List<CasinoGame> games = getSortedGames();
        for (int i = 0; i < GAME_SLOTS.length && i < games.size(); i++) {
            if (GAME_SLOTS[i] != slot) {
                continue;
            }

            toggleGame(games.get(i));
            return;
        }
    }

    private void toggleGame(CasinoGame game) {
        boolean enabled = game.isEnabled();
        boolean updated = enabled
            ? plugin.getGameManager().disableCasinoGame(game.getName())
            : plugin.getGameManager().enableCasinoGame(game.getName());

        if (!updated) {
            plugin.getMessageManager().send(player, "<red>Could not update " + game.getDisplayName() + ".</red>");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
            return;
        }

        render();
        player.playSound(
            player.getLocation(),
            enabled ? Sound.BLOCK_REDSTONE_TORCH_BURNOUT : Sound.BLOCK_BEACON_ACTIVATE,
            0.8f,
            enabled ? 0.9f : 1.1f
        );
        plugin.getMessageManager().send(
            player,
            (enabled ? "<red>Disabled </red>" : "<green>Enabled </green>") +
                "<gold>" + game.getDisplayName() + "</gold>"
        );
    }

    private void render() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, item(
            Material.COMPARATOR,
            "<gold><bold>Game Controls</bold></gold>",
            "<gray>Click any game to enable or disable it.</gray>",
            "<gray>Changes are saved to config immediately.</gray>",
            "<gray>Use the bottom row for bulk actions.</gray>"
        ));

        List<CasinoGame> games = getSortedGames();
        for (int i = 0; i < GAME_SLOTS.length; i++) {
            if (i >= games.size()) {
                inventory.setItem(GAME_SLOTS[i], item(Material.BLACK_STAINED_GLASS_PANE, " "));
                continue;
            }

            CasinoGame game = games.get(i);
            inventory.setItem(GAME_SLOTS[i], createGameItem(game));
        }

        inventory.setItem(SLOT_BACK, item(Material.BARRIER, "<red>Back</red>", "<gray>Return to the casino hub.</gray>"));
        inventory.setItem(SLOT_ENABLE_ALL, item(Material.EMERALD_BLOCK, "<green>Enable All</green>", "<gray>Turn every game on.</gray>"));
        inventory.setItem(47, item(Material.BOOK, "<gold>Enabled Summary</gold>",
            "<gray>Enabled:</gray> <white>" + plugin.getGameManager().getEnabledCasinoGames().size() + "</white>",
            "<gray>Total:</gray> <white>" + plugin.getGameManager().getAllCasinoGames().size() + "</white>"
        ));
        inventory.setItem(SLOT_RELOAD, item(Material.COMPASS, "<aqua>Reload</aqua>", "<gray>Reload config and locale data.</gray>"));
        inventory.setItem(51, item(Material.REDSTONE_TORCH, "<yellow>Debug</yellow>",
            "<gray>Current:</gray> " + (plugin.getConfigManager().isDebugEnabled() ? "<green>On</green>" : "<red>Off</red>"),
            "<gray>Toggle in config.yml and reload.</gray>"
        ));
        inventory.setItem(SLOT_DISABLE_ALL, item(Material.REDSTONE_BLOCK, "<red>Disable All</red>", "<gray>Turn every game off.</gray>"));
    }

    private void updateAllGames(boolean enabled) {
        int changed = 0;
        for (CasinoGame game : getSortedGames()) {
            boolean updated = enabled
                ? plugin.getGameManager().enableCasinoGame(game.getName())
                : plugin.getGameManager().disableCasinoGame(game.getName());
            if (updated) {
                changed++;
            }
        }

        render();
        player.playSound(player.getLocation(), enabled ? Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8f, enabled ? 1.1f : 0.9f);
        plugin.getMessageManager().send(player,
            (enabled ? "<green>Enabled </green>" : "<red>Disabled </red>") +
                "<gold>" + changed + "</gold><gray> game entries.</gray>");
    }

    private ItemStack createGameItem(CasinoGame game) {
        boolean enabled = game.isEnabled();
        Material material = enabled
            ? GAME_MATERIALS.getOrDefault(game.getName(), Material.LIME_CONCRETE)
            : Material.GRAY_DYE;

        return item(
            material,
            (enabled ? "<green>" : "<red>") + "<bold>" + game.getDisplayName() + "</bold>" + (enabled ? "</green>" : "</red>"),
            "<gray>" + game.getDescription() + "</gray>",
            "<gray>Key:</gray> <white>" + game.getName() + "</white>",
            "<gray>Status:</gray> " + (enabled ? "<green>Enabled</green>" : "<red>Disabled</red>"),
            "<gray>Permission:</gray> <white>" + game.getPermission() + "</white>",
            "<yellow>Click to " + (enabled ? "disable" : "enable") + "</yellow>"
        );
    }

    private List<CasinoGame> getSortedGames() {
        List<CasinoGame> games = new ArrayList<>(plugin.getGameManager().getAllCasinoGames().values());
        games.sort(Comparator.comparing(CasinoGame::getName));
        return games;
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
