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
 * Holds Crystals that were sent via /pay to a player who was offline at the time.
 * Each pending payment remembers WHO sent it, so the receiver gets a proper
 * "X sent you Y Crystals" message (not just a generic total) when they next join.
 */
public class MailboxManager {

    /** A single queued payment: who sent it and how much. */
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
                    // Stored format: "senderName|amount"
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
                        // skip malformed entry
                    }
                }

                if (!payments.isEmpty()) {
                    pendingPayments.put(uuid, payments);
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

    /** Queues a Crystal payment for an offline player, remembering who sent it. */
    public void addPending(UUID targetUuid, String senderName, int amount) {
        pendingPayments.computeIfAbsent(targetUuid, k -> new ArrayList<>())
                .add(new PendingPayment(senderName, amount));
        save();
    }

    public boolean hasPending(UUID uuid) {
        List<PendingPayment> payments = pendingPayments.get(uuid);
        return payments != null && !payments.isEmpty();
    }

    /**
     * Delivers all pending Crystals into the given (now-online) player's inventory,
     * announcing each sender individually.
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
