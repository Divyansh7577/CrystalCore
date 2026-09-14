package com.crystalville.crystalcore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player HUD visibility and each player's own personal footer
 * text (e.g. "Duo Gamerz X"), editable by that player via /edit. The
 * header/logo ("CRYSTAL VILLE") is intentionally NOT stored here - it's
 * hardcoded in HudListener and can never be changed by any command.
 */
public class HudManager {

    private static final String DEFAULT_FOOTER = "Duo Gamerz X";

    private final JavaPlugin plugin;
    private final File hudFile;
    private FileConfiguration hudConfig;

    private final Map<UUID, Boolean> enabled = new HashMap<>();
    private final Map<UUID, String> footerText = new HashMap<>();

    public HudManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hudFile = new File(plugin.getDataFolder(), "hud.yml");
    }

    public void load() {
        if (!hudFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                hudFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create hud.yml: " + e.getMessage());
            }
        }

        hudConfig = YamlConfiguration.loadConfiguration(hudFile);
        enabled.clear();
        footerText.clear();

        if (hudConfig.getConfigurationSection("enabled") != null) {
            for (String uuidStr : hudConfig.getConfigurationSection("enabled").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    enabled.put(uuid, hudConfig.getBoolean("enabled." + uuidStr, true));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed UUID entries
                }
            }
        }

        if (hudConfig.getConfigurationSection("footer") != null) {
            for (String uuidStr : hudConfig.getConfigurationSection("footer").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    footerText.put(uuid, hudConfig.getString("footer." + uuidStr, DEFAULT_FOOTER));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed UUID entries
                }
            }
        }
    }

    public void save() {
        if (hudConfig == null) {
            hudConfig = new YamlConfiguration();
        }

        for (Map.Entry<UUID, Boolean> entry : enabled.entrySet()) {
            hudConfig.set("enabled." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, String> entry : footerText.entrySet()) {
            hudConfig.set("footer." + entry.getKey(), entry.getValue());
        }

        try {
            hudConfig.save(hudFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save hud.yml: " + e.getMessage());
        }
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.getOrDefault(uuid, true);
    }

    public boolean toggle(UUID uuid) {
        boolean newValue = !isEnabled(uuid);
        enabled.put(uuid, newValue);
        save();
        return newValue;
    }

    /** Returns this player's own footer text, or the default if they haven't set one. */
    public String getFooterText(UUID uuid) {
        return footerText.getOrDefault(uuid, DEFAULT_FOOTER);
    }

    /** Sets a player's own footer text. Only affects that player's HUD - nobody else's. */
    public void setFooterText(UUID uuid, String text) {
        footerText.put(uuid, text);
        save();
    }
  }
