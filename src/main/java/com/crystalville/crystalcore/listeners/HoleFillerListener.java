package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.managers.HoleFillerManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Hole Filler Mode: while active for a player (OP only, toggled via
 * /holefiller), any air block directly beneath their feet as they walk
 * is automatically filled with their selected block (default STONE).
 */
public class HoleFillerListener implements Listener {

    private final HoleFillerManager holeFillerManager;

    public HoleFillerListener(HoleFillerManager holeFillerManager) {
        this.holeFillerManager = holeFillerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!holeFillerManager.isEnabled(player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Block below = to.clone().subtract(0, 1, 0).getBlock();
        Material type = below.getType();

        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
            Material fillMaterial = holeFillerManager.getMaterial(player.getUniqueId());
            below.setType(fillMaterial);
        }
    }
          }
