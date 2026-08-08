package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.managers.CrystalRenameTask;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class CrystalListener implements Listener {

    private final RankManager rankManager;

    public CrystalListener(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String prefix = rankManager.getFormattedPrefix(player.getUniqueId());

        if (prefix.isEmpty()) {
            return;
        }

        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                prefixComponent
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        String prefix = rankManager.getFormattedPrefix(player.getUniqueId());
        if (!prefix.isEmpty()) {
            Component displayName = LegacyComponentSerializer.legacySection().deserialize(prefix)
                    .append(Component.text(player.getName()));
            player.playerListName(displayName);
        }

        CrystalRenameTask.renamePlayerInventory(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Item groundItem = event.getItem();
        if (groundItem == null) {
            return;
        }
        CrystalItemUtil.applyCrystalMeta(groundItem.getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        CrystalItemUtil.applyCrystalMeta(event.getCurrentItem());
        CrystalItemUtil.applyCrystalMeta(event.getCursor());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        CrystalItemUtil.applyCrystalMeta(event.getCurrentItem());
    }
  }
