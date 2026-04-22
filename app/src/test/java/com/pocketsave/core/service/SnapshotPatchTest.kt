package com.pocketsave.core.service

import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.PriceOptionEntity
import com.pocketsave.data.local.entity.PricePerUnit
import com.pocketsave.domain.model.CartStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Regression coverage for the pure snapshot-patcher helpers introduced in the
 * targeted-refresh refactor. These are the "blast-radius" boundary between the
 * database and the exposed [VaultService.state]: if they diverge from what
 * [VaultService.buildSnapshot] would produce, the whole UI sees stale data for
 * the first mutation after the drift.
 *
 * The patchers are file-internal pure functions, tested via the in-package
 * `internal` visibility accessible from the test source set.
 */
class SnapshotPatchTest {

    // ---------- Fixtures ----------

    private fun cart(
        id: String,
        createdAt: Long,
        deletedAt: Long? = null,
        status: CartStatus = CartStatus.PLANNING,
    ): CartEntity = CartEntity(
        id = id,
        vaultUid = "vault",
        name = "Cart $id",
        budget = 0.0,
        createdAt = Date(createdAt),
        updatedAt = Date(createdAt),
        status = status.raw,
        isDeleted = deletedAt != null,
        deletedAt = deletedAt?.let(::Date),
    )

    private fun cartItem(uid: String, cartId: String): CartItemEntity = CartItemEntity(
        uid = uid,
        cartId = cartId,
        itemId = "item-$uid",
        addedAt = Date(),
        quantity = 1.0,
        plannedStore = "Store",
    )

    private fun item(id: String, deleted: Boolean = false): ItemEntity = ItemEntity(
        id = id,
        vaultUid = "vault",
        categoryUid = "cat",
        name = "Item $id",
        createdAt = Date(),
        isDeleted = deleted,
        deletedAt = if (deleted) Date() else null,
    )

    private fun priceOption(itemId: String, store: String): PriceOptionEntity = PriceOptionEntity(
        uid = "po-$itemId-$store",
        itemId = itemId,
        store = store,
        pricePerUnit = PricePerUnit(priceValue = 1.0, unit = "each"),
    )

    // ---------- Cart patcher ----------

    @Test
    fun `patchCart inserts new cart at the correct createdAt-DESC position`() {
        val older = cart("a", createdAt = 1_000)
        val newer = cart("b", createdAt = 3_000)
        val base = VaultService.Snapshot(carts = listOf(newer, older))

        val middle = cart("c", createdAt = 2_000)
        val patched = VaultService.patchCart(base, "c", middle, items = emptyList())

        assertEquals(listOf("b", "c", "a"), patched.carts.map { it.id })
    }

    @Test
    fun `patchCart updates existing cart in place and preserves order`() {
        val a = cart("a", createdAt = 1_000)
        val b = cart("b", createdAt = 2_000)
        val base = VaultService.Snapshot(carts = listOf(b, a))

        val updated = b.copy(name = "Renamed")
        val patched = VaultService.patchCart(base, "b", updated, items = emptyList())

        assertEquals(listOf("b", "a"), patched.carts.map { it.id })
        assertEquals("Renamed", patched.carts.first().name)
    }

    @Test
    fun `patchCart soft-delete moves the cart from carts to deletedCarts`() {
        val alive = cart("a", createdAt = 1_000, status = CartStatus.COMPLETED)
        val base = VaultService.Snapshot(
            carts = listOf(alive),
            cartItemsByCart = mapOf("a" to listOf(cartItem("row", "a"))),
        )

        val softDeleted = alive.copy(isDeleted = true, deletedAt = Date(5_000))
        val patched = VaultService.patchCart(base, "a", softDeleted, items = listOf(cartItem("row", "a")))

        assertTrue("active carts should drop soft-deleted entry", patched.carts.none { it.id == "a" })
        assertEquals(listOf("a"), patched.deletedCarts.map { it.id })
        // Items are retained: deletedCarts in the trash view still reference their rows.
        assertEquals(1, patched.cartItemsByCart["a"]?.size)
    }

    @Test
    fun `patchCart hard-delete drops cart from both lists and evicts items map`() {
        val alive = cart("a", createdAt = 1_000)
        val base = VaultService.Snapshot(
            carts = listOf(alive),
            cartItemsByCart = mapOf("a" to listOf(cartItem("row", "a"))),
        )

        val patched = VaultService.patchCart(base, "a", cart = null, items = emptyList())

        assertTrue(patched.carts.isEmpty())
        assertTrue(patched.deletedCarts.isEmpty())
        assertFalse("cart_items map must evict the dead key", patched.cartItemsByCart.containsKey("a"))
    }

    @Test
    fun `patchCart restore moves cart from deletedCarts back to carts`() {
        val trashed = cart("a", createdAt = 1_000, deletedAt = 5_000, status = CartStatus.COMPLETED)
        val base = VaultService.Snapshot(deletedCarts = listOf(trashed))

        val restored = trashed.copy(isDeleted = false, deletedAt = null)
        val patched = VaultService.patchCart(base, "a", restored, items = emptyList())

        assertEquals(listOf("a"), patched.carts.map { it.id })
        assertTrue(patched.deletedCarts.isEmpty())
    }

    @Test
    fun `patchCart deletedCarts ordering mirrors DAO deletedAt DESC`() {
        val older = cart("older", createdAt = 1_000, deletedAt = 10_000, status = CartStatus.COMPLETED)
        val newer = cart("newer", createdAt = 2_000, deletedAt = 20_000, status = CartStatus.COMPLETED)
        val base = VaultService.Snapshot(deletedCarts = listOf(older, newer))

        val third = cart("mid", createdAt = 1_500, deletedAt = 15_000, status = CartStatus.COMPLETED)
        val patched = VaultService.patchCart(base, "mid", third, items = emptyList())

        assertEquals(listOf("newer", "mid", "older"), patched.deletedCarts.map { it.id })
    }

    @Test
    fun `patchCart leaves every unrelated field reference-equal (structural sharing)`() {
        val a = cart("a", createdAt = 1_000)
        val unrelatedItem = item("other")
        val base = VaultService.Snapshot(
            carts = listOf(a),
            items = listOf(unrelatedItem),
            itemsById = mapOf("other" to unrelatedItem),
            priceOptionsByItem = mapOf("other" to listOf(priceOption("other", "S"))),
            categories = emptyList(),
        )

        val renamed = a.copy(name = "Zzz")
        val patched = VaultService.patchCart(base, "a", renamed, items = emptyList())

        // The vault-side collections are untouched — same list/map references —
        // so downstream consumers that read only these fields receive a
        // structurally-equal derived state and can skip recomposition.
        assertSame(base.items, patched.items)
        assertSame(base.itemsById, patched.itemsById)
        assertSame(base.priceOptionsByItem, patched.priceOptionsByItem)
        assertSame(base.categories, patched.categories)
    }

    // ---------- Item patcher ----------

    @Test
    fun `patchItem inserts new item and its price options`() {
        val base = VaultService.Snapshot()
        val fresh = item("x")
        val price = priceOption("x", "Store A")

        val patched = VaultService.patchItem(base, "x", fresh, listOf(price))

        assertEquals(listOf("x"), patched.items.map { it.id })
        assertEquals(fresh, patched.itemsById["x"])
        assertEquals(listOf(price), patched.priceOptionsByItem["x"])
    }

    @Test
    fun `patchItem soft-delete moves row from items to deletedItems and clears itemsById`() {
        val alive = item("x")
        val price = priceOption("x", "Store A")
        val base = VaultService.Snapshot(
            items = listOf(alive),
            itemsById = mapOf("x" to alive),
            priceOptionsByItem = mapOf("x" to listOf(price)),
        )

        val softDeleted = alive.copy(isDeleted = true, deletedAt = Date())
        val patched = VaultService.patchItem(base, "x", softDeleted, listOf(price))

        assertTrue(patched.items.isEmpty())
        assertEquals(listOf("x"), patched.deletedItems.map { it.id })
        assertNull("itemsById should not retain deleted rows", patched.itemsById["x"])
        // Price options survive the soft-delete (FK cascade only fires on hard delete).
        assertEquals(listOf(price), patched.priceOptionsByItem["x"])
    }

    @Test
    fun `patchItem hard-delete drops row from both lists and evicts price options`() {
        val alive = item("x")
        val base = VaultService.Snapshot(
            items = listOf(alive),
            itemsById = mapOf("x" to alive),
            priceOptionsByItem = mapOf("x" to listOf(priceOption("x", "S"))),
        )

        val patched = VaultService.patchItem(base, "x", item = null, priceOptions = emptyList())

        assertTrue(patched.items.isEmpty())
        assertTrue(patched.deletedItems.isEmpty())
        assertNull(patched.itemsById["x"])
        assertFalse(
            "price-option map must evict the dead key (FK cascade already removed rows)",
            patched.priceOptionsByItem.containsKey("x"),
        )
    }

    @Test
    fun `patchItem restore moves row from deletedItems back to items and repopulates itemsById`() {
        val trashed = item("x", deleted = true)
        val base = VaultService.Snapshot(
            deletedItems = listOf(trashed),
            priceOptionsByItem = emptyMap(),
        )

        val restored = trashed.copy(isDeleted = false, deletedAt = null, categoryUid = "cat")
        val patched = VaultService.patchItem(base, "x", restored, listOf(priceOption("x", "Store B")))

        assertEquals(listOf("x"), patched.items.map { it.id })
        assertTrue(patched.deletedItems.isEmpty())
        assertEquals(restored, patched.itemsById["x"])
        assertEquals(1, patched.priceOptionsByItem["x"]?.size)
    }

    @Test
    fun `patchItem leaves cart-side fields reference-equal`() {
        val alive = item("x")
        val cartRow = cartItem("row", "cart-1")
        val base = VaultService.Snapshot(
            items = listOf(alive),
            itemsById = mapOf("x" to alive),
            carts = listOf(cart("cart-1", 1_000)),
            cartItemsByCart = mapOf("cart-1" to listOf(cartRow)),
        )

        val renamed = alive.copy(name = "Renamed Item")
        val patched = VaultService.patchItem(base, "x", renamed, emptyList())

        assertSame(base.carts, patched.carts)
        assertSame(base.cartItemsByCart, patched.cartItemsByCart)
        assertSame(base.deletedCarts, patched.deletedCarts)
    }
}
