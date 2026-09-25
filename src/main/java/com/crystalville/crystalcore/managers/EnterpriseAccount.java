package com.crystalville.crystalcore.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EnterpriseAccount {

    public static final long DEFAULT_MAX_BALANCE = 200_000L;
    private static final int MAX_HISTORY_ENTRIES = 50;

    public final String enterpriseId;
    public String name;
    public final UUID ownerUuid;
    public long balance;
    public long maxBalance;
    public final Map<UUID, Set<EnterprisePermission>> members = new LinkedHashMap<>();
    public final List<EnterpriseTransaction> history = new ArrayList<>();

    public EnterpriseAccount(String enterpriseId, String name, UUID ownerUuid) {
        this.enterpriseId = enterpriseId;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.balance = 0L;
        this.maxBalance = DEFAULT_MAX_BALANCE;
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return isOwner(uuid) || members.containsKey(uuid);
    }

    public boolean hasPermission(UUID uuid, EnterprisePermission permission) {
        if (isOwner(uuid)) {
            return true;
        }
        Set<EnterprisePermission> perms = members.get(uuid);
        return perms != null && perms.contains(permission);
    }

    public void addTransaction(String playerName, String type, long amount) {
        history.add(new EnterpriseTransaction(System.currentTimeMillis(), playerName, type, amount, balance));
        while (history.size() > MAX_HISTORY_ENTRIES) {
            history.remove(0);
        }
    }

    /** Adds to balance, capped at maxBalance (which can be raised via /enterprise increase). */
    public long addBalance(long amount) {
        long room = Math.max(0, maxBalance - balance);
        long added = Math.min(amount, room);
        balance += added;
        return added;
    }

    public long removeBalance(long amount) {
        long removed = Math.min(amount, balance);
        balance -= removed;
        return removed;
    }
}