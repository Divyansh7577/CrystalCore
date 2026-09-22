package com.crystalville.crystalcore.managers;

/** The five permissions an Enterprise Bank member can be granted, independent of each other. */
public enum EnterprisePermission {
    VIEW_BALANCE,
    DEPOSIT,
    WITHDRAW,
    VIEW_HISTORY,
    MANAGE_MEMBERS;

    public static EnterprisePermission fromString(String input) {
        if (input == null) {
            return null;
        }
        try {
            return EnterprisePermission.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    }
