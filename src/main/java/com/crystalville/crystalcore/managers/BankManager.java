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
 * Stores each player's Crystal Bank balance and personal cap. The default
 * cap is NORMAL_PLAYER_CAP (10,000), but any player's cap can be
 * individually raised via /increase limit. OPs and Finance Minister role
 * holders bypass caps entirely (checked by the caller, not this class).
 */
public class BankManager {

    public static final long NORMAL_PLAYER_CAP = 10_000L;

    private final JavaPlugin plugin;
    private final File bankFile;
    private FileConfiguration bankConfig;

    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<UUID, Long> customCaps = new HashMap<>();

    public BankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bankFile = new File(plugin.getDataFolder(), "bank.yml");
    }

    public void load() {
        if (!bankFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                bankFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create bank.yml: " + e.getMessage());
            }
        }

        bankConfig = YamlConfiguration.loadConfiguration(bankFile);
        balances.clear();
        customCaps.clear();

        if (bankConfig.getConfigurationSection("balances") != null) {
            for (String uuidStr : bankConfig.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    balances.put(uuid, bankConfig.getLong("balances." + uuidStr, 0L));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (bankConfig.getConfigurationSection("caps") != null) {
            for (String uuidStr : bankConfig.getConfigurationSection("caps").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long cap = bankConfig.getLong("caps." + uuidStr, NORMAL_PLAYER_CAP);
                    if (cap > 0) {
                        customCaps.put(uuid, cap);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        if (bankConfig == null) {
            bankConfig = new YamlConfiguration();
        }
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            bankConfig.set("balances." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Long> entry : customCaps.entrySet()) {
            bankConfig.set("caps." + entry.getKey(), entry.getValue());
        }
        try {
            bankConfig.save(bankFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save bank.yml: " + e.getMessage());
        }
    }

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    /** This player's personal bank cap - default 10,000, or a custom raised limit if set. */
    public long getCap(UUID uuid) {
        return customCaps.getOrDefault(uuid, NORMAL_PLAYER_CAP);
    }

    /** Sets a custom personal bank cap for a player (via /increase limit). Persists immediately. */
    public void setCap(UUID uuid, long newCap) {
        customCaps.put(uuid, newCap);
        save();
    }

    public long deposit(UUID uuid, long amount, boolean unlimited) {
        long current = getBalance(uuid);
        long actuallyDeposited;

        if (unlimited) {
            actuallyDeposited = amount;
        } else {
            long cap = getCap(uuid);
            long room = Math.max(0, cap - current);
            actuallyDeposited = Math.min(amount, room);
        }

        balances.put(uuid, current + actuallyDeposited);
        save();
        return actuallyDeposited;
    }

    public long withdraw(UUID uuid, long amount) {
        long current = getBalance(uuid);
        long actualWithdraw = Math.min(amount, current);
        balances.put(uuid, current - actualWithdraw);
        save();
        return actualWithdraw;
    }
            }
