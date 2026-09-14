package com.crystalville.crystalcore.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the item -> {buy price, sell price} list from config.yml's "shop"
 * section. Buy price = what a player pays to purchase from the server.
 * Sell price = what a player receives for selling to the server. Buy is
 * intentionally kept higher than sell to maintain a healthy economy.
 */
public class ShopManager {

    /** One shop item's two prices. */
    public static final class ShopEntry {
        public final int buyPrice;
        public final int sellPrice;

        public ShopEntry(int buyPrice, int sellPrice) {
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }

    private final JavaPlugin plugin;
    private final Map<Material, ShopEntry> entries = new LinkedHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadShop() {
        entries.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("Unknown shop material in config.yml: " + key);
                continue;
            }

            ConfigurationSection itemSection = section.getConfigurationSection(key);
            int buyPrice;
            int sellPrice;

            if (itemSection != null) {
                buyPrice = itemSection.getInt("buy", -1);
                sellPrice = itemSection.getInt("sell", -1);
            } else {
                // Backward compatibility: a bare number means the same price for buy and sell.
                int flatPrice = section.getInt(key, -1);
                buyPrice = flatPrice;
                sellPrice = flatPrice;
            }

            if (buyPrice <= 0 || sellPrice <= 0) {
                plugin.getLogger().warning("Invalid buy/sell price for " + key + " in config.yml, skipping.");
                continue;
            }

            entries.put(material, new ShopEntry(buyPrice, sellPrice));
        }
    }

    public boolean isForSale(Material material) {
        return entries.containsKey(material);
    }

    public int getBuyPrice(Material material) {
        ShopEntry entry = entries.get(material);
        return entry != null ? entry.buyPrice : -1;
    }

    public int getSellPrice(Material material) {
        ShopEntry entry = entries.get(material);
        return entry != null ? entry.sellPrice : -1;
    }

    public Map<Material, ShopEntry> getAllEntries() {
        return entries;
    }

    /**
     * Adds a new shop item or updates an existing one's buy/sell prices,
     * and immediately persists the change to config.yml.
     */
    public void setPrice(Material material, int buyPrice, int sellPrice) {
        entries.put(material, new ShopEntry(buyPrice, sellPrice));
        plugin.getConfig().set("shop." + material.name() + ".buy", buyPrice);
        plugin.getConfig().set("shop." + material.name() + ".sell", sellPrice);
        plugin.saveConfig();
    }
}
