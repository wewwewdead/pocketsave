package com.pocketsave.core.budget

import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.domain.model.CartStatus
import java.util.Calendar
import java.util.Date

/**
 * Sums spend across every cart whose `createdAt` falls in the calendar month
 * containing [now]. Per-cart spend is delegated to [computeSpent] so this
 * stays pure (no [com.pocketsave.core.service.VaultService] dependency) and
 * the inline composable callers can pass `vaultService::computeTotalSpent`
 * directly. Tests can stub the lambda with a fixed value table.
 *
 * Month boundary uses the device's default [java.util.TimeZone] — same as
 * the inline call sites used to do — so a cart created at 11:59 PM local
 * time on Jan 31 stays in January even if UTC has already rolled over.
 */
fun monthlySpendIn(
    carts: List<CartEntity>,
    itemsByCart: Map<String, List<CartItemEntity>>,
    now: Date = Date(),
    computeSpent: (CartStatus, List<CartItemEntity>) -> Double,
): Double {
    val cal = Calendar.getInstance().apply { time = now }
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH)
    var total = 0.0
    for (cart in carts) {
        cal.time = cart.createdAt
        if (cal.get(Calendar.YEAR) != nowYear) continue
        if (cal.get(Calendar.MONTH) != nowMonth) continue
        val items = itemsByCart[cart.id].orEmpty()
        total += computeSpent(CartStatus.fromRaw(cart.status), items)
    }
    return total
}
