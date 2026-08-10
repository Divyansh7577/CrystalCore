package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.managers.MailboxManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * /pay <player> <amount>
 *
 * Works regardless of receiver status:
 * - Online with space: Crystals go straight into their inventory.
 * - Offline: saved to their mailbox, delivered automatically (with a
 *   "who sent it" breakdown) the next time they join.
 * - Online but inventory full: saved as a claimable balance instead of
 *   being dropped on the ground. They collect it with /claim <amount>.
 *
 * OPs bypass their own inventory cost entirely (infinite send).
 */
public class PayCommand implements CommandExecutor {

    private final CrystalCore plugin;

    public PayCommand(CrystalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player payer = (Player) sender;

        if (args.length != 2) {
            payer.sendMessage(Component.text("Usage: /pay <player> <amount>", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            payer.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            payer.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            payer.sendMessage(Component.text(
                    "Player '" + args[0] + "' has never joined this server.", NamedTextColor.RED));
            return true;
        }

        if (payer.getUniqueId().equals(target.getUniqueId())) {
            payer.sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
            return true;
        }

        if (!payer.isOp()) {
            int have = countCrystals(payer.getInventory());
            if (have < amount) {
                payer.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + amount + ", You have: " + have,
                        NamedTextColor.RED));
                return true;
            }
            removeCrystals(payer.getInventory(), amount);
        }

        Player onlineTarget = target.getPlayer();
        MailboxManager mailboxManager = plugin.getMailboxManager();

        boolean targetOffline = onlineTarget == null || !onlineTarget.isOnline();
        boolean queuedDueToFullInventory = false;

        if (!targetOffline) {
            int capacity = CrystalItemUtil.freeCapacity(onlineTarget);
            if (capacity >= amount) {
                giveCrystals(onlineTarget, amount);
                onlineTarget.sendMessage(Component.text(
                        "You received " + amount + " Crystal(s) from " + payer.getName() + ".",
                        NamedTextColor.GREEN));
            } else {
                mailboxManager.addPending(target.getUniqueId(), payer.getName(), amount);
                queuedDueToFullInventory = true;
                onlineTarget.sendMessage(Component.text(
                        "Your inventory is full! " + payer.getName() + " sent you " + amount
                                + " Crystal(s) - claim them anytime with /claim <amount>.", NamedTextColor.YELLOW));
            }
        } else {
            mailboxManager.addPending(target.getUniqueId(), payer.getName(), amount);
        }

        if (payer.isOp()) {
            payer.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Sent " + amount + " Crystal(s) to " + args[0]
                            + " (unlimited, nothing deducted from your inventory).", NamedTextColor.GREEN)));
        } else {
            payer.sendMessage(Component.text(
                    "You paid " + amount + " Crystal(s) to " + args[0] + ".", NamedTextColor.GREEN));
        }

        if (targetOffline) {
            payer.sendMessage(Component.text(
                    args[0] + " is offline - they will receive it when they next log in.", NamedTextColor.GRAY));
        } else if (queuedDueToFullInventory) {
            payer.sendMessage(Component.text(
                    args[0] + "'s inventory was full - saved as a claimable balance instead.",
                    NamedTextColor.GRAY));
        }

        return true;
    }

    private int countCrystals(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeCrystals(Inventory inventory, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
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
