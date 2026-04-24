package com.casinocore.games.slots;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public enum SlotSymbol {
    DIAMOND("Diamond", "<aqua><bold>Diamond</bold></aqua>", Material.DIAMOND, 1, "Jackpot Symbol"),
    EMERALD("Emerald", "<green><bold>Emerald</bold></green>", Material.EMERALD, 5, "High Value"),
    GOLD_INGOT("Gold", "<gold><bold>Gold</bold></gold>", Material.GOLD_INGOT, 8, "High Value"),
    IRON_INGOT("Iron", "<gray><bold>Iron</bold></gray>", Material.IRON_INGOT, 12, "Medium Value"),
    REDSTONE("Redstone", "<red><bold>Redstone</bold></red>", Material.REDSTONE, 15, "Medium Value"),
    LAPIS("Lapis", "<blue><bold>Lapis</bold></blue>", Material.LAPIS_LAZULI, 15, "Medium Value"),
    COAL("Coal", "<dark_gray><bold>Coal</bold></dark_gray>", Material.COAL, 20, "Low Value"),
    APPLE("Apple", "<red><bold>Apple</bold></red>", Material.APPLE, 24, "Low Value");

    private final String plainName;
    private final String displayName;
    private final Material material;
    private final int weight;
    private final String lore;

    SlotSymbol(String plainName, String displayName, Material material, int weight, String lore) {
        this.plainName = plainName;
        this.displayName = displayName;
        this.material = material;
        this.weight = weight;
        this.lore = lore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlainName() {
        return plainName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(plainName));
            meta.lore(List.of(Component.text(lore)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static SlotSymbol getRandomSymbol(Random random) {
        int totalWeight = 0;
        for (SlotSymbol symbol : values()) {
            totalWeight += symbol.weight;
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (SlotSymbol symbol : values()) {
            currentWeight += symbol.weight;
            if (randomValue < currentWeight) {
                return symbol;
            }
        }

        return COAL;
    }

    public static SlotSymbol fromMaterial(Material material) {
        for (SlotSymbol symbol : values()) {
            if (symbol.material == material) {
                return symbol;
            }
        }
        return null;
    }

    public static double calculateMultiplier(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return 0;
        }

        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            return switch (symbols[0]) {
                case DIAMOND -> 50.0;
                case EMERALD -> 25.0;
                case GOLD_INGOT -> 15.0;
                case IRON_INGOT -> 10.0;
                case REDSTONE, LAPIS -> 5.0;
                case COAL, APPLE -> 3.0;
            };
        }

        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            SlotSymbol matchingSymbol = symbols[0] == symbols[1] ? symbols[0]
                : (symbols[1] == symbols[2] ? symbols[1] : symbols[0]);

            return switch (matchingSymbol) {
                case DIAMOND -> 10.0;
                case EMERALD -> 5.0;
                case GOLD_INGOT -> 3.0;
                case IRON_INGOT, REDSTONE, LAPIS -> 2.0;
                case COAL, APPLE -> 1.5;
            };
        }

        return 0;
    }

    public static String getOutcomeDescription(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return "<red>Invalid</red>";
        }

        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            if (symbols[0] == DIAMOND) {
                return "<gold><bold>JACKPOT!</bold></gold>";
            }
            return "<green><bold>THREE OF A KIND!</bold></green>";
        }

        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            return "<yellow><bold>TWO MATCH!</bold></yellow>";
        }

        return "<red><bold>NO MATCH</bold></red>";
    }
}
