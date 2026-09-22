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
 * Stores each player's Crystal Bank balance - a safe stash of Crystals kept
 * separate from their physical inventory. Normal players are capped at
 * NORMAL_PLAYER_CAP; OPs and Finance Minister role holders have no cap
 * (checked by the caller, not this class).
 */
public class BankManager {

    public static final long NORMAL_PLAYER_CAP = 10_000L;

    private final JavaPlugin plugin;
    private final File bankFile;
    private FileConfiguration bankConfig;

    private final Map<UUID, Long> balances = new HashMap<>();

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

        if (bankConfig.getConfigurationSection("balances") == null) {
            return;
        }

        for (String uuidStr : bankConfig.getConfigurationSection("balances").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                balances.put(uuid, bankConfig.getLong("balances." + uuidStr, 0L));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID entries
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
        try {
            bankConfig.save(bankFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save bank.yml: " + e.getMessage());
        }
    }

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    /**
     * Attempts to add `amount` to the player's balance. If not unlimited,
     * deposits are silently capped at NORMAL_PLAYER_CAP.
     * Returns the amount actually deposited (may be less than requested).
     */
    public long deposit(UUID uuid, long amount, boolean unlimited) {
        long current = getBalance(uuid);
        long actuallyDeposited;

        if (unlimited) {
            actuallyDeposited = amount;
        } else {
            long room = Math.max(0, NORMAL_PLAYER_CAP - current);
            actuallyDeposited = Math.min(amount, room);
        }

        balances.put(uuid, current + actuallyDeposited);
        save();
        return actuallyDeposited;
    }

    /**
     * Attempts to withdraw `amount` from the player's balance.
     * Returns the amount actually withdrawn (may be less if balance is insufficient).
     */
    public long withdraw(UUID uuid, long amount) {
        long current = getBalance(uuid);
        long actualWithdraw = Math.min(amount, current);
        balances.put(uuid, current - actualWithdraw);
        save();
        return actualWithdraw;
    }
}
