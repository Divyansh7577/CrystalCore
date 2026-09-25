package com.crystalville.crystalcore.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Resolves a player by name WITHOUT ever making a network call to Mojang.
 * Checks online players first, then this server's own local player cache
 * (populated purely from local data). Avoids the silent hangs/failures
 * that Bukkit.getOfflinePlayer(String) can trigger for uncached names.
 */
public final class PlayerResolver {

    private PlayerResolver() {
    }

    public static OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }

        for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
            if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }

        return null;
    }
    }
