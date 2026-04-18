package com.pocketsave.app

import android.net.Uri

/**
 * Port of iOS `handleDeepLink(_:)` from `PocketSave/App/GrockApp.swift`.
 *
 * iOS supports:
 *   - `pocketsave://quickadd/{cartId}/{itemId}` — open a shopping cart and
 *     stage a pending item add (sets `CartStateManager.sharedPendingWidgetItemId`).
 *   - `pocketsave://finishtrip/{cartId}` — open a shopping cart and present
 *     the finish-trip sheet.
 *
 * Android mirrors the routes by navigating to [Routes.CART_DETAIL] and
 * passing the pending action via [PendingDeepLink]. [PocketSaveNavHost]
 * reads the pending action and drives the detail screen accordingly.
 */
object DeepLink {
    const val SCHEME = "pocketsave"
    const val HOST_QUICK_ADD = "quickadd"
    const val HOST_FINISH_TRIP = "finishtrip"

    fun parse(uri: Uri?): PendingDeepLink? {
        if (uri == null || uri.scheme != SCHEME) return null
        val segments = uri.pathSegments
        return when (uri.host) {
            HOST_QUICK_ADD -> {
                // pocketsave://quickadd/<cartId>/<itemId>
                val cartId = segments.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
                val itemId = segments.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
                PendingDeepLink.QuickAdd(cartId = cartId, itemId = itemId)
            }
            HOST_FINISH_TRIP -> {
                val cartId = segments.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
                PendingDeepLink.FinishTrip(cartId = cartId)
            }
            else -> null
        }
    }
}

sealed interface PendingDeepLink {
    val cartId: String

    data class QuickAdd(override val cartId: String, val itemId: String) : PendingDeepLink
    data class FinishTrip(override val cartId: String) : PendingDeepLink
}
