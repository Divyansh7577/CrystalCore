package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.MailboxManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * /claim <amount>
 * Withdraws up to <amount> Crystals from the player's pending balance
 * (built up from /pay when their inventory was too full to receive it
 * directly, or from offline payments). Only claims what actually fits
 * in their inventory right now; the rest stays pending for later.
 */
public class ClaimCommand implements CommandExecutor {

    private final MailboxManager mailboxManager;

    public ClaimCommand(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /claim <amount>", NamedTextColor.RED));
            return true;
        }

        int requested;
        try {
            requested = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (requested <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        int pending = mailboxManager.getTotalPending(player.getUniqueId());
        if (pending <= 0) {
            player.sendMessage(Component.text("You have no pending Crystals to claim.", NamedTextColor.YELLOW));
            return true;
        }

        int wanted = Math.min(requested, pending);
        int capacity = CrystalItemUtil.freeCapacity(player);

        if (capacity <= 0) {
            player.sendMessage(Component.text(
                    "Your inventory is full. Free up space and try again.", NamedTextColor.RED));
            return true;
        }

        int actualClaim = Math.min(wanted, capacity);
        int deducted = mailboxManager.deductPending(player.getUniqueId(), actualClaim);

        giveCrystals(player, deducted);

        player.sendMessage(Component.text("Claimed " + deducted + " Crystal(s).", NamedTextColor.GREEN));

        int remaining = mailboxManager.getTotalPending(player.getUniqueId());
        if (remaining > 0) {
            player.sendMessage(Component.text(
                    remaining + " Crystal(s) still pending - claim more anytime with /claim <amount>.",
                    NamedTextColor.GRAY));
        }

        return true;
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
