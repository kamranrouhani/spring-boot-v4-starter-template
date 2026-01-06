package com.kamran.template.user;

/**
 * System roles - defines WHO the user is in the system.
 *
 * This is separate from subscription tiers (what they pay for).
 *
 * USER: Regular user of the system
 * ADMIN: System administrator with special privileges
 *
 * Future: Could add MODERATOR, SUPPORT_AGENT, etc.
 */
public enum Role {
    /**
     * Regular user - default for all new registrations.
     * Can use the system based on their subscription tier.
     */
    USER,

    /**
     * System administrator - special system privileges.
     * Can manage users, view all data, access admin panel.
     * Note: Admins still have a subscription tier (usually FREE or granted PREMIUM).
     */
    ADMIN
}
