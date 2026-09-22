package com.crystalville.crystalcore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Marker holder identifying an open Crystal Bank GUI inventory, so
 * BankGuiListener can distinguish it from any other inventory (chests,
 * other plugins' GUIs, etc) purely by type-checking the holder.
 */
public class BankGuiHolder implements InventoryHolder {

    private final UUID viewerUuid;
    private Inventory inventory;

    public BankGuiHolder(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }
  }
