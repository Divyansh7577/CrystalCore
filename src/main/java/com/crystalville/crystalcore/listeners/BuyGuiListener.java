package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.gui.BuyGuiHolder;
import com.crystalville.crystalcore.gui.BuyGuiManager;
import com.crystalville.crystalcore.managers.OpBypassManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.ShopManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the Buy GUI: normal click buys 1, shift-click buys a full stack.
 * Same permission rules as /buy. OPs bypass the Crystal cost, UNLESS
 * they've disabled it via /opbypass off.
 */
public class BuyGuiListener implements Listener {

    private static final String MARKET_MINISTER_ROLE = "MARKET_MINISTER";

    private final BuyGuiManager buyGuiManager;
    private final ShopManager shopManager;
    private final RankManager rankManager;
    private final OpBypassManager opBypassManager;

    public BuyGuiListener(BuyGuiManager buyGuiManager, ShopManager shopManager, RankManager rankManager,
                           OpBypassManager opBypassManager) {
        this.buyGuiManager = buyGuiManager;
        this.shopManager = shopManager;
        this.rankManager = rankManager;
        this.opBypassManager = opBypassManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BuyGuiHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        BuyGuiHolder guiHolder = (BuyGuiHolder) holder;

        int rawSlot = event.getRawSlot();
        boolean topInv = rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize();
        if (!topInv) return;

        event.setCancelled(true);

        if (rawSlot == BuyGuiManager.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (rawSlot == BuyGuiManager.PREV_PAGE_SLOT) {
            player.openInventory(buyGuiManager.open(player, guiHolder.getPage() - 1));
            return;
        }
        if (rawSlot == BuyGuiManager.NEXT_PAGE_SLOT) {
            player.openInventory(buyGuiManager.open(player, guiHolder.getPage() + 1));
            return;
        }
        if (rawSlot >= BuyGuiManager.ITEMS_PER_PAGE) return;

        Material material = buyGuiManager.getMaterialAt(guiHolder.getPage(), rawSlot);
        if (material == null) return;

        boolean isMarketMinister = rankManager.hasRole(player.getUniqueId(), MARKET_MINISTER_ROLE);
        if (!player.isOp() && !isMarketMinister) {
            player.sendMessage(Component.text(
                    "Only the Market Minister is authorized to buy items from this shop.", NamedTextColor.RED));
            return;
        }

        int quantity = (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
                ? material.getMaxStackSize()
                : 1;

        buyItem(player, material, quantity);
    }

    private void buyItem(Player player, Material material, int quantity) {
        boolean bypass = opBypassManager.hasBypass(player);
        int buyPrice = shopManager.getBuyPrice(material);
        int totalCost = buyPrice * quantity;

        if (!bypass) {
            int have = countCrystals(player.getInventory());
            if (have < totalCost) {
                player.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + totalCost + ", You have: " + have,
                        NamedTextColor.RED));
                return;
            }
            removeCrystals(player.getInventory(), totalCost);
        }

        giveItems(player, material, quantity);

        if (bypass) {
            player.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Purchased " + quantity + "x " + prettyName(material)
                            + " for free.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Purchased " + quantity + "x " + prettyName(material)
                    + " for " + totalCost + " Crystal(s).", NamedTextColor.GREEN));
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

    private void giveItems(Player player, Material material, int amount) {
        int maxStack = material.getMaxStackSize();
        List<ItemStack> overflow = new ArrayList<>();
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(material, stackSize);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            overflow.addAll(leftover.values());
            remaining -= stackSize;
        }

        for (ItemStack leftover : overflow) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
                         }
