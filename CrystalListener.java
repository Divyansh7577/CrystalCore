package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.managers.CrystalRenameTask;
import com.crystalville.crystalcore.managers.MailboxManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
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
    private final MailboxManager mailboxManager;

    public CrystalListener(RankManager rankManager, MailboxManager mailboxManager) {
        this.rankManager = rankManager;
        this.mailboxManager = mailboxManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component prefix = rankManager.getFormattedPrefixComponent(player.getUniqueId());

        if (prefix.equals(Component.empty())) {
            return;
        }

        event.renderer((source, sourceDisplayName, message, viewer) ->
                prefix
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Component prefix = rankManager.getFormattedPrefixComponent(player.getUniqueId());
        if (!prefix.equals(Component.empty())) {
            Component displayName = prefix.append(Component.text(player.getName()));
            player.playerListName(displayName);
        }

        CrystalRenameTask.renamePlayerInventory(player);

        // Deliver any Crystals sent via /pay while this player was offline.
        mailboxManager.deliverIfPending(player);
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
