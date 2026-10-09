package com.crystalville.crystalcore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores OP-created redeem codes: an amount of Crystals, an optional
 * expiration timestamp (null/absent = never expires), and which players
 * have already redeemed it (each code redeemable once per player).
 */
public class RedeemCodeManager {

    public static final class RedeemCode {
        public final String code;
        public final long amount;
        public final Long expiresAtMillis; // null = infinite, never expires
        public final String createdBy;
        public final Set<UUID> redeemedBy = new HashSet<>();

        public RedeemCode(String code, long amount, Long expiresAtMillis, String createdBy) {
            this.code = code;
            this.amount = amount;
            this.expiresAtMillis = expiresAtMillis;
            this.createdBy = createdBy;
        }

        public boolean isExpired() {
            return expiresAtMillis != null && System.currentTimeMillis() > expiresAtMillis;
        }

        public boolean hasBeenRedeemedBy(UUID uuid) {
            return redeemedBy.contains(uuid);
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<String, RedeemCode> codes = new LinkedHashMap<>();

    public RedeemCodeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "redeem_codes.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create redeem_codes.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        codes.clear();

        ConfigurationSection section = config.getConfigurationSection("codes");
        if (section == null) {
            return;
        }

        for (String codeKey : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(codeKey);
            if (entry == null) continue;

            long amount = entry.getLong("amount", 0L);
            String createdBy = entry.getString("createdBy", "Unknown");
            Long expiresAt = entry.contains("expiresAt") ? entry.getLong("expiresAt") : null;

            RedeemCode redeemCode = new RedeemCode(codeKey, amount, expiresAt, createdBy);

            for (String uuidStr : entry.getStringList("redeemedBy")) {
                try {
                    redeemCode.redeemedBy.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed entries
                }
            }

            codes.put(codeKey, redeemCode);
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        config.set("codes", null);
        for (RedeemCode code : codes.values()) {
            String base = "codes." + code.code;
            config.set(base + ".amount", code.amount);
            config.set(base + ".createdBy", code.createdBy);
            if (code.expiresAtMillis != null) {
                config.set(base + ".expiresAt", code.expiresAtMillis);
            }

            List<String> redeemedByList = new ArrayList<>();
            for (UUID uuid : code.redeemedBy) {
                redeemedByList.add(uuid.toString());
            }
            config.set(base + ".redeemedBy", redeemedByList);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save redeem_codes.yml: " + e.getMessage());
        }
    }

    public boolean exists(String code) {
        return codes.containsKey(code);
    }

    public RedeemCode get(String code) {
        return codes.get(code);
    }

    /**
     * Creates a new redeem code. expirationMinutes null means it never
     * expires; a positive value sets an expiration that many minutes from now.
     */
    public RedeemCode create(String code, long amount, Long expirationMinutes, String createdBy) {
        Long expiresAt = expirationMinutes == null
                ? null
                : System.currentTimeMillis() + (expirationMinutes * 60_000L);

        RedeemCode redeemCode = new RedeemCode(code, amount, expiresAt, createdBy);
        codes.put(code, redeemCode);
        save();
        return redeemCode;
    }

    /** Marks a code as redeemed by this player. Caller must have already validated eligibility. */
    public void markRedeemed(String code, UUID uuid) {
        RedeemCode redeemCode = codes.get(code);
        if (redeemCode != null) {
            redeemCode.redeemedBy.add(uuid);
            save();
        }
    }
  }
