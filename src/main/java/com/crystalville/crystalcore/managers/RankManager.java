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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and applies per-player rank name + color, and pre-built server "roles"
 * (e.g. Market Minister, Finance Minister) which grant special command permissions.
 */
public class RankManager {

    public static final class RoleDefinition {
        public final String key;
        public final String displayName;
        public final String colorHex;

        RoleDefinition(String key, String displayName, String colorHex) {
            this.key = key;
            this.displayName = displayName;
            this.colorHex = colorHex;
        }
    }

    private static final Map<String, RoleDefinition> PREDEFINED_ROLES = new LinkedHashMap<>();

    static {
        PREDEFINED_ROLES.put("MARKET_MINISTER",
                new RoleDefinition("MARKET_MINISTER", "Market Minister", "#20B2AA"));
        PREDEFINED_ROLES.put("FINANCE_MINISTER",
                new RoleDefinition("FINANCE_MINISTER", "Finance Minister", "#FFD700"));
    }

    public static RoleDefinition findRoleByName(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        String normalized = input.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return PREDEFINED_ROLES.get(normalized);
    }

    private final JavaPlugin plugin;
    private final File ranksFile;
    private FileConfiguration ranksConfig;

    private final Map<UUID, String> rankNames = new HashMap<>();
    private final Map<UUID, String> rankColors = new HashMap<>();
    private final Map<UUID, String> playerRoleKeys = new HashMap<>();

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
        playerRoleKeys.clear();

        if (ranksConfig.getConfigurationSection("ranks") == null) {
            return;
        }

        for (String uuidStr : ranksConfig.getConfigurationSection("ranks").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = ranksConfig.getString("ranks." + uuidStr + ".name", "");
                String color = ranksConfig.getString("ranks." + uuidStr + ".color", "WHITE");
                String role = ranksConfig.getString("ranks." + uuidStr + ".role", "");
                rankNames.put(uuid, name);
                rankColors.put(uuid, color);
                if (!role.isEmpty()) {
                    playerRoleKeys.put(uuid, role);
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID entries
            }
        }
    }

    public void saveRanks() {
        if (ranksConfig == null) {
            ranksConfig = new YamlConfiguration();
        }

        // Rebuild the whole "ranks" section from scratch so removed players
        // (via clearRank) don't linger in the saved file.
        ranksConfig.set("ranks", null);

        for (UUID uuid : rankNames.keySet()) {
            ranksConfig.set("ranks." + uuid + ".name", rankNames.get(uuid));
            ranksConfig.set("ranks." + uuid + ".color", rankColors.getOrDefault(uuid, "WHITE"));
            ranksConfig.set("ranks." + uuid + ".role", playerRoleKeys.getOrDefault(uuid, ""));
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

    public void assignRole(OfflinePlayer target, RoleDefinition role) {
        UUID uuid = target.getUniqueId();
        rankNames.put(uuid, role.displayName);
        rankColors.put(uuid, role.colorHex);
        playerRoleKeys.put(uuid, role.key);
        saveRanks();
    }

    /**
     * Removes a player's rank entirely - clears their custom rank name/color
     * AND any predefined role (Market Minister, Finance Minister, etc).
     * Returns true if the player had anything to remove.
     */
    public boolean clearRank(UUID uuid) {
        boolean hadSomething = rankNames.containsKey(uuid) || playerRoleKeys.containsKey(uuid);
        rankNames.remove(uuid);
        rankColors.remove(uuid);
        playerRoleKeys.remove(uuid);
        saveRanks();
        return hadSomething;
    }

    public boolean hasRole(UUID uuid, String roleKey) {
        return roleKey.equalsIgnoreCase(playerRoleKeys.get(uuid));
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
