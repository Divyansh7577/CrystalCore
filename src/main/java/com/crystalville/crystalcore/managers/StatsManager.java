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
 * Tracks kills, deaths, and accumulated playtime per player for the HUD.
 * Playtime is stored as a running total (seconds) plus an in-memory
 * session start timestamp, so "live" playtime = stored total + current
 * session elapsed, without writing to disk every tick.
 */
public class StatsManager {

    private final JavaPlugin plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Long> totalPlaytimeSeconds = new HashMap<>();
    private final Map<UUID, Long> sessionStartMillis = new HashMap<>();

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        if (!statsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml: " + e.getMessage());
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        kills.clear();
        deaths.clear();
        totalPlaytimeSeconds.clear();

        if (statsConfig.getConfigurationSection("stats") == null) {
            return;
        }

        for (String uuidStr : statsConfig.getConfigurationSection("stats").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                kills.put(uuid, statsConfig.getInt("stats." + uuidStr + ".kills", 0));
                deaths.put(uuid, statsConfig.getInt("stats." + uuidStr + ".deaths", 0));
                totalPlaytimeSeconds.put(uuid, statsConfig.getLong("stats." + uuidStr + ".playtime", 0L));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID entries
            }
        }
    }

    public void save() {
        if (statsConfig == null) {
            statsConfig = new YamlConfiguration();
        }

        for (UUID uuid : kills.keySet()) {
            statsConfig.set("stats." + uuid + ".kills", kills.getOrDefault(uuid, 0));
            statsConfig.set("stats." + uuid + ".deaths", deaths.getOrDefault(uuid, 0));
            statsConfig.set("stats." + uuid + ".playtime", totalPlaytimeSeconds.getOrDefault(uuid, 0L));
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }

    public void incrementKills(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
        save();
    }

    public void incrementDeaths(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
        save();
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getDeaths(UUID uuid) {
        return deaths.getOrDefault(uuid, 0);
    }

    /** Call when a player joins - starts a live session timer for playtime tracking. */
    public void startSession(UUID uuid) {
        sessionStartMillis.put(uuid, System.currentTimeMillis());
    }

    /** Call when a player leaves - folds the session's elapsed time into the stored total. */
    public void endSession(UUID uuid) {
        Long start = sessionStartMillis.remove(uuid);
        if (start == null) {
            return;
        }
        long elapsedSeconds = (System.currentTimeMillis() - start) / 1000L;
        totalPlaytimeSeconds.merge(uuid, elapsedSeconds, Long::sum);
        save();
    }

    /** Returns total playtime in seconds, including the current live session if online. */
    public long getLivePlaytimeSeconds(UUID uuid) {
        long stored = totalPlaytimeSeconds.getOrDefault(uuid, 0L);
        Long start = sessionStartMillis.get(uuid);
        if (start == null) {
            return stored;
        }
        long liveElapsed = (System.currentTimeMillis() - start) / 1000L;
        return stored + liveElapsed;
    }

    /** Formats seconds into a short "1d 4h" / "4h 12m" / "12m" style string. */
    public static String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
  }
