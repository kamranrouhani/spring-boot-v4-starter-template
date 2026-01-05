package com.kamran.template.user;

/**
 * Subscription tiers - defines WHAT features the user has access to.
 * <p>
 * This is separate from system roles (who they are).
 * <p>
 * FREE: Basic features, no payment required
 * PRO: Enhanced features, paid subscription
 * PREMIUM: All features, highest paid subscription
 */
public enum SubscriptionTier {
    /**
     * Free tier - default for new users.
     * Access to basic features only.
     */
    FREE,

    /**
     * Pro tier - first paid subscription.
     * Enhanced features like:
     * - Advanced analytics
     * - More storage
     * - Priority support
     */
    PRO,

    /**
     * Premium tier - highest subscription level.
     * All features including:
     * - AI-powered suggestions
     * - Unlimited storage
     * - White-label options
     */
    PREMIUM
}