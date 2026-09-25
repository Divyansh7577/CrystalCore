package com.crystalville.crystalcore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EnterpriseManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<String, EnterpriseAccount> enterprises = new LinkedHashMap<>();

    public EnterpriseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "enterprises.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create enterprises.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        enterprises.clear();

        ConfigurationSection section = config.getConfigurationSection("enterprises");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String name = entry.getString("name", id);
            String ownerStr = entry.getString("owner", "");
            UUID owner;
            try {
                owner = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            EnterpriseAccount account = new EnterpriseAccount(id, name, owner);
            account.balance = entry.getLong("balance", 0L);
            account.maxBalance = entry.getLong("maxBalance", EnterpriseAccount.DEFAULT_MAX_BALANCE);

            ConfigurationSection membersSection = entry.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberUuidStr : membersSection.getKeys(false)) {
                    try {
                        UUID memberUuid = UUID.fromString(memberUuidStr);
                        String permsRaw = membersSection.getString(memberUuidStr, "");
                        Set<EnterprisePermission> perms = new HashSet<>();
                        for (String part : permsRaw.split(",")) {
                            EnterprisePermission perm = EnterprisePermission.fromString(part);
                            if (perm != null) perms.add(perm);
                        }
                        account.members.put(memberUuid, perms);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            List<String> historyRaw = entry.getStringList("history");
            for (String raw : historyRaw) {
                EnterpriseTransaction tx = EnterpriseTransaction.deserialize(raw);
                if (tx != null) account.history.add(tx);
            }

            enterprises.put(id, account);
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        config.set("enterprises", null);
        for (EnterpriseAccount account : enterprises.values()) {
            String base = "enterprises." + account.enterpriseId;
            config.set(base + ".name", account.name);
            config.set(base + ".owner", account.ownerUuid.toString());
            config.set(base + ".balance", account.balance);
            config.set(base + ".maxBalance", account.maxBalance);

            for (Map.Entry<UUID, Set<EnterprisePermission>> entry : account.members.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (EnterprisePermission perm : entry.getValue()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(perm.name());
                }
                config.set(base + ".members." + entry.getKey(), sb.toString());
            }

            List<String> historySerialized = new ArrayList<>();
            for (EnterpriseTransaction tx : account.history) {
                historySerialized.add(tx.serialize());
            }
            config.set(base + ".history", historySerialized);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save enterprises.yml: " + e.getMessage());
        }
    }

    public boolean exists(String enterpriseId) {
        return enterprises.containsKey(normalize(enterpriseId));
    }

    public EnterpriseAccount get(String enterpriseId) {
        return enterprises.get(normalize(enterpriseId));
    }

    public static String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase();
    }

    public static boolean isValidId(String id) {
        return id != null && id.matches("[A-Z0-9_]{3,20}");
    }

    public EnterpriseAccount create(String enterpriseId, String name, UUID owner) {
        String normalized = normalize(enterpriseId);
        EnterpriseAccount account = new EnterpriseAccount(normalized, name, owner);
        enterprises.put(normalized, account);
        save();
        return account;
    }

    /** Raises (or lowers) an Enterprise's maximum balance cap. Persists immediately. */
    public void setMaxBalance(EnterpriseAccount account, long newMax) {
        account.maxBalance = newMax;
        save();
    }

    public Collection<EnterpriseAccount> getAll() {
        return enterprises.values();
    }

    public List<EnterpriseAccount> getForPlayer(UUID uuid) {
        List<EnterpriseAccount> result = new ArrayList<>();
        for (EnterpriseAccount account : enterprises.values()) {
            if (account.isMember(uuid)) result.add(account);
        }
        return result;
    }
}