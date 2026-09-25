package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.gui.SellGuiHolder;
import com.crystalville.crystalcore.gui.SellGuiManager;
import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.ShopManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class SellGuiListener implements Listener {

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";

    private final SellGuiManager sellGuiManager;
    private final ShopManager shopManager;
    private final BankManager bankManager;
    private final RankManager rankManager;

    public SellGuiListener(SellGuiManager sellGuiManager, ShopManager shopManager,
                            BankManager bankManager, RankManager rankManager) {
        this.sellGuiManager = sellGuiManager;
        this.shopManager = shopManager;
        this.bankManager = bankManager;
        this.rankManager = rankManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SellGuiHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        SellGuiHolder guiHolder = (SellGuiHolder) holder;

        int rawSlot = event.getRawSlot();
        boolean topInv = rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize();
        if (!topInv) return;

        event.setCancelled(true);

        if (rawSlot == SellGuiManager.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (rawSlot == SellGuiManager.PREV_PAGE_SLOT) {
            player.openInventory(sellGuiManager.open(player, guiHolder.getPage() - 1));
            return;
        }

        if (rawSlot == SellGuiManager.NEXT_PAGE_SLOT) {
            player.openInventory(sellGuiManager.open(player, guiHolder.getPage() + 1));
            return;
        }

        if (rawSlot >= SellGuiManager.ITEMS_PER_PAGE) return;

        Material material = sellGuiManager.getMaterialAt(guiHolder.getPage(), rawSlot);
        if (material == null || material == CrystalItemUtil.CURRENCY_MATERIAL) return;

        sellAllOwned(player, material);

        player.openInventory(sellGuiManager.open(player, guiHolder.getPage()));
    }

    private void sellAllOwned(Player player, Material material) {
        int owned = countItems(player.getInventory(), material);
        if (owned <= 0) {
            player.sendMessage(Component.text("You don't have any of that to sell.", NamedTextColor.YELLOW));
            return;
        }

        int sellPrice = shopManager.getSellPrice(material);
        int totalPayout = sellPrice * owned;

        removeItems(player.getInventory(), material, owned);
        payoutCrystals(player, totalPayout);

        player.sendMessage(Component.text("Sold " + owned + "x " + prettyName(material)
                + " for " + totalPayout + " Crystal(s).", NamedTextColor.GREEN));
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
                    + " Crystal(s) were deposited into your bank.", NamedTextColor.AQUA));
        }
        if (stillOverflow > 0) {
            CrystalItemUtil.giveCrystals(player, stillOverflow);
            player.sendMessage(Component.text("Your bank is also full - " + stillOverflow
                    + " Crystal(s) were dropped at your feet.", NamedTextColor.YELLOW));
        }
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private int countItems(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) total += item.getAmount();
        }
        return total;
    }

    private void removeItems(Inventory inventory, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
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
