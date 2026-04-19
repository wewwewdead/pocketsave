package com.pocketsave.billing

/**
 * Free-tier caps. Every count-based gate in [PaywallGate] reads from here so
 * tuning the product only takes one edit.
 *
 * Design notes:
 *  - Caps apply to *creating more*, never to *reading what's already there*.
 *    A Pro user who cancels keeps all their trips / items / categories and
 *    can still view, edit, and delete them — they just can't add beyond the
 *    free limit until they re-subscribe.
 *  - Onboarding already creates 1 store + 1 item + 1 cart. The numbers below
 *    assume those seed rows are part of the free headroom.
 *  - [FREE_HISTORY_ROWS] is a display cap, not a retention cap. Data is
 *    never deleted; the History screen simply shows an "unlock full history"
 *    row after the Nth completed trip.
 */
object FeatureLimits {

    /**
     * Maximum concurrently active (planning + shopping) carts for a free user.
     * Completed carts don't count — finishing a trip always frees a slot.
     */
    const val FREE_ACTIVE_CARTS: Int = 2

    /** Maximum items saved in the vault for a free user. */
    const val FREE_VAULT_ITEMS: Int = 10

    /**
     * Maximum **custom** categories for a free user. The default
     * [com.pocketsave.domain.model.GroceryCategory] set is never counted
     * toward this — defaults are always available.
     */
    const val FREE_CUSTOM_CATEGORIES: Int = 2

    /**
     * Number of completed trips the History screen renders for a free user.
     * Older trips are still persisted and restored if the user upgrades.
     */
    const val FREE_HISTORY_ROWS: Int = 10
}
