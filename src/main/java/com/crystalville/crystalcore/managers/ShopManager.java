package com.crystalville.crystalcore.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the item -> price list from config.yml's "shop" section and
 * answers price/availability lookups for /buy and /sell. Supports adding
 * or updating prices at runtime via /shop add, persisting changes back
 * to config.yml immediately.
 */
public class ShopManager {

    private final JavaPlugin plugin;
    private final Map<Material, Integer> prices = new LinkedHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadShop() {
        prices.clear();
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
            int price = section.getInt(key, -1);
            if (price <= 0) {
                plugin.getLogger().warning("Invalid price for " + key + " in config.yml, skipping.");
                continue;
            }
            prices.put(material, price);
        }
    }

    public boolean isForSale(Material material) {
        return prices.containsKey(material);
    }

    public int getPrice(Material material) {
        return prices.getOrDefault(material, -1);
    }

    public Map<Material, Integer> getAllPrices() {
        return prices;
    }

    /**
     * Adds a new shop item or updates the price of an existing one, and
     * immediately persists the change to config.yml.
     */
    public void setPrice(Material material, int price) {
        prices.put(material, price);
        plugin.getConfig().set("shop." + material.name(), price);
        plugin.saveConfig();
    }
          }
