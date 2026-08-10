package com.crystalville.crystalcore.managers;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which OPs currently have Hole Filler Mode active, and which block
 * type each of them has selected to fill holes with (defaults to STONE).
 * In-memory only; resets on server restart.
 */
public class HoleFillerManager {

    private final Set<UUID> enabled = new HashSet<>();
    private final Map<UUID, Material> fillMaterial = new HashMap<>();

    public boolean toggle(UUID uuid) {
        if (enabled.contains(uuid)) {
            enabled.remove(uuid);
            return false;
        } else {
            enabled.add(uuid);
            return true;
        }
    }

    public void setEnabled(UUID uuid, boolean value) {
        if (value) {
            enabled.add(uuid);
        } else {
            enabled.remove(uuid);
        }
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    public void setMaterial(UUID uuid, Material material) {
        fillMaterial.put(uuid, material);
    }

    public Material getMaterial(UUID uuid) {
        return fillMaterial.getOrDefault(uuid, Material.STONE);
    }
    }
