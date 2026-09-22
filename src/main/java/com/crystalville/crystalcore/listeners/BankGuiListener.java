package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.gui.BankGuiHolder;
import com.crystalville.crystalcore.gui.BankGuiManager;
import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all interaction with the Crystal Bank GUI: restricting deposit
 * slots to Crystal items only, wiring the Confirm Deposit / Withdraw /
 * Close buttons, and returning any un-confirmed items on close so nothing
 * is ever lost or duplicated.
 */
public class BankGuiListener implements Listener {

    private final BankGuiManager bankGuiManager;
    private final BankManager bankManager;

    public BankGuiListener(BankGuiManager bankGuiManager, BankManager bankManager) {
        this.bankGuiManager = bankGuiManager;
        this.bankManager = bankManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BankGuiHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();
        boolean clickedTopInventory = rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize();

        if (!clickedTopInventory) {
            // Block shift-clicking items from the player's own inventory, since that
            // could otherwise dump non-Crystal items straight into deposit slots.
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
            }
            return;
        }

        if (BankGuiManager.isDepositSlot(rawSlot)) {
            handleDepositSlotClick(event, player);
            return;
        }

        // Every other slot in the GUI (border, buttons) is fixed and not removable.
        event.setCancelled(true);

        if (rawSlot == BankGuiManager.CONFIRM_DEPOSIT_SLOT) {
            handleConfirmDeposit(event.getInventory(), player);
        } else if (rawSlot == BankGuiManager.WITHDRAW_100_SLOT) {
            handleWithdraw(event.getInventory(), player, 100);
        } else if (rawSlot == BankGuiManager.WITHDRAW_500_SLOT) {
            handleWithdraw(event.getInventory(), player, 500);
        } else if (rawSlot == BankGuiManager.WITHDRAW_ALL_SLOT) {
            long balance = bankManager.getBalance(player.getUniqueId());
            handleWithdraw(event.getInventory(), player, balance);
        } else if (rawSlot == BankGuiManager.CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    /** Deposit slots behave like a normal chest slot, but only accept Crystal items. */
    private void handleDepositSlotClick(InventoryClickEvent event, Player player) {
        ItemStack cursor = event.getCursor();

        boolean placingNonCrystal = cursor != null
                && cursor.getType() != Material.AIR
                && cursor.getType() != CrystalItemUtil.CURRENCY_MATERIAL;

        if (placingNonCrystal) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Only Crystals can be placed here.", NamedTextColor.RED));
        }
        // Otherwise leave the event uncancelled - normal chest-style placement/removal.
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BankGuiHolder)) {
            return;
        }

        // Dragging across multiple slots is disabled for safety; players can still
        // place Crystals one slot at a time via normal single clicks.
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BankGuiHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // Return any un-confirmed items sitting in the deposit slots so nothing is lost.
        for (int slot : BankGuiManager.DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inv.setItem(slot, null);
            }
        }
    }

    private void handleConfirmDeposit(Inventory inv, Player player) {
        int totalToDeposit = 0;
        for (int slot : BankGuiManager.DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                totalToDeposit += item.getAmount();
            }
        }

        if (totalToDeposit <= 0) {
            player.sendMessage(Component.text(
                    "Place some Crystals in the slots above first.", NamedTextColor.YELLOW));
            return;
        }

        boolean unlimited = bankGuiManager.isUnlimited(player);
        long deposited = bankManager.deposit(player.getUniqueId(), totalToDeposit, unlimited);

        int remainingToRemove = (int) deposited;
        for (int slot : BankGuiManager.DEPOSIT_SLOTS) {
            if (remainingToRemove <= 0) {
                break;
            }
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() != CrystalItemUtil.CURRENCY_MATERIAL) {
                continue;
            }
            int amount = item.getAmount();
            if (amount <= remainingToRemove) {
                remainingToRemove -= amount;
                inv.setItem(slot, null);
            } else {
                item.setAmount(amount - remainingToRemove);
                remainingToRemove = 0;
            }
        }

        player.sendMessage(Component.text(
                "Deposited " + deposited + " Crystal(s) into your bank.", NamedTextColor.GREEN));

        if (!unlimited && deposited < totalToDeposit) {
            player.sendMessage(Component.text(
                    (totalToDeposit - deposited) + " Crystal(s) couldn't be deposited - bank limit reached.",
                    NamedTextColor.YELLOW));
        }

        bankGuiManager.refreshBalance(inv, player);
    }

    private void handleWithdraw(Inventory inv, Player player, long amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("You have nothing to withdraw.", NamedTextColor.YELLOW));
            return;
        }

        long withdrawn = bankManager.withdraw(player.getUniqueId(), amount);
        if (withdrawn <= 0) {
            player.sendMessage(Component.text("You have nothing in your bank.", NamedTextColor.YELLOW));
            return;
        }

        giveCrystals(player, (int) withdrawn);
        player.sendMessage(Component.text(
                "Withdrew " + withdrawn + " Crystal(s) from your bank.", NamedTextColor.GREEN));

        bankGuiManager.refreshBalance(inv, player);
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
