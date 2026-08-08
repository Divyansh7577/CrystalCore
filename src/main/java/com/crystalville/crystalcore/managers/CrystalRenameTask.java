package com.crystalville.crystalcore.managers;

import com.crystalville.crystalcore.util.CrystalItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CrystalRenameTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            renamePlayerInventory(player);
        }
    }

    public static void renamePlayerInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            CrystalItemUtil.applyCrystalMeta(item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            CrystalItemUtil.applyCrystalMeta(item);
        }
        CrystalItemUtil.applyCrystalMeta(player.getInventory().getItemInOffHand());
    }
  }
