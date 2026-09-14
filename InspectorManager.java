package com.crystalville.crystalcore.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks which OPs currently have Chest Inspector Mode (/i) active. */
public class InspectorManager {

    private final Set<UUID> inspecting = new HashSet<>();

    public boolean toggle(UUID uuid) {
        if (inspecting.contains(uuid)) {
            inspecting.remove(uuid);
            return false;
        } else {
            inspecting.add(uuid);
            return true;
        }
    }

    public boolean isInspecting(UUID uuid) {
        return inspecting.contains(uuid);
    }
}
