package com.crystalville.crystalcore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks whether each OP has voluntarily turned OFF their own "OP Bypass"
 * perk - unlimited Crystals / free items across /pay, /buy, /sell
 * overflow, /bank cap, /enterprise, and /payenterprise. Bypass is ON by
 * default for every OP, matching the plugin's original behavior; this
 * only lets an OP opt out (e.g. to test the server as a normal player).
 * Non-OPs never have bypass regardless of this setting.
 */
public class OpBypassManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    private final Set<UUID> bypassDisabled = new HashSet<>();

    public OpBypassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "op_bypass.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create op_bypass.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        bypassDisabled.clear();

        for (String uuidStr : config.getStringList("disabled")) {
            try {
                bypassDisabled.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        List<String> serialized = new ArrayList<>();
        for (UUID uuid : bypassDisabled) {
            serialized.add(uuid.toString());
        }
        config.set("disabled", serialized);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save op_bypass.yml: " + e.getMessage());
        }
    }

    /** True only if this player is an OP AND has not turned their bypass off. */
    public boolean hasBypass(Player player) {
        return player.isOp() && !bypassDisabled.contains(player.getUniqueId());
    }

    public boolean isEnabled(UUID uuid) {
        return !bypassDisabled.contains(uuid);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            bypassDisabled.remove(uuid);
        } else {
            bypassDisabled.add(uuid);
        }
        save();
    }

    public boolean toggle(UUID uuid) {
        boolean newEnabled = !isEnabled(uuid);
        setEnabled(uuid, newEnabled);
        return newEnabled;
    }
}
