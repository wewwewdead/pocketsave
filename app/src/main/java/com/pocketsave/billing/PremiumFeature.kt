package com.pocketsave.billing

/**
 * Enumerates every gate in the app. Each feature carries:
 *  - a stable [key] used for nav arguments and analytics
 *  - trigger-specific paywall copy so the hero reads like "here's why you're
 *    seeing this now", not a generic upsell
 *
 * **Never** add a gate without adding a case here first. That way the paywall,
 * the gate helper, and the deep-link layer all know about it at once.
 *
 * Categories:
 *  - *Count-based* features (`CreateActiveTrip`, `AddVaultItem`,
 *    `AddCustomCategory`, `DeepHistory`) are evaluated against caps in
 *    [FeatureLimits]. Free users hit them as their usage grows.
 *  - *Feature-based* gates (`Scanner`, `CartTheme`, `TripShareCard`, `Widget`)
 *    are binary: either the user is Pro or they aren't.
 */
sealed class PremiumFeature(
    val key: String,
    val copy: TriggerCopy,
) {

    data object CreateActiveTrip : PremiumFeature(
        key = "create_active_trip",
        copy = TriggerCopy(
            heroSubtitle = "Two live trips is the free limit. Go Pro to plan a full week of shops side-by-side.",
        ),
    )

    data object AddVaultItem : PremiumFeature(
        key = "add_vault_item",
        copy = TriggerCopy(
            heroSubtitle = "Your free vault is full. Go Pro to remember every item, at every store, forever.",
        ),
    )

    data object AddCustomCategory : PremiumFeature(
        key = "add_custom_category",
        copy = TriggerCopy(
            heroSubtitle = "You've used both free custom categories. Go Pro for unlimited — organise every aisle your way.",
        ),
    )

    data object Scanner : PremiumFeature(
        key = "scanner",
        copy = TriggerCopy(
            heroSubtitle = "Point your camera at a price tag — Pro fills in the name, price, and package size for you.",
        ),
    )

    data object CartTheme : PremiumFeature(
        key = "cart_theme",
        copy = TriggerCopy(
            heroSubtitle = "Cart colours and photo backdrops. Pro makes every trip feel like yours.",
        ),
    )

    data object TripShareCard : PremiumFeature(
        key = "trip_share_card",
        copy = TriggerCopy(
            heroSubtitle = "Finish a trip, send a polished receipt. Pro unlocks the share card.",
        ),
    )

    data object Widget : PremiumFeature(
        key = "widget",
        copy = TriggerCopy(
            heroSubtitle = "Your current cart, live on your home screen. Pro unlocks the widget.",
        ),
    )

    data object DeepHistory : PremiumFeature(
        key = "deep_history",
        copy = TriggerCopy(
            heroSubtitle = "Every trip you've ever completed, with budget deltas and spending trends. Pro unlocks your full history.",
        ),
    )

    companion object {
        fun fromKey(key: String?): PremiumFeature? = when (key) {
            CreateActiveTrip.key -> CreateActiveTrip
            AddVaultItem.key -> AddVaultItem
            AddCustomCategory.key -> AddCustomCategory
            Scanner.key -> Scanner
            CartTheme.key -> CartTheme
            TripShareCard.key -> TripShareCard
            Widget.key -> Widget
            DeepHistory.key -> DeepHistory
            else -> null
        }
    }
}

/**
 * Copy surfaced on the paywall when it's opened from a specific trigger.
 * Only the subtitle varies — the title ("PocketSave Pro") stays constant so
 * users can tell they're looking at the same product no matter how they
 * got here.
 */
data class TriggerCopy(
    val heroSubtitle: String,
)
