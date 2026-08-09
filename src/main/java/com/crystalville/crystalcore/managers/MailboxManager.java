package com.crystalville.crystalcore.managers;

import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds Crystals that were sent via /pay to a player who was offline at the time.
 * Amounts are persisted to mailbox.yml and delivered straight into the
 * player's inventory the next time they join.
 */
public class MailboxManager {

    private final JavaPlugin plugin;
    private final File mailboxFile;
    private FileConfiguration mailboxConfig;

    private final Map<UUID, Integer> pendingCrystals = new HashMap<>();

    public MailboxManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mailboxFile = new File(plugin.getDataFolder(), "mailbox.yml");
    }

    public void load() {
        if (!mailboxFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                mailboxFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create mailbox.yml: " + e.getMessage());
            }
        }

        mailboxConfig = YamlConfiguration.loadConfiguration(mailboxFile);
        pendingCrystals.clear();

        if (mailboxConfig.getConfigurationSection("pending") == null) {
            return;
        }

        for (String uuidStr : mailboxConfig.getConfigurationSection("pending").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int amount = mailboxConfig.getInt("pending." + uuidStr, 0);
                if (amount > 0) {
                    pendingCrystals.put(uuid, amount);
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID entries
            }
        }
    }

    public void save() {
        if (mailboxConfig == null) {
            mailboxConfig = new YamlConfiguration();
        }

        // Clear the whole section first so delivered/removed entries don't linger.
        mailboxConfig.set("pending", null);
        for (Map.Entry<UUID, Integer> entry : pendingCrystals.entrySet()) {
            mailboxConfig.set("pending." + entry.getKey(), entry.getValue());
        }

        try {
            mailboxConfig.save(mailboxFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save mailbox.yml: " + e.getMessage());
        }
    }

    /** Adds Crystals to a player's mailbox (they'll receive it on next join). */
    public void addPending(UUID uuid, int amount) {
        pendingCrystals.merge(uuid, amount, Integer::sum);
        save();
    }

    public boolean hasPending(UUID uuid) {
        return pendingCrystals.getOrDefault(uuid, 0) > 0;
    }

    /** Delivers any pending Crystals into the given (now-online) player's inventory. */
    public void deliverIfPending(Player player) {
        UUID uuid = player.getUniqueId();
        Integer amount = pendingCrystals.remove(uuid);
        if (amount == null || amount <= 0) {
            return;
        }

        int maxStack = CrystalItemUtil.CURRENCY_MATERIAL.getMaxStackSize();
        int remaining = amount;
        Map<Integer, ItemStack> overflow = new HashMap<>();

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = CrystalItemUtil.createCrystal(stackSize);
            overflow.putAll(player.getInventory().addItem(stack));
            remaining -= stackSize;
        }

        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(Component.text("You received " + amount
                + " Crystal(s) that were sent to you while you were offline.", NamedTextColor.GREEN));

        save();
    }
                                    }
