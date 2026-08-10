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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds Crystals that were sent via /pay to a player who was offline, or
 * whose inventory was too full to receive them directly. Each pending
 * payment remembers WHO sent it. Players withdraw their balance manually
 * with /claim <amount>, or it auto-delivers in full when they next join
 * (for the offline case).
 */
public class MailboxManager {

    public static final class PendingPayment {
        public final String senderName;
        public final int amount;

        public PendingPayment(String senderName, int amount) {
            this.senderName = senderName;
            this.amount = amount;
        }
    }

    private final JavaPlugin plugin;
    private final File mailboxFile;
    private FileConfiguration mailboxConfig;

    private final Map<UUID, List<PendingPayment>> pendingPayments = new HashMap<>();

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
        pendingPayments.clear();

        if (mailboxConfig.getConfigurationSection("pending") == null) {
            return;
        }

        for (String uuidStr : mailboxConfig.getConfigurationSection("pending").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> entries = mailboxConfig.getStringList("pending." + uuidStr);
                List<PendingPayment> payments = new ArrayList<>();

                for (String entry : entries) {
                    int splitIndex = entry.lastIndexOf('|');
                    if (splitIndex == -1) continue;

                    String senderName = entry.substring(0, splitIndex);
                    String amountStr = entry.substring(splitIndex + 1);

                    try {
                        int amount = Integer.parseInt(amountStr);
                        if (amount > 0) {
                            payments.add(new PendingPayment(senderName, amount));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (!payments.isEmpty()) {
                    pendingPayments.put(uuid, payments);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        if (mailboxConfig == null) {
            mailboxConfig = new YamlConfiguration();
        }

        mailboxConfig.set("pending", null);
        for (Map.Entry<UUID, List<PendingPayment>> entry : pendingPayments.entrySet()) {
            List<String> serialized = new ArrayList<>();
            for (PendingPayment payment : entry.getValue()) {
                serialized.add(payment.senderName + "|" + payment.amount);
            }
            mailboxConfig.set("pending." + entry.getKey(), serialized);
        }

        try {
            mailboxConfig.save(mailboxFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save mailbox.yml: " + e.getMessage());
        }
    }

    /** Queues a Crystal payment for a player, remembering who sent it. */
    public void addPending(UUID targetUuid, String senderName, int amount) {
        pendingPayments.computeIfAbsent(targetUuid, k -> new ArrayList<>())
                .add(new PendingPayment(senderName, amount));
        save();
    }

    public boolean hasPending(UUID uuid) {
        List<PendingPayment> payments = pendingPayments.get(uuid);
        return payments != null && !payments.isEmpty();
    }

    /** Total Crystals currently owed to this player across all pending payments. */
    public int getTotalPending(UUID uuid) {
        List<PendingPayment> payments = pendingPayments.get(uuid);
        if (payments == null) {
            return 0;
        }
        int total = 0;
        for (PendingPayment payment : payments) {
            total += payment.amount;
        }
        return total;
    }

    /**
     * Deducts up to `amount` Crystals from the player's pending balance
     * (oldest entries first, splitting a payment if needed). Returns how
     * much was actually deducted (never more than what was pending).
     */
    public int deductPending(UUID uuid, int amount) {
        List<PendingPayment> payments = pendingPayments.get(uuid);
        if (payments == null || payments.isEmpty() || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        List<PendingPayment> updated = new ArrayList<>();

        for (PendingPayment payment : payments) {
            if (remaining <= 0) {
                updated.add(payment);
                continue;
            }
            if (payment.amount <= remaining) {
                remaining -= payment.amount;
                // fully consumed - not carried over
            } else {
                updated.add(new PendingPayment(payment.senderName, payment.amount - remaining));
                remaining = 0;
            }
        }

        if (updated.isEmpty()) {
            pendingPayments.remove(uuid);
        } else {
            pendingPayments.put(uuid, updated);
        }
        save();

        return amount - remaining;
    }

    /**
     * Delivers ALL pending Crystals into the given (now-online) player's
     * inventory, announcing each sender individually. Used automatically
     * on join for offline-payment delivery.
     */
    public void deliverIfPending(Player player) {
        UUID uuid = player.getUniqueId();
        List<PendingPayment> payments = pendingPayments.remove(uuid);
        if (payments == null || payments.isEmpty()) {
            return;
        }

        int totalAmount = 0;
        for (PendingPayment payment : payments) {
            totalAmount += payment.amount;
        }

        giveCrystals(player, totalAmount);

        player.sendMessage(Component.text(
                "While you were offline, you received Crystals from:", NamedTextColor.GREEN));
        for (PendingPayment payment : payments) {
            player.sendMessage(Component.text(
                    "  - " + payment.senderName + ": " + payment.amount + " Crystal(s)", NamedTextColor.GRAY));
        }
        player.sendMessage(Component.text(
                "Total received: " + totalAmount + " Crystal(s)", NamedTextColor.GREEN));

        save();
    }

    private void giveCrystals(Player player, int amount) {
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
    }
  }
