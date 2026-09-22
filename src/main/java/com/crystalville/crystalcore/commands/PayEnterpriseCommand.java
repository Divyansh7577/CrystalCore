package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterpriseManager;
import com.crystalville.crystalcore.managers.EnterprisePaymentConfirmationManager;
import com.crystalville.crystalcore.managers.EnterprisePermission;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
 * /payenterprise <EnterpriseID> <amount>  - pay an Enterprise Bank directly (UPI-style)
 * /payenterprise confirm                  - confirms a pending payment
 * /payenterprise cancel                   - cancels a pending payment
 *
 * Open to every player. Requires confirmation before completion to prevent
 * accidental transfers. OPs bypass their own inventory cost (infinite
 * send), matching the rest of CrystalCore's economy. The Enterprise's
 * 200,000 Crystal cap always applies regardless of who is paying.
 */
public class PayEnterpriseCommand implements CommandExecutor {

    private final EnterpriseManager enterpriseManager;
    private final EnterprisePaymentConfirmationManager confirmationManager;

    public PayEnterpriseCommand(EnterpriseManager enterpriseManager,
                                 EnterprisePaymentConfirmationManager confirmationManager) {
        this.enterpriseManager = enterpriseManager;
        this.confirmationManager = confirmationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /payenterprise <EnterpriseID> <amount>", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            handleConfirm(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            confirmationManager.clear(player.getUniqueId());
            player.sendMessage(Component.text("Payment cancelled.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /payenterprise <EnterpriseID> <amount>", NamedTextColor.RED));
            return true;
        }

        handleInitiate(player, args[0], args[1]);
        return true;
    }

    private void handleInitiate(Player player, String idInput, String amountInput) {
        String enterpriseId = EnterpriseManager.normalize(idInput);
        EnterpriseAccount account = enterpriseManager.get(enterpriseId);

        if (account == null) {
            player.sendMessage(Component.text(
                    "No Enterprise found with ID '" + enterpriseId + "'.", NamedTextColor.RED));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountInput);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return;
        }

        long room = EnterpriseAccount.MAX_BALANCE - account.balance;
        if (amount > room) {
            player.sendMessage(Component.text(
                    account.name + " can only receive " + room + " more Crystal(s) before hitting its "
                            + String.format("%,d", EnterpriseAccount.MAX_BALANCE) + " limit.", NamedTextColor.RED));
            return;
        }

        if (!player.isOp()) {
            int have = countCrystals(player.getInventory());
            if (have < amount) {
                player.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + amount + ", You have: " + have,
                        NamedTextColor.RED));
                return;
            }
        }

        confirmationManager.set(player.getUniqueId(), enterpriseId, amount);

        Component confirmButton = Component.text("[Click to Confirm]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/payenterprise confirm"));
        Component cancelButton = Component.text("[Cancel]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/payenterprise cancel"));

        player.sendMessage(Component.text("Confirm payment of ", NamedTextColor.YELLOW)
                .append(Component.text(amount + " Crystal(s) ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("to " + account.name + " (" + account.enterpriseId + ")?",
                        NamedTextColor.YELLOW)));
        player.sendMessage(confirmButton.append(Component.text("   ")).append(cancelButton));
        player.sendMessage(Component.text("This confirmation expires in 30 seconds.", NamedTextColor.GRAY));
    }

    private void handleConfirm(Player player) {
        EnterprisePaymentConfirmationManager.PendingPayment pending =
                confirmationManager.get(player.getUniqueId());

        if (pending == null) {
            player.sendMessage(Component.text(
                    "You have no pending Enterprise payment to confirm.", NamedTextColor.YELLOW));
            return;
        }

        if (pending.isExpired()) {
            confirmationManager.clear(player.getUniqueId());
            player.sendMessage(Component.text(
                    "That payment confirmation expired. Please try again.", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = enterpriseManager.get(pending.enterpriseId);
        if (account == null) {
            confirmationManager.clear(player.getUniqueId());
            player.sendMessage(Component.text("That Enterprise no longer exists.", NamedTextColor.RED));
            return;
        }

        long amount = pending.amount;
        long room = EnterpriseAccount.MAX_BALANCE - account.balance;
        if (amount > room) {
            confirmationManager.clear(player.getUniqueId());
            player.sendMessage(Component.text(
                    "This Enterprise's balance changed and can no longer accept that amount. Payment cancelled.",
                    NamedTextColor.RED));
            return;
        }

        if (!player.isOp()) {
            int have = countCrystals(player.getInventory());
            if (have < amount) {
                confirmationManager.clear(player.getUniqueId());
                player.sendMessage(Component.text(
                        "You no longer have enough Crystals for this payment. Payment cancelled.",
                        NamedTextColor.RED));
                return;
            }
            removeCrystals(player.getInventory(), (int) amount);
        }

        account.addBalance(amount);
        account.addTransaction(player.getName(), "PAYMENT", amount);
        enterpriseManager.save();
        confirmationManager.clear(player.getUniqueId());

        if (player.isOp()) {
            player.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Paid " + amount + " Crystal(s) to " + account.name
                            + " (unlimited, nothing deducted from your inventory).", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Paid " + amount + " Crystal(s) to " + account.name + ".",
                    NamedTextColor.GREEN));
        }

        notifyMembers(account, player, amount);
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
