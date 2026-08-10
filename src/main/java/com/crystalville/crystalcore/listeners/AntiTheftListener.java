package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.managers.ChestLogManager;
import com.crystalville.crystalcore.managers.InspectorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-theft protection: while a player has Inspector Mode active (/i), any
 * attempt to open a chest/barrel/shulker box is intercepted and replaced
 * with a printed history of that container's activity instead of opening
 * the GUI. For everyone else, chest openings are silently snapshotted so
 * that when they close it, the net change can be logged.
 */
public class AntiTheftListener implements Listener {

    private static final class OpenSession {
        final String locationKey;
        final ItemStack[] snapshot;

        OpenSession(String locationKey, ItemStack[] snapshot) {
            this.locationKey = locationKey;
            this.snapshot = snapshot;
        }
    }

    private final ChestLogManager chestLogManager;
    private final InspectorManager inspectorManager;
    private final Map<UUID, OpenSession> openSessions = new HashMap<>();

    public AntiTheftListener(ChestLogManager chestLogManager, InspectorManager inspectorManager) {
        this.chestLogManager = chestLogManager;
        this.inspectorManager = inspectorManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        InventoryHolder holder = event.getInventory().getHolder();
        String locationKey = ChestLogManager.resolveKey(holder);
        if (locationKey == null) {
            return; // not a chest-like container we track
        }

        if (inspectorManager.isInspecting(player.getUniqueId())) {
            event.setCancelled(true);
            chestLogManager.sendLogsTo(player, locationKey);
            return;
        }

        openSessions.put(player.getUniqueId(),
                new OpenSession(locationKey, event.getInventory().getContents().clone()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        OpenSession session = openSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        ItemStack[] after = event.getInventory().getContents().clone();
        chestLogManager.recordDiff(session.locationKey, player.getName(), session.snapshot, after);
    }
  }
