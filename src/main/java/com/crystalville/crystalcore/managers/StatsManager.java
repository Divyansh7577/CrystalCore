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
 * Tracks kills, deaths, blocks broken and accumulated playtime per player.
 */
public class StatsManager {

    private final JavaPlugin plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Long> totalPlaytimeSeconds = new HashMap<>();
    private final Map<UUID, Long> blocksBroken = new HashMap<>();
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
                plugin.getLogger().warning(
                        "Could not create stats.yml: " + e.getMessage()
                );
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        kills.clear();
        deaths.clear();
        totalPlaytimeSeconds.clear();
        blocksBroken.clear();
        sessionStartMillis.clear();

        if (statsConfig.getConfigurationSection("stats") == null) {
            return;
        }

        for (String uuidStr :
                statsConfig.getConfigurationSection("stats").getKeys(false)) {

            try {
                UUID uuid = UUID.fromString(uuidStr);

                kills.put(
                        uuid,
                        statsConfig.getInt(
                                "stats." + uuidStr + ".kills",
                                0
                        )
                );

                deaths.put(
                        uuid,
                        statsConfig.getInt(
                                "stats." + uuidStr + ".deaths",
                                0
                        )
                );

                totalPlaytimeSeconds.put(
                        uuid,
                        statsConfig.getLong(
                                "stats." + uuidStr + ".playtime_seconds",
                                0L
                        )
                );

                blocksBroken.put(
                        uuid,
                        statsConfig.getLong(
                                "stats." + uuidStr + ".blocks_broken",
                                0L
                        )
                );

            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning(
                        "Invalid UUID in stats.yml: " + uuidStr
                );
            }
        }
    }

    public void save() {
        if (statsConfig == null) {
            statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        }

        for (UUID uuid : kills.keySet()) {
            savePlayer(uuid);
        }

        for (UUID uuid : deaths.keySet()) {
            savePlayer(uuid);
        }

        for (UUID uuid : totalPlaytimeSeconds.keySet()) {
            savePlayer(uuid);
        }

        for (UUID uuid : blocksBroken.keySet()) {
            savePlayer(uuid);
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Could not save stats.yml: " + e.getMessage()
            );
        }
    }

    private void savePlayer(UUID uuid) {
        String path = "stats." + uuid;

        statsConfig.set(
                path + ".kills",
                kills.getOrDefault(uuid, 0)
        );

        statsConfig.set(
                path + ".deaths",
                deaths.getOrDefault(uuid, 0)
        );

        statsConfig.set(
                path + ".playtime_seconds",
                getLivePlaytimeSeconds(uuid)
        );

        statsConfig.set(
                path + ".blocks_broken",
                blocksBroken.getOrDefault(uuid, 0L)
        );
    }

    public void playerJoined(UUID uuid) {
        sessionStartMillis.put(
                uuid,
                System.currentTimeMillis()
        );
    }

    public void playerQuit(UUID uuid) {
        long live = getLivePlaytimeSeconds(uuid);

        totalPlaytimeSeconds.put(uuid, live);
        sessionStartMillis.remove(uuid);

        savePlayer(uuid);
    }

    public void incrementKills(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
    }

    public void incrementDeaths(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
    }

    public void incrementBlocksBroken(UUID uuid) {
        blocksBroken.merge(uuid, 1L, Long::sum);
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getDeaths(UUID uuid) {
        return deaths.getOrDefault(uuid, 0);
    }

    public long getBlocksBroken(UUID uuid) {
        return blocksBroken.getOrDefault(uuid, 0L);
    }

    public long getPlaytimeSeconds(UUID uuid) {
        return totalPlaytimeSeconds.getOrDefault(uuid, 0L);
    }

    public long getLivePlaytimeSeconds(UUID uuid) {
        long stored =
                totalPlaytimeSeconds.getOrDefault(uuid, 0L);

        Long start = sessionStartMillis.get(uuid);

        if (start == null) {
            return stored;
        }

        long elapsed =
                (System.currentTimeMillis() - start) / 1000L;

        return stored + Math.max(0L, elapsed);
    }

    public static String formatPlaytime(long seconds) {
        long days = seconds / 86400L;
        seconds %= 86400L;

        long hours = seconds / 3600L;
        seconds %= 3600L;

        long minutes = seconds / 60L;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }
}
