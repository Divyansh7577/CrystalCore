package com.crystalville.crystalcore.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending /payenterprise confirmations (in-memory only - these are
 * meant to be short-lived and don't need to survive a restart). A
 * confirmation expires after CONFIRMATION_WINDOW_MILLIS if not confirmed,
 * so an accidental /payenterprise never silently completes.
 */
public class EnterprisePaymentConfirmationManager {

    public static final long CONFIRMATION_WINDOW_MILLIS = 30_000L;

    public static final class PendingPayment {
        public final String enterpriseId;
        public final long amount;
        public final long expiresAtMillis;

        public PendingPayment(String enterpriseId, long amount, long expiresAtMillis) {
            this.enterpriseId = enterpriseId;
            this.amount = amount;
            this.expiresAtMillis = expiresAtMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private final Map<UUID, PendingPayment> pending = new HashMap<>();

    public void set(UUID uuid, String enterpriseId, long amount) {
        pending.put(uuid, new PendingPayment(
                enterpriseId, amount, System.currentTimeMillis() + CONFIRMATION_WINDOW_MILLIS));
    }

    public PendingPayment get(UUID uuid) {
        return pending.get(uuid);
    }

    public void clear(UUID uuid) {
        pending.remove(uuid);
    }
  }
