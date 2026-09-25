package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterpriseManager;
import com.crystalville.crystalcore.managers.EnterprisePermission;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /payenterprise <EnterpriseID> <amount>
 * Pays an Enterprise Bank directly (UPI-style), instantly. Open to every
 * player. OPs bypass their own inventory cost. The Enterprise's balance
 * cap (default 200,000, raisable via /enterprise increase) always applies.
 */
public class PayEnterpriseCommand implements CommandExecutor {

    private final EnterpriseManager enterpriseManager;

    public PayEnterpriseCommand(EnterpriseManager enterpriseManager) {
        this.enterpriseManager = enterpriseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /payenterprise <EnterpriseID> <amount>", NamedTextColor.RED));
            return true;
        }

        String enterpriseId = EnterpriseManager.normalize(args[0]);
        EnterpriseAccount account = enterpriseManager.get(enterpriseId);

        if (account == null) {
            player.sendMessage(Component.text(
                    "No Enterprise found with ID '" + enterpriseId + "'.", NamedTextColor.RED));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        long room = account.maxBalance - account.balance;
        if (amount > room) {
            player.sendMessage(Component.text(
                    account.name + " can only receive " + room + " more Crystal(s) before hitting its "
                            + String.format("%,d", account.maxBalance) + " limit.", NamedTextColor.RED));
            return true;
        }

        if (!player.isOp()) {
            int have = countCrystals(player.getInventory());
            if (have < amount) {
                player.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + amount + ", You have: " + have,
                        NamedTextColor.RED));
                return true;
            }
            removeCrystals(player.getInventory(), (int) amount);
        }

        account.addBalance(amount);
        account.addTransaction(player.getName(), "PAYMENT", amount);
        enterpriseManager.save();

        if (player.isOp()) {
            player.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Paid " + amount + " Crystal(s) to " + account.name
                            + " (unlimited, nothing deducted from your inventory).", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Paid " + amount + " Crystal(s) to " + account.name + ".",
                    NamedTextColor.GREEN));
        }

        notifyMembers(account, player, amount);
        return true;
    }

    private void notifyMembers(EnterpriseAccount account, Player payer, long amount) {
        for (Map.Entry<UUID, Set<EnterprisePermission>> entry : account.members.entrySet()) {
            if (!account.hasPermission(entry.getKey(), EnterprisePermission.VIEW_BALANCE)) {
                continue;
            }
            Player member = Bukkit.getPlayer(entry.getKey());
            if (member != null && member.isOnline()) {
                member.sendMessage(Component.text(payer.getName() + " paid " + amount + " Crystal(s) to "
                        + account.name + ". New balance: " + account.balance + " Crystal(s).",
                        NamedTextColor.AQUA));
            }
        }

        Player owner = Bukkit.getPlayer(account.ownerUuid);
        if (owner != null && owner.isOnline() && !owner.getUniqueId().equals(payer.getUniqueId())) {
            owner.sendMessage(Component.text(payer.getName() + " paid " + amount + " Crystal(s) to "
                    + account.name + ". New balance: " + account.balance + " Crystal(s).", NamedTextColor.AQUA));
        }
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
                }
