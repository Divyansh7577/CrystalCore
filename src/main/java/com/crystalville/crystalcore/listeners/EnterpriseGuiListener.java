package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.gui.EnterpriseGuiHolder;
import com.crystalville.crystalcore.gui.EnterpriseGuiManager;
import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterpriseManager;
import com.crystalville.crystalcore.managers.EnterprisePermission;
import com.crystalville.crystalcore.managers.EnterpriseTransaction;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class EnterpriseGuiListener implements Listener {

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd/MM HH:mm");

    private final EnterpriseGuiManager guiManager;
    private final EnterpriseManager enterpriseManager;
    private final BankManager bankManager;
    private final RankManager rankManager;

    public EnterpriseGuiListener(EnterpriseGuiManager guiManager, EnterpriseManager enterpriseManager,
                                  BankManager bankManager, RankManager rankManager) {
        this.guiManager = guiManager;
        this.enterpriseManager = enterpriseManager;
        this.bankManager = bankManager;
        this.rankManager = rankManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof EnterpriseGuiHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        EnterpriseGuiHolder guiHolder = (EnterpriseGuiHolder) holder;

        EnterpriseAccount account = enterpriseManager.get(guiHolder.getEnterpriseId());
        if (account == null) {
            player.closeInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean topInv = rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize();

        if (!topInv) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
            }
            return;
        }

        if (EnterpriseGuiManager.isDepositSlot(rawSlot)) {
            handleDepositSlotClick(event, player);
            return;
        }

        event.setCancelled(true);

        if (rawSlot == EnterpriseGuiManager.CONFIRM_DEPOSIT_SLOT) {
            handleConfirmDeposit(event.getInventory(), player, account);
        } else if (rawSlot == EnterpriseGuiManager.WITHDRAW_100_SLOT) {
            handleWithdraw(event.getInventory(), player, account, 100);
        } else if (rawSlot == EnterpriseGuiManager.WITHDRAW_500_SLOT) {
            handleWithdraw(event.getInventory(), player, account, 500);
        } else if (rawSlot == EnterpriseGuiManager.WITHDRAW_ALL_SLOT) {
            handleWithdraw(event.getInventory(), player, account, account.balance);
        } else if (rawSlot == EnterpriseGuiManager.HISTORY_SLOT) {
            handleHistory(player, account);
        } else if (rawSlot == EnterpriseGuiManager.MEMBERS_SLOT) {
            handleMembers(player, account);
        } else if (rawSlot == EnterpriseGuiManager.CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void handleDepositSlotClick(InventoryClickEvent event, Player player) {
        ItemStack cursor = event.getCursor();
        boolean placingNonCrystal = cursor != null && cursor.getType() != Material.AIR
                && cursor.getType() != CrystalItemUtil.CURRENCY_MATERIAL;
        if (placingNonCrystal) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Only Crystals can be placed here.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnterpriseGuiHolder)) return;
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
        if (!(event.getInventory().getHolder() instanceof EnterpriseGuiHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        for (int slot : EnterpriseGuiManager.DEPOSIT_SLOTS) {
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

    private void handleConfirmDeposit(Inventory inv, Player player, EnterpriseAccount account) {
        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.DEPOSIT)) {
            player.sendMessage(Component.text("You don't have permission to deposit here.", NamedTextColor.RED));
            return;
        }

        int total = 0;
        for (int slot : EnterpriseGuiManager.DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }

        if (total <= 0) {
            player.sendMessage(Component.text("Place some Crystals in the slots above first.", NamedTextColor.YELLOW));
            return;
        }

        long added = account.addBalance(total);
        int remainingToRemove = (int) added;
        for (int slot : EnterpriseGuiManager.DEPOSIT_SLOTS) {
            if (remainingToRemove <= 0) break;
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() != CrystalItemUtil.CURRENCY_MATERIAL) continue;
            int amount = item.getAmount();
            if (amount <= remainingToRemove) {
                remainingToRemove -= amount;
                inv.setItem(slot, null);
            } else {
                item.setAmount(amount - remainingToRemove);
                remainingToRemove = 0;
            }
        }

        account.addTransaction(player.getName(), "DEPOSIT", added);
        enterpriseManager.save();

        player.sendMessage(Component.text("Deposited " + added + " Crystal(s) into " + account.name + ".",
                NamedTextColor.GREEN));
        if (added < total) {
            player.sendMessage(Component.text((total - added) + " Crystal(s) couldn't be deposited - limit reached.",
                    NamedTextColor.YELLOW));
        }

        guiManager.refresh(inv, player, account);
    }

    private void handleWithdraw(Inventory inv, Player player, EnterpriseAccount account, long amount) {
        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.WITHDRAW)) {
            player.sendMessage(Component.text("You don't have permission to withdraw here.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Nothing to withdraw.", NamedTextColor.YELLOW));
            return;
        }

        long withdrawn = account.removeBalance(amount);
        if (withdrawn <= 0) {
            player.sendMessage(Component.text(account.name + " has no funds to withdraw.", NamedTextColor.YELLOW));
            return;
        }

        account.addTransaction(player.getName(), "WITHDRAWAL", withdrawn);
        enterpriseManager.save();

        payoutCrystals(player, (int) withdrawn);

        player.sendMessage(Component.text("Withdrew " + withdrawn + " Crystal(s) from " + account.name + ".",
                NamedTextColor.GREEN));

        guiManager.refresh(inv, player, account);
    }

    private void handleHistory(Player player, EnterpriseAccount account) {
        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.VIEW_HISTORY)) {
            player.sendMessage(Component.text("You don't have permission to view history.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("=== " + account.name + " History ===", NamedTextColor.AQUA));
        if (account.history.isEmpty()) {
            player.sendMessage(Component.text("No transactions yet.", NamedTextColor.GRAY));
            return;
        }
        List<EnterpriseTransaction> history = account.history;
        int start = Math.max(0, history.size() - 10);
        for (int i = history.size() - 1; i >= start; i--) {
            EnterpriseTransaction tx = history.get(i);
            String sign = tx.type.equals("WITHDRAWAL") ? "-" : "+";
            player.sendMessage(Component.text("[" + TIME_FORMAT.format(new Date(tx.timestampMillis)) + "] "
                    + tx.playerName + " " + sign + tx.amount + " (" + tx.type + ")", NamedTextColor.GRAY));
        }
    }

    private void handleMembers(Player player, EnterpriseAccount account) {
        player.sendMessage(Component.text("=== " + account.name + " Members ===", NamedTextColor.AQUA));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(account.ownerUuid);
        player.sendMessage(Component.text((owner.getName() != null ? owner.getName() : "Unknown")
                + " - OWNER", NamedTextColor.GOLD));
        for (var entry : account.members.entrySet()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
            player.sendMessage(Component.text((member.getName() != null ? member.getName() : "Unknown")
                    + " - " + entry.getValue(), NamedTextColor.WHITE));
        }
    }

    private void payoutCrystals(Player player, int totalAmount) {
        int inventoryCapacity = CrystalItemUtil.freeCapacity(player);
        int toInventory = Math.min(totalAmount, inventoryCapacity);
        int remainder = totalAmount - toInventory;

        if (toInventory > 0) {
            CrystalItemUtil.giveCrystals(player, toInventory);
        }
        if (remainder <= 0) return;

        boolean unlimited = player.isOp() || rankManager.hasRole(player.getUniqueId(), FINANCE_MINISTER_ROLE);
        long depositedToBank = bankManager.deposit(player.getUniqueId(), remainder, unlimited);
        long stillOverflow = remainder - depositedToBank;

        if (depositedToBank > 0) {
            player.sendMessage(Component.text("Your inventory was full - " + depositedToBank
                    + " Crystal(s) went to your personal bank.", NamedTextColor.AQUA));
        }
        if (stillOverflow > 0) {
            CrystalItemUtil.giveCrystals(player, stillOverflow);
            player.sendMessage(Component.text("Your personal bank is also full - " + stillOverflow
                    + " Crystal(s) were dropped at your feet.", NamedTextColor.YELLOW));
        }
    }
  }
