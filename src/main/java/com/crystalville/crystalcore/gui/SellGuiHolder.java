package com.crystalville.crystalcore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class SellGuiHolder implements InventoryHolder {

    private final UUID viewerUuid;
    private final int page;
    private Inventory inventory;

    public SellGuiHolder(UUID viewerUuid, int page) {
        this.viewerUuid = viewerUuid;
        this.page = page;
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

    public int getPage() {
        return page;
    }
}
