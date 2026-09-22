package com.crystalville.crystalcore.managers;

/**
 * A single recorded Enterprise transaction: who did it, what kind, how much,
 * when, and the resulting balance. Serialized as a pipe-delimited string
 * for storage in enterprises.yml.
 */
public final class EnterpriseTransaction {

    public final long timestampMillis;
    public final String playerName;
    public final String type; // "PAYMENT", "DEPOSIT", or "WITHDRAWAL"
    public final long amount;
    public final long balanceAfter;

    public EnterpriseTransaction(long timestampMillis, String playerName, String type, long amount, long balanceAfter) {
        this.timestampMillis = timestampMillis;
        this.playerName = playerName;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public String serialize() {
        return timestampMillis + "|" + playerName + "|" + type + "|" + amount + "|" + balanceAfter;
    }

    public static EnterpriseTransaction deserialize(String raw) {
        String[] parts = raw.split("\\|", 5);
        if (parts.length != 5) {
            return null;
        }
        try {
            long ts = Long.parseLong(parts[0]);
            String player = parts[1];
            String type = parts[2];
            long amount = Long.parseLong(parts[3]);
            long balanceAfter = Long.parseLong(parts[4]);
            return new EnterpriseTransaction(ts, player, type, amount, balanceAfter);
        } catch (Exception e) {
            return null;
        }
    }
      }
