package com.pocketsave.core.service

import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.domain.model.CartStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

/**
 * Regression coverage for [VaultService.computeTotalSpent].
 *
 * `computeTotalSpent` is invoked on a [VaultService] instance but the method
 * itself doesn't touch the database — it's a pure switch over the status +
 * cart item fields. Rather than bootstrap Room, we instantiate the service
 * with a Mockito proxy over [com.pocketsave.data.local.db.PocketSaveDatabase].
 */
class VaultServiceTotalsTest {

    private val service: VaultService by lazy {
        val db = Mockito.mock(com.pocketsave.data.local.db.PocketSaveDatabase::class.java)
        VaultService(db)
    }

    private fun vaultItem(
        uid: String = "row-1",
        itemId: String = "item-1",
        quantity: Double = 1.0,
        plannedPrice: Double? = null,
        plannedUnit: String? = null,
        actualPrice: Double? = null,
        actualQuantity: Double? = null,
        actualUnit: String? = null,
        isFulfilled: Boolean = false,
        isSkipped: Boolean = false,
        wasEdited: Boolean = false,
        isShoppingOnly: Boolean = false,
        shoppingOnlyPrice: Double? = null,
    ): CartItemEntity = CartItemEntity(
        uid = uid,
        cartId = "cart-1",
        itemId = itemId,
        addedAt = Date(),
        quantity = quantity,
        isFulfilled = isFulfilled,
        isSkippedDuringShopping = isSkipped,
        plannedStore = "Store A",
        plannedPrice = plannedPrice,
        plannedUnit = plannedUnit,
        actualStore = null,
        actualPrice = actualPrice,
        actualQuantity = actualQuantity,
        actualUnit = actualUnit,
        wasEditedDuringShopping = wasEdited,
        isShoppingOnlyItem = isShoppingOnly,
        shoppingOnlyPrice = shoppingOnlyPrice,
    )

    @Test
    fun `planning total uses planned price times quantity`() {
        val items = listOf(
            vaultItem(plannedPrice = 2.5, quantity = 3.0),
            vaultItem(uid = "row-2", itemId = "item-2", plannedPrice = 4.0, quantity = 2.0),
        )
        val total = service.computeTotalSpent(CartStatus.PLANNING, items)
        assertEquals(2.5 * 3 + 4.0 * 2, total, 1e-9)
    }

    @Test
    fun `planning total falls back to zero when plannedPrice null`() {
        val items = listOf(vaultItem(plannedPrice = null, quantity = 5.0))
        assertEquals(0.0, service.computeTotalSpent(CartStatus.PLANNING, items), 1e-9)
    }

    @Test
    fun `planning total uses shopping-only price path`() {
        val item = vaultItem(
            isShoppingOnly = true,
            shoppingOnlyPrice = 7.25,
            quantity = 2.0,
        )
        assertEquals(7.25 * 2, service.computeTotalSpent(CartStatus.PLANNING, listOf(item)), 1e-9)
    }

    @Test
    fun `shopping total skips items flagged skipped`() {
        val items = listOf(
            vaultItem(plannedPrice = 5.0, quantity = 1.0, isFulfilled = true, actualPrice = 6.0),
            vaultItem(uid = "row-2", itemId = "item-2", plannedPrice = 10.0, quantity = 2.0, isSkipped = true),
        )
        // Only the first row counts — actualPrice 6.0 × 1.0.
        assertEquals(6.0, service.computeTotalSpent(CartStatus.SHOPPING, items), 1e-9)
    }

    @Test
    fun `shopping total respects wasEdited without fulfilment`() {
        val items = listOf(
            vaultItem(
                plannedPrice = 5.0,
                quantity = 1.0,
                actualPrice = 6.5,
                actualQuantity = 2.0,
                wasEdited = true,
            ),
        )
        assertEquals(6.5 * 2.0, service.computeTotalSpent(CartStatus.SHOPPING, items), 1e-9)
    }

    @Test
    fun `shopping total uses plannedPrice when unfulfilled and not edited`() {
        val items = listOf(vaultItem(plannedPrice = 5.0, quantity = 2.0, actualPrice = 9.0))
        // Actual price is ignored because the row isn't fulfilled AND wasEdited is false.
        assertEquals(5.0 * 2.0, service.computeTotalSpent(CartStatus.SHOPPING, items), 1e-9)
    }

    @Test
    fun `completed total uses actual fields when available`() {
        val items = listOf(
            vaultItem(
                plannedPrice = 5.0,
                quantity = 1.0,
                actualPrice = 6.25,
                actualQuantity = 2.0,
                isFulfilled = true,
            ),
            vaultItem(
                uid = "row-skip",
                itemId = "item-2",
                plannedPrice = 10.0,
                quantity = 3.0,
                isSkipped = true,
            ),
        )
        assertEquals(6.25 * 2.0, service.computeTotalSpent(CartStatus.COMPLETED, items), 1e-9)
    }

    @Test
    fun `completed total falls back to planned when actual missing`() {
        val items = listOf(
            vaultItem(plannedPrice = 3.0, quantity = 4.0, isFulfilled = true),
        )
        assertEquals(3.0 * 4.0, service.computeTotalSpent(CartStatus.COMPLETED, items), 1e-9)
    }
}
