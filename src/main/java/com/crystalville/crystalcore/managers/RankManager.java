package com.crystalville.crystalcore.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {

    private final JavaPlugin plugin;
    private final File ranksFile;
    private FileConfiguration ranksConfig;

    private final Map<UUID, String> rankNames = new HashMap<>();
    private final Map<UUID, String> rankColors = new HashMap<>();

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
    }

    public void loadRanks() {
        if (!ranksFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                ranksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create ranks.yml: " + e.getMessage());
            }
        }

        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

        rankNames.clear();
        rankColors.clear();

        if (ranksConfig.getConfigurationSection("ranks") == null) {
            return;
        }

        for (String uuidStr : ranksConfig.getConfigurationSection("ranks").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = ranksConfig.getString("ranks." + uuidStr + ".name", "");
                String color = ranksConfig.getString("ranks." + uuidStr + ".color", "WHITE");
                rankNames.put(uuid, name);
                rankColors.put(uuid, color);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveRanks() {
        if (ranksConfig == null) {
            ranksConfig = new YamlConfiguration();
        }

        for (Map.Entry<UUID, String> entry : rankNames.entrySet()) {
            UUID uuid = entry.getKey();
            ranksConfig.set("ranks." + uuid + ".name", entry.getValue());
            ranksConfig.set("ranks." + uuid + ".color", rankColors.getOrDefault(uuid, "WHITE"));
        }

        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ranks.yml: " + e.getMessage());
        }
    }

    public void setRank(OfflinePlayer target, String rankName, String colorInput) {
        UUID uuid = target.getUniqueId();
        rankNames.put(uuid, rankName);
        rankColors.put(uuid, colorInput.toUpperCase());
        saveRanks();
    }

    public boolean hasRank(UUID uuid) {
        return rankNames.containsKey(uuid) && rankNames.get(uuid) != null && !rankNames.get(uuid).isEmpty();
    }

    public String getRankName(UUID uuid) {
        return rankNames.getOrDefault(uuid, "");
    }

    public TextColor resolveColor(UUID uuid) {
        String colorInput = rankColors.getOrDefault(uuid, "WHITE");
        return parseColor(colorInput);
    }

    public static TextColor parseColor(String colorInput) {
        if (colorInput == null || colorInput.isEmpty()) {
            return NamedTextColor.WHITE;
        }

        String trimmed = colorInput.trim();

        String hex = trimmed.startsWith("#") ? trimmed : "#" + trimmed;
        if (hex.matches("#[0-9A-Fa-f]{6}")) {
            TextColor color = TextColor.fromHexString(hex);
            if (color != null) {
                return color;
            }
        }

        String key = trimmed.toLowerCase().replace(' ', '_');
        NamedTextColor named = NamedTextColor.NAMES.value(key);
        if (named != null) {
            return named;
        }

        return NamedTextColor.WHITE;
    }

    public Component getFormattedPrefixComponent(UUID uuid) {
        if (!hasRank(uuid)) {
            return Component.empty();
        }
        TextColor color = resolveColor(uuid);
        String name = getRankName(uuid);
        return Component.text("[" + name + "] ", color);
    }
}
