package com.crystalville.crystalcore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class EnterpriseGuiHolder implements InventoryHolder {

    private final UUID viewerUuid;
    private final String enterpriseId;
    private Inventory inventory;

    public EnterpriseGuiHolder(UUID viewerUuid, String enterpriseId) {
        this.viewerUuid = viewerUuid;
        this.enterpriseId = enterpriseId;
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

    public String getEnterpriseId() {
        return enterpriseId;
    }
}
