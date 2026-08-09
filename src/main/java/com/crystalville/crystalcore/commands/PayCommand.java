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

/**
 * /pay <player> <amount>
 *
 * Works regardless of whether the receiver is online:
 * - If they're online, Crystals go straight into their inventory.
 * - If they're offline, Crystals are saved to their mailbox and delivered
 *   automatically (with a notification) the next time they join.
 *
 * OPs bypass their own inventory cost entirely (infinite send), matching
 * the rest of CrystalCore's economy rules. Normal players still need
 * enough Crystals in their own inventory to send.
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

        // Non-OP senders must actually have the Crystals in their inventory.
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

        if (onlineTarget != null && onlineTarget.isOnline()) {
            giveCrystals(onlineTarget, amount);
            onlineTarget.sendMessage(Component.text(
                    "You received " + amount + " Crystal(s) from " + payer.getName() + ".", NamedTextColor.GREEN));
        } else {
            mailboxManager.addPending(target.getUniqueId(), amount);
        }

        if (payer.isOp()) {
            payer.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Sent " + amount + " Crystal(s) to " + args[0]
                            + " (unlimited, nothing deducted from your inventory).", NamedTextColor.GREEN)));
        } else {
            payer.sendMessage(Component.text(
                    "You paid " + amount + " Crystal(s) to " + args[0] + ".", NamedTextColor.GREEN));
        }

        if (onlineTarget == null || !onlineTarget.isOnline()) {
            payer.sendMessage(Component.text(
                    args[0] + " is offline - they will receive it when they next log in.", NamedTextColor.GRAY));
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
        java.util.Map<Integer, ItemStack> overflow = new java.util.HashMap<>();

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
