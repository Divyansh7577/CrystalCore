package com.crystalville.crystalcore.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anti-theft chest logging. Records every net change in a chest/barrel/
 * shulker box's contents (who changed it, what was added or removed, and
 * when), keyed by the container's world location. Used by /i (inspect mode)
 * to show a full history of a chest without opening it.
 */
public class ChestLogManager {

    private static final int MAX_ENTRIES_PER_CONTAINER = 30;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd/MM HH:mm:ss");

    private final JavaPlugin plugin;
    private final File logFile;
    private FileConfiguration logConfig;

    private final Map<String, List<String>> logs = new HashMap<>();

    public ChestLogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "chestlogs.yml");
    }

    public void load() {
        if (!logFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create chestlogs.yml: " + e.getMessage());
            }
        }

        logConfig = YamlConfiguration.loadConfiguration(logFile);
        logs.clear();

        if (logConfig.getConfigurationSection("logs") == null) {
            return;
        }

        for (String key : logConfig.getConfigurationSection("logs").getKeys(false)) {
            List<String> entries = logConfig.getStringList("logs." + key);
            logs.put(key, new ArrayList<>(entries));
        }
    }

    public void save() {
        if (logConfig == null) {
            logConfig = new YamlConfiguration();
        }

        logConfig.set("logs", null);
        for (Map.Entry<String, List<String>> entry : logs.entrySet()) {
            logConfig.set("logs." + entry.getKey(), entry.getValue());
        }

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save chestlogs.yml: " + e.getMessage());
        }
    }

    /** Resolves a stable location key for a chest-like container, or null if not applicable. */
    public static String resolveKey(InventoryHolder holder) {
        Location loc = resolveLocation(holder);
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static Location resolveLocation(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return ((DoubleChest) holder).getLocation();
        }
        if (holder instanceof Chest || holder instanceof Barrel || holder instanceof ShulkerBox) {
            return ((BlockState) holder).getLocation();
        }
        return null;
    }

    /** Records the net difference between two content snapshots of the same container. */
    public void recordDiff(String locationKey, String playerName, ItemStack[] before, ItemStack[] after) {
        Map<Material, Integer> beforeCounts = countMaterials(before);
        Map<Material, Integer> afterCounts = countMaterials(after);

        Set<Material> allMaterials = new HashSet<>();
        allMaterials.addAll(beforeCounts.keySet());
        allMaterials.addAll(afterCounts.keySet());

        List<String> newEntries = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Material material : allMaterials) {
            int b = beforeCounts.getOrDefault(material, 0);
            int a = afterCounts.getOrDefault(material, 0);
            int delta = a - b;

            if (delta > 0) {
                newEntries.add(now + "|" + playerName + "|ADD|" + material.name() + "|" + delta);
            } else if (delta < 0) {
                newEntries.add(now + "|" + playerName + "|REMOVE|" + material.name() + "|" + (-delta));
            }
        }

        if (newEntries.isEmpty()) {
            return;
        }

        List<String> existing = logs.computeIfAbsent(locationKey, k -> new ArrayList<>());
        existing.addAll(newEntries);

        while (existing.size() > MAX_ENTRIES_PER_CONTAINER) {
            existing.remove(0);
        }

        save();
    }

    private Map<Material, Integer> countMaterials(ItemStack[] contents) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    /** Sends a formatted log history for the given chest to the inspecting player. */
    public void sendLogsTo(Player viewer, String locationKey) {
        List<String> entries = logs.get(locationKey);

        viewer.sendMessage(Component.text("=== Chest Log: " + locationKey + " ===", NamedTextColor.AQUA));

        if (entries == null || entries.isEmpty()) {
            viewer.sendMessage(Component.text("No recorded activity for this chest yet.", NamedTextColor.GRAY));
            return;
        }

        for (int i = entries.size() - 1; i >= 0; i--) {
            String formatted = formatEntry(entries.get(i));
            if (formatted != null) {
                viewer.sendMessage(Component.text(formatted, NamedTextColor.GRAY));
            }
        }
    }

    private String formatEntry(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 5) {
            return null;
        }

        try {
            long timestamp = Long.parseLong(parts[0]);
            String playerName = parts[1];
            String action = parts[2];
            String materialName = parts[3];
            String amount = parts[4];

            String time = TIME_FORMAT.format(new Date(timestamp));
            String verb = action.equals("ADD") ? "added" : "removed";
            String pretty = prettyName(materialName);

            return "[" + time + "] " + playerName + " " + verb + " " + amount + "x " + pretty;
        } catch (Exception e) {
            return null;
        }
    }

    private String prettyName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
  }
