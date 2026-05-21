package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerHeadFactory {

    private PlayerHeadFactory() {
    }

    public static ItemStack createPlayerHead(CasinoPlugin plugin, UUID ownerId, String fallbackName, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        OfflinePlayer owner = ownerId != null ? Bukkit.getOfflinePlayer(ownerId) : null;
        if (owner != null) {
            meta.setOwningPlayer(owner);
        } else if (fallbackName != null && !fallbackName.isBlank()) {
            OfflinePlayer fallback = Bukkit.getOfflinePlayer(fallbackName);
            if (fallback.hasPlayedBefore() || fallback.isOnline()) {
                meta.setOwningPlayer(fallback);
            }
        }

        meta.displayName(plugin.getMessageManager().parse(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(plugin.getMessageManager().parse(line));
        }
        meta.lore(lines);
        hideTechnicalTooltip(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCustomHead(CasinoPlugin plugin, String textureValue, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        if (textureValue != null && !textureValue.isBlank()) {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(textureValue.getBytes()), null);
            profile.setProperty(new ProfileProperty("textures", textureValue));
            meta.setPlayerProfile(profile);
        }

        meta.displayName(plugin.getMessageManager().parse(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(plugin.getMessageManager().parse(line));
        }
        meta.lore(lines);
        hideTechnicalTooltip(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPlayerHead(UUID ownerId, String fallbackName, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        OfflinePlayer owner = ownerId != null ? Bukkit.getOfflinePlayer(ownerId) : null;
        if (owner != null) {
            meta.setOwningPlayer(owner);
        } else if (fallbackName != null && !fallbackName.isBlank()) {
            OfflinePlayer fallback = Bukkit.getOfflinePlayer(fallbackName);
            if (fallback.hasPlayedBefore() || fallback.isOnline()) {
                meta.setOwningPlayer(fallback);
            }
        }

        meta.displayName(Component.text(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Component.text(line));
        }
        meta.lore(lines);
        hideTechnicalTooltip(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static void hideTechnicalTooltip(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
    }
}
