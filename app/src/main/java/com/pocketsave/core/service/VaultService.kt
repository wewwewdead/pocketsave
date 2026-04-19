package com.pocketsave.core.service

import androidx.room.withTransaction
import com.pocketsave.common.util.ActiveItemSelectionKey
import com.pocketsave.data.local.db.PocketSaveDatabase
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.data.local.entity.CategoryEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.PriceOptionEntity
import com.pocketsave.data.local.entity.PricePerUnit
import com.pocketsave.data.local.entity.StoreEntity
import com.pocketsave.data.local.entity.UserEntity
import com.pocketsave.data.local.entity.VaultEntity
import com.pocketsave.domain.model.CartStatus
import com.pocketsave.domain.model.GroceryCategory
import com.pocketsave.domain.semantics.PackagingScanMetadata
import com.pocketsave.domain.semantics.PackagingSemantics
import com.pocketsave.domain.semantics.UnitSemantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.UUID

/**
 * Android port of iOS `PocketSave/Service/VaultService.swift`.
 *
 * Role (as in iOS): the app's "data brain". Loads (or creates) the single user +
 * vault on launch, pre-populates default categories, and exposes the current
 * vault snapshot to the rest of the app. Category / item / store mutations are
 * ported from `VaultService+Domain.swift` and `VaultService+Validation.swift`;
 * cart and shopping helpers land in later phases.
 */
class VaultService(
    private val db: PocketSaveDatabase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    data class Snapshot(
        val user: UserEntity? = null,
        val vault: VaultEntity? = null,
        val categories: List<CategoryEntity> = emptyList(),
        val stores: List<StoreEntity> = emptyList(),
        /** Active (non-deleted) vault items across every category. */
        val items: List<ItemEntity> = emptyList(),
        /**
         * Items keyed by `id` for O(1) lookup. Pre-computed here (once per
         * snapshot) so that per-frame consumers (cart detail, trip share,
         * widget) don't each rebuild their own `items.associateBy { it.id }`.
         */
        val itemsById: Map<String, ItemEntity> = emptyMap(),
        /** Map from itemId → price options for fast per-row lookup. */
        val priceOptionsByItem: Map<String, List<PriceOptionEntity>> = emptyMap(),
        /** Non-deleted carts (planning, shopping, completed), newest first. */
        val carts: List<CartEntity> = emptyList(),
        /** cartId → ordered cart items (`addedAt` ascending, iOS ordering). */
        val cartItemsByCart: Map<String, List<CartItemEntity>> = emptyMap(),
        /** Soft-deleted vault items (iOS `vault.deletedItems`). */
        val deletedItems: List<ItemEntity> = emptyList(),
        /** Soft-deleted carts (iOS `vault.deletedCarts` — completed trash only). */
        val deletedCarts: List<CartEntity> = emptyList(),
        val isLoading: Boolean = false,
        val error: Throwable? = null,
    )

    /**
     * Port of `VaultService+Validation.swift`'s `(isValid, errorMessage)` tuple.
     * Kept as a dedicated type so the UI can pattern-match without caring about
     * tuple ordering across ports.
     */
    data class ValidationResult(val isValid: Boolean, val errorMessage: String?) {
        companion object {
            val VALID = ValidationResult(true, null)
            fun invalid(message: String) = ValidationResult(false, message)
        }
    }

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private val loadLock = Mutex()
    private val writeLock = Mutex()

    /**
     * Hard-resets the vault back to first-run state: wipes every Room table and
     * publishes an empty snapshot. Callers are expected to wipe preferences and
     * image storage on their own and then decide where to navigate the user
     * next (usually back through onboarding).
     *
     * Runs under both locks so it can't interleave with a load or a write.
     */
    suspend fun resetAllData() {
        loadLock.withLock {
            writeLock.withLock {
                // Room's generated `clearAllTables` respects FK constraints
                // and is the canonical way to hard-wipe the database without
                // dropping the file itself.
                db.clearAllTables()
                _state.value = Snapshot()
            }
        }
    }

    /**
     * Entry-point mirroring `VaultService.loadUserAndVault()` in iOS. Safe to call
     * multiple times; a mutex guarantees that parallel callers don't race to create
     * duplicate users.
     */
    fun loadUserAndVault() {
        scope.launch { loadUserAndVaultBlocking() }
    }

    suspend fun loadUserAndVaultBlocking() {
        loadLock.withLock {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val user = db.userDao().firstUser()
                if (user != null) {
                    val vault = db.vaultDao().findByUid(user.vaultUid)
                        ?: VaultEntity(user.vaultUid).also { db.vaultDao().insert(it) }
                    ensureAllCategoriesExist(vault.uid)
                    publishSnapshot(user, vault)
                } else {
                    val newVault = VaultEntity(UUID.randomUUID().toString())
                    db.vaultDao().insert(newVault)
                    val newUser = UserEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Default User",
                        vaultUid = newVault.uid,
                    )
                    db.userDao().insert(newUser)
                    prePopulateCategories(newVault.uid)
                    publishSnapshot(newUser, newVault)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isLoading = false, error = t)
            }
        }
    }

    /**
     * Mirrors `VaultService.updateUserName`. Trims the input and no-ops on empty
     * strings so the rest of the app can rely on `user.name` being non-blank.
     */
    suspend fun updateUserName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val user = _state.value.user ?: return
        val updated = user.copy(name = trimmed)
        db.userDao().update(updated)
        _state.value = _state.value.copy(user = updated)
    }

    // MARK: - Category lookups (VaultService+Domain.swift `getCategory` variants)

    /** Port of `getCategory(_ groceryCategory:)` — exact title match. */
    suspend fun getCategory(groceryCategory: GroceryCategory): CategoryEntity? {
        val vault = _state.value.vault ?: return null
        return db.categoryDao().listByVault(vault.uid)
            .firstOrNull { it.name == groceryCategory.title }
    }

    /** Port of `getCategory(named name:)` — case-insensitive, trimmed. */
    suspend fun getCategoryByName(name: String): CategoryEntity? {
        val vault = _state.value.vault ?: return null
        return db.categoryDao().findByName(vault.uid, name)
    }

    // MARK: - Store writes (VaultService+Domain.swift store section)

    /**
     * Port of `addStore(_:)`. Paywall gating (`canUseStoreName`, `isPro`) is
     * intentionally omitted — Phase 1/2 runs without monetization.
     */
    suspend fun addStore(storeName: String) {
        val vault = _state.value.vault ?: return
        val trimmed = storeName.trim()
        if (trimmed.isEmpty()) return

        val inserted = writeLock.withLock {
            insertStoreIfMissing(vault.uid, trimmed)
        }
        if (inserted) refreshStoresSnapshot()
    }

    /** Port of `ensureStoreExists(_:)` — convenience wrapper over [addStore]. */
    suspend fun ensureStoreExists(storeName: String) = addStore(storeName)

    /**
     * Port of `getAllStores()`, minus the paywall-specific ordering tweaks.
     *
     * Replaces the legacy `listActive` + per-item `listForItem` N+1 loop with a
     * single indexed join (`listActiveByVault`) that already returns all active
     * vault item price options.
     */
    suspend fun getAllStores(): List<String> {
        val vault = _state.value.vault ?: return emptyList()
        val rows = db.storeDao().listByVault(vault.uid).sortedByDescending { it.createdAt }
        val vaultNames = rows.map { it.name }

        val itemStores = db.priceOptionDao().listActiveByVault(vault.uid).map { it.store }
        val seen = vaultNames.map { it.lowercase() }.toMutableSet()
        val extras = itemStores
            .asSequence()
            .filter { it.lowercase() !in seen }
            .onEach { seen += it.lowercase() }
            .distinctBy { it.lowercase() }
            .sorted()
            .toList()
        return vaultNames + extras
    }

    /** Port of `getMostRecentStore()`. */
    suspend fun getMostRecentStore(): String? {
        val vault = _state.value.vault ?: return null
        return db.storeDao().mostRecent(vault.uid)?.name
    }

    // MARK: - Validation (VaultService+Validation.swift)

    /**
     * Port of `isItemNameDuplicate(_:store:excluding:)`.
     *
     * Replaces the legacy `listActive` + per-item `listForItem` N+1 loop with a
     * single indexed join query that returns a count. The join uses the
     * `(vaultUid, isDeleted)` composite on items and the unique `(itemId, store)`
     * index on price options, so the whole check is one seek per index.
     */
    suspend fun isItemNameDuplicate(
        name: String,
        store: String,
        excludingItemId: String? = null,
    ): Boolean {
        val vault = _state.value.vault ?: return false
        val trimmedName = name.trim()
        val trimmedStore = store.trim()
        if (trimmedName.isEmpty() || trimmedStore.isEmpty()) return false
        return db.priceOptionDao()
            .countDuplicateByNameAndStore(vault.uid, trimmedName, trimmedStore, excludingItemId) > 0
    }

    /**
     * Port of the private `categoryNameForExistingItemName` helper used by the
     * validation pipeline. Returns the category an item with this name currently
     * lives in, or null when the name is free.
     */
    private suspend fun categoryNameForExistingItemName(
        itemName: String,
        excludingItemId: String? = null,
    ): String? {
        val vault = _state.value.vault ?: return null
        val target = itemName.trim().lowercase()
        if (target.isEmpty()) return null

        val categories = db.categoryDao().listByVault(vault.uid)
        val categoryByUid = categories.associateBy { it.uid }
        val activeItems = db.itemDao().listActive(vault.uid)
        for (item in activeItems) {
            if (excludingItemId != null && item.id == excludingItemId) continue
            if (item.name.trim().lowercase() != target) continue
            val categoryUid = item.categoryUid ?: continue
            val category = categoryByUid[categoryUid] ?: continue
            val trimmedName = category.name.trim()
            return if (trimmedName.isEmpty()) category.name else trimmedName
        }
        return null
    }

    /**
     * Port of `validateItemName(_:store:categoryName:excluding:allowedLockedStoreNames:)`.
     * Paywall-specific store-limit branch is dropped; Phase 2 always treats the
     * store check as passing.
     */
    suspend fun validateItemName(
        name: String,
        store: String,
        categoryName: String? = null,
        excludingItemId: String? = null,
    ): ValidationResult {
        val trimmedName = name.trim()
        val trimmedStore = store.trim()
        val trimmedCategory = categoryName?.trim().orEmpty()

        if (trimmedName.isEmpty()) return ValidationResult.invalid("Item name is required")

        if (trimmedCategory.isNotEmpty()) {
            val existingCategory = categoryNameForExistingItemName(trimmedName, excludingItemId)
            if (existingCategory != null) {
                val existingKey = existingCategory.trim().lowercase()
                val targetKey = trimmedCategory.lowercase()
                if (existingKey != targetKey) {
                    return ValidationResult.invalid("$trimmedName already exists in $existingCategory")
                }
            }
        }

        if (trimmedStore.isEmpty()) return ValidationResult.invalid("Store name is required")

        if (isItemNameDuplicate(trimmedName, trimmedStore, excludingItemId)) {
            return ValidationResult.invalid("$trimmedName already exists at $trimmedStore")
        }

        return ValidationResult.VALID
    }

    // MARK: - Item writes (VaultService+Domain.swift addItem variants)

    /**
     * Port of `addItem(name:toCategoryName:store:price:unit:packageSizeValue:packageSizeUnit:packagingMetadata:)`
     * from `VaultService+Domain.swift`. Creates a category by name when one
     * doesn't exist, normalises units, persists a single price option, and also
     * ensures the store is present in the stores table (so subsequent flows that
     * read `vault.stores` see it — iOS reaches the same state via
     * `ensureStoreExists` in other call sites).
     *
     * Plan-suppression checks are omitted per the Phase 1/2 no-paywall rule.
     */
    suspend fun addItem(
        name: String,
        toCategoryName: String,
        store: String,
        price: Double,
        unit: String,
        packageSizeValue: Double? = null,
        packageSizeUnit: String? = null,
        packagingMetadata: PackagingScanMetadata? = null,
        imageUri: String? = null,
    ): ItemEntity? {
        val vault = _state.value.vault ?: return null
        val trimmedCategoryName = toCategoryName.trim()
        val trimmedStore = store.trim()
        val trimmedName = name.trim()

        val validation = validateItemName(
            name = trimmedName,
            store = trimmedStore,
            categoryName = trimmedCategoryName.ifEmpty { null },
        )
        if (!validation.isValid) return null

        val inserted = writeLock.withLock {
            db.withTransaction {
                val category = resolveOrCreateCategory(vault.uid, trimmedCategoryName)
                    ?: return@withTransaction null

                val normalizedUnit = UnitSemantics.canonicalUnit(unit)
                val normalizedPackageUnit = packageSizeUnit?.let(UnitSemantics::canonicalUnit)
                val shouldStorePackageSize = !UnitSemantics.isContinuous(normalizedUnit)
                val finalPackageSizeValue = if (
                    shouldStorePackageSize &&
                    !normalizedPackageUnit.isNullOrBlank() &&
                    packageSizeValue != null &&
                    packageSizeValue > 0.0
                ) packageSizeValue else null
                val finalPackageSizeUnit = if (finalPackageSizeValue != null) normalizedPackageUnit else null
                val finalOuterPackagingUnit =
                    PackagingSemantics.canonicalPackagingUnit(packagingMetadata?.outerPackagingUnit)
                val finalOuterPackagingConfidence = if (finalOuterPackagingUnit != null) {
                    PackagingSemantics.clampedConfidence(packagingMetadata?.outerPackagingConfidence)
                } else null
                val finalOuterPackagingSource = if (finalOuterPackagingUnit != null) {
                    packagingMetadata?.source?.raw
                } else null

                val newItem = ItemEntity(
                    id = UUID.randomUUID().toString(),
                    vaultUid = vault.uid,
                    categoryUid = category.uid,
                    name = trimmedName,
                    createdAt = Date(),
                    imageUri = imageUri,
                )
                db.itemDao().insert(newItem)

                val priceOption = PriceOptionEntity(
                    uid = UUID.randomUUID().toString(),
                    itemId = newItem.id,
                    store = trimmedStore,
                    pricePerUnit = PricePerUnit(
                        priceValue = price,
                        unit = normalizedUnit,
                        packageSizeValue = finalPackageSizeValue,
                        packageSizeUnit = finalPackageSizeUnit,
                        outerPackagingUnit = finalOuterPackagingUnit,
                        outerPackagingConfidence = finalOuterPackagingConfidence,
                        outerPackagingSource = finalOuterPackagingSource,
                    ),
                )
                db.priceOptionDao().insert(priceOption)

                newItem
            }
        }

        if (inserted != null) {
            // Mirror the iOS behaviour where price-option stores eventually end up in
            // `vault.stores` — do it here explicitly so the stores table is always a
            // superset of the price-option stores.
            ensureStoreExists(trimmedStore)
            publishSnapshotRefresh()
        }

        return inserted
    }

    // MARK: - Item updates (VaultService+Domain.swift updateItem variants)

    /**
     * Port of `updateItem(item:newName:newCategoryName:newStore:newPrice:newUnit:packageSize...)` in
     * `VaultService+Domain.swift`. Renames the item, optionally moves it between
     * categories, and replaces the first price option with the new
     * store/price/unit. Paywall checks are omitted — Phase 4 has no plan gating.
     */
    suspend fun updateItem(
        itemId: String,
        newName: String,
        newCategoryName: String,
        newStore: String,
        newPrice: Double,
        newUnit: String,
        packageSizeValue: Double? = null,
        packageSizeUnit: String? = null,
        updatePackageSize: Boolean = false,
        packagingMetadata: PackagingScanMetadata? = null,
        updatePackagingMetadata: Boolean = false,
        imageUri: String? = null,
        updateImage: Boolean = false,
    ): Boolean {
        val vault = _state.value.vault ?: return false
        val trimmedName = newName.trim()
        val trimmedStore = newStore.trim()
        val trimmedCategory = newCategoryName.trim()

        val validation = validateItemName(
            name = trimmedName,
            store = trimmedStore,
            categoryName = trimmedCategory.ifEmpty { null },
            excludingItemId = itemId,
        )
        if (!validation.isValid) return false

        val result = writeLock.withLock {
            db.withTransaction {
                val existing = db.itemDao().findById(itemId) ?: return@withTransaction false
                if (existing.isDeleted) return@withTransaction false

                val targetCategory = resolveOrCreateCategory(vault.uid, trimmedCategory)
                    ?: return@withTransaction false

                val normalizedUnit = UnitSemantics.canonicalUnit(newUnit)
                val normalizedPackageUnit = packageSizeUnit?.let(UnitSemantics::canonicalUnit)
                val isContinuousUnit = UnitSemantics.isContinuous(normalizedUnit)
                val normalizedOuterPackagingUnit =
                    PackagingSemantics.canonicalPackagingUnit(packagingMetadata?.outerPackagingUnit)
                val normalizedOuterPackagingConfidence = if (normalizedOuterPackagingUnit != null) {
                    PackagingSemantics.clampedConfidence(packagingMetadata?.outerPackagingConfidence)
                } else null
                val normalizedOuterPackagingSource = if (normalizedOuterPackagingUnit != null) {
                    packagingMetadata?.source?.raw
                } else null

                val updatedItem = existing.copy(
                    name = trimmedName,
                    categoryUid = targetCategory.uid,
                    imageUri = if (updateImage) imageUri else existing.imageUri,
                )
                db.itemDao().update(updatedItem)

                val priceOptions = db.priceOptionDao().listForItem(itemId)
                if (priceOptions.isNotEmpty()) {
                // Mirror iOS: mutate the first price option in-place.
                val primary = priceOptions.first()
                val existingPpu = primary.pricePerUnit
                val updatedPpu = existingPpu.copy(
                    priceValue = newPrice,
                    unit = normalizedUnit,
                    packageSizeValue = when {
                        isContinuousUnit -> null
                        updatePackageSize -> if (
                            packageSizeValue != null && packageSizeValue > 0.0 &&
                            !normalizedPackageUnit.isNullOrBlank()
                        ) packageSizeValue else null
                        else -> existingPpu.packageSizeValue
                    },
                    packageSizeUnit = when {
                        isContinuousUnit -> null
                        updatePackageSize -> if (
                            packageSizeValue != null && packageSizeValue > 0.0 &&
                            !normalizedPackageUnit.isNullOrBlank()
                        ) normalizedPackageUnit else null
                        else -> existingPpu.packageSizeUnit
                    },
                    outerPackagingUnit = if (updatePackagingMetadata) normalizedOuterPackagingUnit else existingPpu.outerPackagingUnit,
                    outerPackagingConfidence = if (updatePackagingMetadata) normalizedOuterPackagingConfidence else existingPpu.outerPackagingConfidence,
                    outerPackagingSource = if (updatePackagingMetadata) normalizedOuterPackagingSource else existingPpu.outerPackagingSource,
                )
                db.priceOptionDao().update(
                    primary.copy(store = trimmedStore, pricePerUnit = updatedPpu),
                )
            } else {
                val sanitizedPackageSizeValue = if (
                    !isContinuousUnit && packageSizeValue != null && packageSizeValue > 0.0
                ) packageSizeValue else null
                val sanitizedPackageSizeUnit =
                    if (sanitizedPackageSizeValue != null) normalizedPackageUnit else null
                db.priceOptionDao().insert(
                    PriceOptionEntity(
                        uid = UUID.randomUUID().toString(),
                        itemId = itemId,
                        store = trimmedStore,
                        pricePerUnit = PricePerUnit(
                            priceValue = newPrice,
                            unit = normalizedUnit,
                            packageSizeValue = sanitizedPackageSizeValue,
                            packageSizeUnit = sanitizedPackageSizeUnit,
                            outerPackagingUnit = if (updatePackagingMetadata) normalizedOuterPackagingUnit else null,
                            outerPackagingConfidence = if (updatePackagingMetadata) normalizedOuterPackagingConfidence else null,
                            outerPackagingSource = if (updatePackagingMetadata) normalizedOuterPackagingSource else null,
                        ),
                    ),
                )
            }
                true
            }
        }

        if (result) {
            ensureStoreExists(trimmedStore)
            publishSnapshotRefresh()
        }
        return result
    }

    // MARK: - Item soft-delete (VaultService+Domain.swift deleteItem variants)

    /**
     * Port of `deleteItem(itemId:)` / `deleteItem(_ item:)`. Soft-deletes the
     * item (iOS moves it to `vault.deletedItems`). The cart cleanup
     * (`DeletedCartItemSnapshot`) lands with the cart phase — for now the item
     * is simply flagged deleted and orphaned from its category.
     */
    suspend fun deleteItem(itemId: String): Boolean {
        val result = writeLock.withLock {
            db.withTransaction {
                val existing = db.itemDao().findById(itemId) ?: return@withTransaction false
                if (existing.isDeleted) return@withTransaction false
                val fromCategoryName = existing.categoryUid?.let { db.categoryDao().findByUid(it)?.name }
                db.itemDao().update(
                    existing.copy(
                        isDeleted = true,
                        deletedAt = Date(),
                        deletedFromCategoryName = fromCategoryName,
                        categoryUid = null,
                    ),
                )
                true
            }
        }
        // Targeted: only the one item row moves from `items` to `deletedItems`;
        // categories / stores / carts are untouched.
        if (result) refreshItem(itemId)
        return result
    }

    suspend fun deleteItem(item: ItemEntity): Boolean = deleteItem(item.id)

    /**
     * Port of `permanentlyDeleteItemFromTrash(itemId:)`. Issues a hard delete so
     * price options cascade away. Safe to call on both deleted and active items.
     */
    suspend fun permanentlyDeleteItem(itemId: String): Boolean {
        val result = writeLock.withLock {
            val existing = db.itemDao().findById(itemId) ?: return@withLock false
            db.itemDao().delete(existing)
            true
        }
        // Targeted: refreshItem's null-path drops the row from both lists and
        // evicts the price-option map entry (FK cascade already removed rows).
        if (result) refreshItem(itemId)
        return result
    }

    // MARK: - Item lookups (VaultService+Domain.swift get/find helpers)

    /** Port of `getAllItems()` — every active vault item, across categories. */
    suspend fun getAllItems(): List<ItemEntity> {
        val vault = _state.value.vault ?: return emptyList()
        return db.itemDao().listActive(vault.uid)
    }

    /**
     * Port of `findItemById(_:)`. Looks in active items first, falls back to the
     * trash. Shopping-only lookups live with the cart phase.
     */
    suspend fun findItemById(itemId: String): ItemEntity? = db.itemDao().findById(itemId)

    /** Port of `findItemsByName(_:)` — case-insensitive substring match. */
    suspend fun findItemsByName(name: String): List<ItemEntity> {
        val vault = _state.value.vault ?: return emptyList()
        val term = name.trim().lowercase()
        if (term.isEmpty()) return emptyList()
        return db.itemDao().listActive(vault.uid)
            .filter { it.name.lowercase().contains(term) }
    }

    /** Port of `getCategoryName(for itemId:)`. */
    suspend fun getCategoryNameForItem(itemId: String): String? {
        val item = db.itemDao().findById(itemId) ?: return null
        val categoryUid = item.categoryUid ?: return null
        return db.categoryDao().findByUid(categoryUid)?.name
    }

    /** Port of `getCategory(for itemId:)`. */
    suspend fun getCategoryForItem(itemId: String): CategoryEntity? {
        val item = db.itemDao().findById(itemId) ?: return null
        val categoryUid = item.categoryUid ?: return null
        return db.categoryDao().findByUid(categoryUid)
    }

    /** Returns the price options for a single item (empty when none exist). */
    suspend fun getPriceOptionsForItem(itemId: String): List<PriceOptionEntity> =
        db.priceOptionDao().listForItem(itemId)

    // MARK: - Category writes (VaultService+Domain.swift category helpers)

    /**
     * Port of `addCategory(_:)`. Adds a default grocery category by enum; no-op
     * when the category is already present.
     */
    suspend fun addCategory(groceryCategory: GroceryCategory): CategoryEntity? {
        val vault = _state.value.vault ?: return null
        return writeLock.withLock {
            val existing = db.categoryDao().findByName(vault.uid, groceryCategory.title)
            if (existing != null) return@withLock existing
            val nextSortOrder = db.categoryDao().listByVault(vault.uid).size
            val created = CategoryEntity(
                uid = UUID.randomUUID().toString(),
                vaultUid = vault.uid,
                name = groceryCategory.title,
                iconKey = groceryCategory.defaultIconKey,
                sortOrder = nextSortOrder,
            )
            db.categoryDao().insert(created)
            refreshCategoriesSnapshot()
            created
        }
    }

    /**
     * Port of iOS `createCustomCategory(named:iconKey:colorHex:)`. Trims inputs
     * and returns the existing row when the name already matches a category
     * (system or custom). `iconKey` values are looked up at render time via
     * `AppIcon.resolveIcon` — unknown keys fall back to a generic label icon.
     */
    suspend fun createCustomCategory(
        name: String,
        iconKey: String? = null,
        colorHex: String? = null,
    ): CategoryEntity? {
        val vault = _state.value.vault ?: return null
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null

        return writeLock.withLock {
            val existing = db.categoryDao().findByName(vault.uid, trimmed)
            if (existing != null) return@withLock existing

            val normalizedKey = iconKey?.trim()?.takeIf { it.isNotEmpty() }
            val nextSortOrder = db.categoryDao().listByVault(vault.uid).size
            val created = CategoryEntity(
                uid = UUID.randomUUID().toString(),
                vaultUid = vault.uid,
                name = trimmed,
                iconKey = normalizedKey,
                sortOrder = nextSortOrder,
                colorHex = colorHex?.trim()?.takeIf { it.isNotEmpty() },
            )
            db.categoryDao().insert(created)
            refreshCategoriesSnapshot()
            created
        }
    }

    /**
     * Port of iOS `updateCustomCategory(originalName:newName:iconKey:colorHex:)`.
     * Rejects edits on system categories and on names that would collide with
     * an existing different category.
     */
    suspend fun updateCustomCategory(
        originalName: String,
        newName: String,
        iconKey: String? = null,
        colorHex: String? = null,
    ): CategoryEntity? {
        val vault = _state.value.vault ?: return null
        val normalizedOriginal = normalizedCategoryName(originalName)
        val trimmedNew = newName.trim()
        if (trimmedNew.isEmpty()) return null

        return writeLock.withLock {
            val category = db.categoryDao().listByVault(vault.uid)
                .firstOrNull { normalizedCategoryName(it.name) == normalizedOriginal }
                ?: return@withLock null

            if (isSystemCategory(category.name)) return@withLock null

            val normalizedNew = normalizedCategoryName(trimmedNew)
            if (normalizedNew != normalizedOriginal) {
                val collision = db.categoryDao().findByName(vault.uid, trimmedNew)
                if (collision != null && collision.uid != category.uid) return@withLock null
            }

            val normalizedKey = iconKey?.trim()?.takeIf { it.isNotEmpty() }
            val normalizedColor = colorHex?.trim()?.takeIf { it.isNotEmpty() }
            val updated = category.copy(
                name = trimmedNew,
                iconKey = normalizedKey,
                colorHex = normalizedColor,
            )
            db.categoryDao().update(updated)
            refreshCategoriesSnapshot()
            updated
        }
    }

    /**
     * Port of `deleteCustomCategory(named:)`. Soft-deletes every item in the
     * category first (matching the iOS behaviour), then removes the category
     * row. Rejects system category deletes.
     */
    suspend fun deleteCustomCategory(name: String): Boolean {
        val vault = _state.value.vault ?: return false
        val normalized = normalizedCategoryName(name)

        val success = writeLock.withLock {
            db.withTransaction {
                val category = db.categoryDao().listByVault(vault.uid)
                    .firstOrNull { normalizedCategoryName(it.name) == normalized }
                    ?: return@withTransaction false
                if (isSystemCategory(category.name)) return@withTransaction false

                val items = db.itemDao().listByCategory(category.uid)
                val now = Date()
                for (item in items) {
                    if (item.isDeleted) continue
                    db.itemDao().update(
                        item.copy(
                            isDeleted = true,
                            deletedAt = now,
                            deletedFromCategoryName = category.name,
                            categoryUid = null,
                        ),
                    )
                }
                db.categoryDao().delete(category)
                true
            }
        }
        if (success) publishSnapshotRefresh()
        return success
    }

    private fun isSystemCategory(name: String): Boolean {
        val normalized = normalizedCategoryName(name)
        return GroceryCategory.entries.any { normalizedCategoryName(it.title) == normalized }
    }

    /**
     * Port of iOS `displayEmoji(forCategoryName:)` adapted to Material Icons.
     * Resolution order:
     *   1. the stored `iconKey` on the category row
     *   2. the default key for a matching [GroceryCategory]
     *   3. the `AppIcon.DEFAULT` fallback (generic label icon)
     */
    fun displayIconKeyForCategory(category: CategoryEntity): String {
        val stored = category.iconKey?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        val match = GroceryCategory.entries.firstOrNull {
            normalizedCategoryName(it.title) == normalizedCategoryName(category.name)
        }
        return match?.defaultIconKey ?: "label"
    }

    // MARK: - Carts (VaultService+Carts.swift)

    /**
     * Port of `createCart(name:budget:)` — starts a cart in `.planning` with the
     * cart background color scaffolding omitted (Phase 5 doesn't persist cart
     * styling yet).
     */
    suspend fun createCart(name: String, budget: Double): CartEntity? {
        val vault = _state.value.vault ?: return null
        val now = Date()
        val cart = CartEntity(
            id = UUID.randomUUID().toString(),
            vaultUid = vault.uid,
            name = name.trim(),
            budget = budget,
            fulfillmentStatus = 0.0,
            createdAt = now,
            updatedAt = now,
            status = CartStatus.PLANNING.raw,
        )
        writeLock.withLock { db.cartDao().insert(cart) }
        // Targeted: only the one new cart needs to appear in the snapshot.
        refreshCart(cart.id)
        return cart
    }

    /**
     * Port of `createCartWithActiveItems(name:budget:activeItems:)`. Keys follow
     * the `itemId||store` format produced by [ActiveItemSelectionKey], matching
     * iOS so shared selection state interoperates.
     */
    suspend fun createCartWithActiveItems(
        name: String,
        budget: Double,
        activeItems: Map<String, Double>,
    ): CartEntity? {
        val vault = _state.value.vault ?: return null

        // Preserve the legacy behaviour where multiple selection keys for the
        // same itemId (different stores) collapse into a single cart row whose
        // store is the first one seen and whose quantity is the sum. That was
        // the net effect of looping addVaultItemToCart per key — it found the
        // existing row each iteration and bumped the quantity in place.
        data class PlannedEntry(val itemId: String, val firstStore: String?, val quantity: Double)
        val planned = LinkedHashMap<String, PlannedEntry>()
        for ((selectionKey, qty) in activeItems) {
            val parsed = ActiveItemSelectionKey.parse(selectionKey)
            val prior = planned[parsed.itemId]
            planned[parsed.itemId] = if (prior == null) {
                PlannedEntry(parsed.itemId, parsed.store, qty)
            } else {
                PlannedEntry(prior.itemId, prior.firstStore, prior.quantity + qty)
            }
        }
        val itemIds = planned.keys.toList()

        val now = Date()
        val newCart = CartEntity(
            id = UUID.randomUUID().toString(),
            vaultUid = vault.uid,
            name = name.trim(),
            budget = budget,
            fulfillmentStatus = 0.0,
            createdAt = now,
            updatedAt = now,
            status = CartStatus.PLANNING.raw,
        )

        val result = writeLock.withLock {
            db.withTransaction {
                db.cartDao().insert(newCart)

                if (itemIds.isNotEmpty()) {
                    // Three batched reads up-front; nothing per-item.
                    val itemsById = db.itemDao().findByIds(itemIds).associateBy { it.id }
                    val priceOptionsByItem = priceOptionsFor(itemIds)
                    val categoryUids = itemsById.values.mapNotNull { it.categoryUid }.distinct()
                    val categoriesByUid = if (categoryUids.isEmpty()) emptyMap()
                    else db.categoryDao().findByUids(categoryUids).associateBy { it.uid }

                    for (entry in planned.values) {
                        val item = itemsById[entry.itemId] ?: continue
                        val options = priceOptionsByItem[entry.itemId].orEmpty()
                        val store = entry.firstStore?.trim()?.takeIf { it.isNotEmpty() }
                            ?: options.firstOrNull()?.store
                            ?: "Unknown Store"
                        val matching = options.firstOrNull { it.store == store }
                        val categoryName = item.categoryUid?.let { categoriesByUid[it]?.name }
                        db.cartItemDao().insert(
                            CartItemEntity(
                                uid = UUID.randomUUID().toString(),
                                cartId = newCart.id,
                                itemId = item.id,
                                addedAt = Date(),
                                quantity = entry.quantity,
                                isFulfilled = false,
                                plannedStore = store,
                                plannedPrice = matching?.pricePerUnit?.priceValue,
                                plannedUnit = matching?.pricePerUnit?.unit,
                                vaultItemNameSnapshot = item.name,
                                vaultItemCategorySnapshot = categoryName,
                                addedDuringShopping = false,
                            ),
                        )
                    }
                }

                recomputeCartTotalsInternal(newCart.id)
            }
        }
        // Targeted: only the new cart + its items need to land in the snapshot.
        refreshCart(newCart.id)
        return result
    }

    /**
     * Port of `addVaultItemToCart(item:cart:quantity:selectedStore:)`. Planning
     * cart only in Phase 5. If the item is already in the cart, adds to its
     * quantity; otherwise inserts a new row with planned store/price/unit
     * captured from the vault price options.
     */
    suspend fun addVaultItemToCart(
        cartId: String,
        item: ItemEntity,
        quantity: Double,
        selectedStore: String? = null,
    ): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        val status = CartStatus.fromRaw(cart.status)
        if (status == CartStatus.COMPLETED) return false

        val priceOptions = db.priceOptionDao().listForItem(item.id)
        val store = selectedStore?.trim()?.takeIf { it.isNotEmpty() }
            ?: priceOptions.firstOrNull()?.store
            ?: "Unknown Store"
        val matchingPrice = priceOptions.firstOrNull { it.store == store }
        val categoryName = item.categoryUid?.let { db.categoryDao().findByUid(it)?.name }

        writeLock.withLock {
            val existing = db.cartItemDao().findByCartAndItem(cartId, item.id)
            if (existing != null && !existing.isShoppingOnlyItem) {
                db.cartItemDao().update(
                    existing.copy(
                        quantity = existing.quantity + quantity,
                        addedAt = Date(),
                    ),
                )
            } else {
                val isShopping = status == CartStatus.SHOPPING
                db.cartItemDao().insert(
                    CartItemEntity(
                        uid = UUID.randomUUID().toString(),
                        cartId = cartId,
                        itemId = item.id,
                        addedAt = Date(),
                        quantity = quantity,
                        isFulfilled = false,
                        plannedStore = store,
                        plannedPrice = matchingPrice?.pricePerUnit?.priceValue,
                        plannedUnit = matchingPrice?.pricePerUnit?.unit,
                        actualStore = if (isShopping) store else null,
                        actualPrice = if (isShopping) matchingPrice?.pricePerUnit?.priceValue else null,
                        actualQuantity = if (isShopping) quantity else null,
                        actualUnit = if (isShopping) matchingPrice?.pricePerUnit?.unit else null,
                        vaultItemNameSnapshot = item.name,
                        vaultItemCategorySnapshot = categoryName,
                        addedDuringShopping = isShopping,
                    ),
                )
            }
        }
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `removeItemFromCart(cart:itemId:)` — planning branch. Shopping
     * behaviour (skip vs remove) lands with the shopping phase.
     */
    suspend fun removeItemFromCart(cartId: String, itemId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.PLANNING) return false

        writeLock.withLock {
            db.cartItemDao().deleteByCartAndItem(cartId, itemId)
        }
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `changeCartItemStore(cart:itemId:newStore:)` — planning branch.
     * Looks up price + unit from the matching [PriceOptionEntity] and updates
     * the planned snapshot accordingly.
     */
    suspend fun changeCartItemStore(cartId: String, itemId: String, newStore: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.PLANNING) return false

        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false
        val priceOptions = db.priceOptionDao().listForItem(itemId)
        val match = priceOptions.firstOrNull { it.store == newStore }

        writeLock.withLock {
            db.cartItemDao().update(
                existing.copy(
                    plannedStore = newStore.trim(),
                    plannedPrice = match?.pricePerUnit?.priceValue ?: existing.plannedPrice,
                    plannedUnit = match?.pricePerUnit?.unit ?: existing.plannedUnit,
                ),
            )
        }
        ensureStoreExists(newStore)
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Planning-mode counterpart to iOS `updateCartItemActualData(cart:itemId:)`.
     * Updates the cart item's planned snapshot (quantity / price / unit / store)
     * without touching the vault price options.
     */
    suspend fun updateCartItemPlannedData(
        cartId: String,
        itemId: String,
        plannedQuantity: Double? = null,
        plannedPrice: Double? = null,
        plannedUnit: String? = null,
        plannedStore: String? = null,
    ): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.PLANNING) return false

        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false
        val normalizedUnit = plannedUnit?.let(UnitSemantics::canonicalUnit)
        val newStore = plannedStore?.trim()?.takeIf { it.isNotEmpty() }

        writeLock.withLock {
            db.cartItemDao().update(
                existing.copy(
                    quantity = plannedQuantity?.coerceAtLeast(0.0) ?: existing.quantity,
                    plannedPrice = plannedPrice ?: existing.plannedPrice,
                    plannedUnit = normalizedUnit ?: existing.plannedUnit,
                    plannedStore = newStore ?: existing.plannedStore,
                ),
            )
        }
        if (newStore != null) ensureStoreExists(newStore)
        recomputeCartTotals(cartId)
        return true
    }

    /** Port of `updateCartName(cart:newName:)`. */
    suspend fun updateCartName(cartId: String, newName: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        writeLock.withLock {
            db.cartDao().update(cart.copy(name = trimmed, updatedAt = Date()))
        }
        // Cart items and the rest of the snapshot are untouched.
        refreshCartRow(cartId)
        return true
    }

    /** Port of `updateCartBudget(cart:newBudget:)`. */
    suspend fun updateCartBudget(cartId: String, newBudget: Double): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (newBudget < 0.0) return false
        writeLock.withLock {
            db.cartDao().update(cart.copy(budget = newBudget, updatedAt = Date()))
        }
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `deleteCart(_:)`. Planning carts are hard-deleted; completed carts
     * go to trash. Phase 5 only exercises the planning path.
     */
    suspend fun deleteCart(cartId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        val status = CartStatus.fromRaw(cart.status)
        writeLock.withLock {
            if (status == CartStatus.COMPLETED) {
                db.cartDao().update(cart.copy(isDeleted = true, deletedAt = Date(), updatedAt = Date()))
            } else {
                // Room's FK cascade also removes the child cart_items for planning carts.
                db.cartDao().delete(cart)
            }
        }
        // Targeted: completed carts move from `carts` to `deletedCarts`; planning
        // carts disappear entirely. applyCartPatch handles both via the nullable/
        // isDeleted cases.
        refreshCart(cartId)
        return true
    }

    /**
     * Port of iOS `restoreDeletedCart(cartId:)` (`VaultService+Carts.swift`).
     * Clears the soft-delete flags; the cart reappears in the active list at
     * its previous status (always `.completed` in Phase 5+6 since planning
     * carts are hard-deleted).
     */
    suspend fun restoreDeletedCart(cartId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (!cart.isDeleted) return false
        writeLock.withLock {
            db.cartDao().update(
                cart.copy(isDeleted = false, deletedAt = null, updatedAt = Date()),
            )
        }
        // Targeted: cart moves from `deletedCarts` back to `carts`; its items are
        // still in `cartItemsByCart[cartId]` if the map kept them.
        refreshCart(cartId)
        return true
    }

    /**
     * Port of iOS `permanentlyDeleteCartFromTrash(cartId:)`. Hard-deletes the
     * cart row (and cascades `cart_items` via the Room foreign-key rule).
     */
    suspend fun permanentlyDeleteCart(cartId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        writeLock.withLock { db.cartDao().delete(cart) }
        // Cart + cart_items (via FK cascade) are gone — refreshCart's null-path
        // clears them from both lists and evicts cartItemsByCart[cartId].
        refreshCart(cartId)
        return true
    }

    /**
     * Port of iOS `restoreDeletedItem(itemId:restoreToActiveCarts:)`. Phase 10
     * keeps the behaviour minimal: clear soft-delete flags and rehome the item
     * under its previous category (iOS also re-inserts `DeletedCartItemSnapshot`
     * rows into active carts when `restoreToActiveCarts = true`; we match the
     * default `false` path since those snapshots are unused pending the trash
     * phase).
     */
    suspend fun restoreDeletedItem(itemId: String): Boolean {
        val item = db.itemDao().findById(itemId) ?: return false
        if (!item.isDeleted) return false
        val vaultUid = item.vaultUid

        val restoredCategoryUid = writeLock.withLock {
            db.withTransaction {
                val targetCategoryName = item.deletedFromCategoryName
                val targetCategoryUid = targetCategoryName
                    ?.let { db.categoryDao().findByName(vaultUid, it)?.uid }
                    ?: run {
                        // Category got renamed / deleted — rehome into the first
                        // available category (iOS falls back to `GroceryCategory.allCases.first.title`).
                        val fallbackName = GroceryCategory.entries.first().title
                        resolveOrCreateCategory(vaultUid, fallbackName)?.uid
                    }
                db.itemDao().update(
                    item.copy(
                        isDeleted = false,
                        deletedAt = null,
                        deletedFromCategoryName = null,
                        categoryUid = targetCategoryUid,
                    ),
                )
                targetCategoryUid
            }
        }
        publishSnapshotRefresh()
        return restoredCategoryUid != null
    }

    /**
     * Port of `updateCartTotals(cart:)`. Recomputes `fulfillmentStatus` using
     * the cart-status-specific rules and persists the new total in a single
     * update. Returns the refreshed cart entity for callers that need it.
     */
    suspend fun recomputeCartTotals(cartId: String): CartEntity? {
        val result = writeLock.withLock {
            db.withTransaction { recomputeCartTotalsInternal(cartId) }
        }
        // Targeted refresh: only the one cart's row + items changed. Other carts,
        // vault items, categories, stores, and trash are all untouched so the rest
        // of the snapshot can be reused via structural sharing.
        refreshCart(cartId)
        return result
    }

    /**
     * Lock-free / snapshot-free inner body of [recomputeCartTotals]. Callers are
     * responsible for holding [writeLock] and for calling [publishSnapshotRefresh]
     * exactly once after their batch completes. Used by multi-step transactions
     * that already hold the lock — nesting [writeLock] would deadlock the mutex,
     * and publishing intermediate snapshots would undo the transactional batching.
     */
    private suspend fun recomputeCartTotalsInternal(cartId: String): CartEntity? {
        val cart = db.cartDao().findById(cartId) ?: return null
        val items = db.cartItemDao().listByCart(cartId)
        val status = CartStatus.fromRaw(cart.status)

        val totalSpent = computeTotalSpent(status, items)
        val fulfillment = when (status) {
            CartStatus.PLANNING -> if (cart.budget > 0.0) minOf(totalSpent / cart.budget, 1.0) else 0.0
            CartStatus.SHOPPING -> {
                val count = items.size
                if (count == 0) 0.0 else items.count { it.isFulfilled }.toDouble() / count
            }
            CartStatus.COMPLETED -> 1.0
        }

        val updated = cart.copy(fulfillmentStatus = fulfillment, updatedAt = Date())
        db.cartDao().update(updated)
        return updated
    }

    /**
     * Mirrors iOS `Cart.totalSpent` — the planning branch uses `plannedPrice *
     * quantity`; the shopping/completed branches inspect fulfillment + skipped
     * flags. Shopping-only cart items use their `shoppingOnly*` fields.
     */
    fun computeTotalSpent(status: CartStatus, items: List<CartItemEntity>): Double {
        var total = 0.0
        for (item in items) {
            val (price, qty) = when (status) {
                CartStatus.PLANNING -> resolvePlanningPriceAndQty(item)
                CartStatus.SHOPPING -> resolveShoppingPriceAndQty(item) ?: continue
                CartStatus.COMPLETED -> resolveCompletedPriceAndQty(item) ?: continue
            }
            total += price * qty
        }
        return total
    }

    private fun resolvePlanningPriceAndQty(item: CartItemEntity): Pair<Double, Double> {
        if (item.isShoppingOnlyItem) return (item.shoppingOnlyPrice ?: 0.0) to item.quantity
        return (item.plannedPrice ?: 0.0) to item.quantity
    }

    private fun resolveShoppingPriceAndQty(item: CartItemEntity): Pair<Double, Double>? {
        if (item.isSkippedDuringShopping) return null
        val price = if (item.isFulfilled || item.wasEditedDuringShopping) {
            item.actualPrice ?: item.plannedPrice ?: 0.0
        } else {
            item.plannedPrice ?: 0.0
        }
        val qty = if (item.isFulfilled || item.wasEditedDuringShopping) {
            item.actualQuantity ?: item.quantity
        } else item.quantity
        return price to qty
    }

    private fun resolveCompletedPriceAndQty(item: CartItemEntity): Pair<Double, Double>? {
        if (item.isSkippedDuringShopping) return null
        val price = item.actualPrice ?: item.plannedPrice ?: 0.0
        val qty = item.actualQuantity ?: item.quantity
        return price to qty
    }

    // MARK: - Shopping mode (VaultService+Carts.swift shopping branch)

    /**
     * Port of `startShopping(cart:)`. Transitions a planning cart into shopping:
     * cleans up stale shopping-only rows, snapshots `originalPlanningQuantity`
     * for every vault item, captures planned price/unit from the current vault
     * where missing, then flips status + `startedAt`.
     */
    suspend fun startShopping(cartId: String): Boolean {
        val result = writeLock.withLock {
            db.withTransaction {
                val cart = db.cartDao().findById(cartId) ?: return@withTransaction false
                if (CartStatus.fromRaw(cart.status) != CartStatus.PLANNING) return@withTransaction false

                val items = db.cartItemDao().listByCart(cartId)
                // cleanupShoppingOnlyItems — hard-delete the shopping-only additions.
                for (item in items.filter { it.isShoppingOnlyItem }) {
                    db.cartItemDao().delete(item)
                }
                val remaining = items.filterNot { it.isShoppingOnlyItem }

                // Batched price-option fetch: 1 query instead of N.
                val priceOptionsByItem = priceOptionsFor(remaining.map { it.itemId })

                for (item in remaining) {
                    // capturePlannedData: fill in planned price/unit from current vault price options.
                    val match = priceOptionsByItem[item.itemId]
                        .orEmpty()
                        .firstOrNull { it.store == item.plannedStore }
                    db.cartItemDao().update(
                        item.copy(
                            originalPlanningQuantity = item.originalPlanningQuantity ?: item.quantity,
                            plannedPrice = item.plannedPrice ?: match?.pricePerUnit?.priceValue,
                            plannedUnit = item.plannedUnit ?: match?.pricePerUnit?.unit,
                        ),
                    )
                }
                val now = Date()
                db.cartDao().update(
                    cart.copy(
                        status = CartStatus.SHOPPING.raw,
                        startedAt = now,
                        updatedAt = now,
                    ),
                )
                recomputeCartTotalsInternal(cartId)
                true
            }
        }
        if (result) refreshCart(cartId)
        return result
    }

    /**
     * Port of `returnToPlanning(cart:)`. Drops every row added during shopping
     * (both `addedDuringShopping` vault items and `isShoppingOnlyItem`) and
     * resets the remaining items: clears actuals/fulfilled/skipped/wasEdited,
     * restores `originalPlanningQuantity`, re-samples planned price/unit from
     * the current vault, then flips status back to `.planning`.
     */
    suspend fun returnToPlanning(cartId: String): Boolean {
        val result = writeLock.withLock {
            db.withTransaction {
                val cart = db.cartDao().findById(cartId) ?: return@withTransaction false
                if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return@withTransaction false

                val items = db.cartItemDao().listByCart(cartId)
                // Batched price-option fetch for all survivors: 1 query instead of N.
                val survivors = items.filterNot { it.addedDuringShopping || it.isShoppingOnlyItem }
                val priceOptionsByItem = priceOptionsFor(survivors.map { it.itemId })

                for (item in items) {
                    if (item.addedDuringShopping || item.isShoppingOnlyItem) {
                        db.cartItemDao().delete(item)
                        continue
                    }
                    val match = priceOptionsByItem[item.itemId]
                        .orEmpty()
                        .firstOrNull { it.store == item.plannedStore }
                    val restoredQuantity = item.originalPlanningQuantity ?: item.quantity
                    db.cartItemDao().update(
                        item.copy(
                            quantity = restoredQuantity,
                            originalPlanningQuantity = null,
                            isFulfilled = false,
                            isSkippedDuringShopping = false,
                            wasEditedDuringShopping = false,
                            addedDuringShopping = false,
                            actualPrice = null,
                            actualQuantity = null,
                            actualUnit = null,
                            actualStore = null,
                            plannedPrice = match?.pricePerUnit?.priceValue ?: item.plannedPrice,
                            plannedUnit = match?.pricePerUnit?.unit ?: item.plannedUnit,
                        ),
                    )
                }
                db.cartDao().update(
                    cart.copy(
                        status = CartStatus.PLANNING.raw,
                        startedAt = null,
                        updatedAt = Date(),
                    ),
                )
                recomputeCartTotalsInternal(cartId)
                true
            }
        }
        if (result) refreshCart(cartId)
        return result
    }

    /**
     * Port of `completeShopping(cart:)`. Fills actual fields from planned
     * fallbacks via `captureActualData`, writes back vault prices for fulfilled
     * non-shopping-only items via [writeBackVaultPrices], snapshots the vault
     * name/category on each cart item, then flips status + `completedAt`.
     */
    suspend fun completeShopping(cartId: String): Boolean {
        val result = writeLock.withLock {
            db.withTransaction {
                val cart = db.cartDao().findById(cartId) ?: return@withTransaction false
                if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return@withTransaction false

                val items = db.cartItemDao().listByCart(cartId)

                // Batched pre-fetch: all vault items + their categories needed to
                // backfill the name/category snapshots. 1+1 queries instead of N*2.
                val vaultItemIds = items
                    .filterNot { it.isShoppingOnlyItem }
                    .map { it.itemId }
                    .distinct()
                val itemsById = if (vaultItemIds.isEmpty()) emptyMap()
                else db.itemDao().findByIds(vaultItemIds).associateBy { it.id }
                val categoryUids = itemsById.values.mapNotNull { it.categoryUid }.distinct()
                val categoriesByUid = if (categoryUids.isEmpty()) emptyMap()
                else db.categoryDao().findByUids(categoryUids).associateBy { it.uid }

                for (item in items) {
                    val capturedStore = item.actualStore ?: item.plannedStore
                    val capturedPrice = item.actualPrice ?: item.plannedPrice
                    val capturedQuantity = item.actualQuantity ?: item.quantity
                    val capturedUnit = item.actualUnit ?: item.plannedUnit

                    var nameSnapshot = item.vaultItemNameSnapshot
                    var categorySnapshot = item.vaultItemCategorySnapshot
                    if (!item.isShoppingOnlyItem) {
                        val vaultItem = itemsById[item.itemId]
                        if (nameSnapshot == null) nameSnapshot = vaultItem?.name
                        if (categorySnapshot == null) {
                            categorySnapshot = vaultItem?.categoryUid?.let { categoriesByUid[it]?.name }
                        }
                    }

                    db.cartItemDao().update(
                        item.copy(
                            actualStore = capturedStore,
                            actualPrice = capturedPrice,
                            actualQuantity = capturedQuantity,
                            actualUnit = capturedUnit,
                            vaultItemNameSnapshot = nameSnapshot,
                            vaultItemCategorySnapshot = categorySnapshot,
                        ),
                    )

                    if (!item.isShoppingOnlyItem && item.isFulfilled && !item.isSkippedDuringShopping) {
                        // writeBackVaultPrices uses the indexed unique (itemId,store)
                        // lookup, so the per-item cost is a single row read — no N+1.
                        writeBackVaultPrices(
                            itemId = item.itemId,
                            actualStore = capturedStore,
                            actualPrice = capturedPrice,
                            actualUnit = capturedUnit,
                        )
                    }
                }
                val now = Date()
                db.cartDao().update(
                    cart.copy(
                        status = CartStatus.COMPLETED.raw,
                        completedAt = now,
                        updatedAt = now,
                    ),
                )
                recomputeCartTotalsInternal(cartId)
                true
            }
        }
        if (result) publishSnapshotRefresh()
        return result
    }

    /**
     * Port of `reopenCart(cart:)`. Clears actuals + unfulfilled everything so
     * the user can re-confirm values at the current vault prices. Paywall
     * active-cart-limit check is intentionally omitted.
     */
    suspend fun reopenCart(cartId: String): Boolean {
        val result = writeLock.withLock {
            db.withTransaction {
                val cart = db.cartDao().findById(cartId) ?: return@withTransaction false
                if (CartStatus.fromRaw(cart.status) != CartStatus.COMPLETED) return@withTransaction false

                val items = db.cartItemDao().listByCart(cartId)
                for (item in items) {
                    db.cartItemDao().update(
                        item.copy(
                            actualPrice = null,
                            actualQuantity = null,
                            actualUnit = null,
                            actualStore = null,
                            isFulfilled = false,
                        ),
                    )
                }
                db.cartDao().update(
                    cart.copy(
                        status = CartStatus.SHOPPING.raw,
                        completedAt = null,
                        updatedAt = Date(),
                        isDeleted = false,
                        deletedAt = null,
                    ),
                )
                recomputeCartTotalsInternal(cartId)
                true
            }
        }
        if (result) refreshCart(cartId)
        return result
    }

    /** Port of `toggleItemFulfillment(cart:itemId:)`. */
    suspend fun toggleItemFulfillment(cartId: String, itemId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false

        writeLock.withLock {
            db.cartItemDao().update(existing.copy(isFulfilled = !existing.isFulfilled))
            db.cartDao().update(cart.copy(updatedAt = Date()))
        }
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `updateCartItemActualData(cart:itemId:actualPrice:actualQuantity:actualUnit:actualStore:)`.
     * Shopping-only. Also flips `wasEditedDuringShopping` when the item isn't
     * yet fulfilled, matching iOS `CartItem.updateShoppingData` semantics.
     */
    suspend fun updateCartItemActualData(
        cartId: String,
        itemId: String,
        actualPrice: Double? = null,
        actualQuantity: Double? = null,
        actualUnit: String? = null,
        actualStore: String? = null,
    ): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false

        val normalizedUnit = actualUnit?.let(UnitSemantics::canonicalUnit)
        val trimmedStore = actualStore?.trim()?.takeIf { it.isNotEmpty() }
        val shouldFlagEdited = !existing.isFulfilled &&
            (actualPrice != null || actualQuantity != null || actualUnit != null || actualStore != null)

        writeLock.withLock {
            db.cartItemDao().update(
                existing.copy(
                    actualPrice = actualPrice ?: existing.actualPrice,
                    actualQuantity = actualQuantity ?: existing.actualQuantity,
                    actualUnit = normalizedUnit ?: existing.actualUnit,
                    actualStore = trimmedStore ?: existing.actualStore,
                    wasEditedDuringShopping = existing.wasEditedDuringShopping || shouldFlagEdited,
                ),
            )
        }
        if (trimmedStore != null) ensureStoreExists(trimmedStore)
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Shopping-branch port of `changeCartItemStore(cart:itemId:newStore:)`. Uses
     * the actual store + looks up the matching vault price option for auto-fill.
     */
    suspend fun changeCartItemStoreShopping(cartId: String, itemId: String, newStore: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false

        val priceOptions = db.priceOptionDao().listForItem(itemId)
        val match = priceOptions.firstOrNull { it.store == newStore }
        writeLock.withLock {
            db.cartItemDao().update(
                existing.copy(
                    actualStore = newStore.trim(),
                    actualPrice = match?.pricePerUnit?.priceValue ?: existing.actualPrice,
                    actualUnit = match?.pricePerUnit?.unit ?: existing.actualUnit,
                ),
            )
        }
        ensureStoreExists(newStore)
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of the "skip" branch in `removeItemFromCart(cart:itemId:)` — during
     * shopping a vault item is flagged as skipped instead of being removed so
     * the row can be un-skipped later. Shopping-only items are hard-deleted.
     */
    suspend fun skipCartItemDuringShopping(cartId: String, itemId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false

        writeLock.withLock {
            if (existing.isShoppingOnlyItem) {
                db.cartItemDao().delete(existing)
            } else {
                db.cartItemDao().update(
                    existing.copy(
                        isSkippedDuringShopping = true,
                        isFulfilled = false,
                    ),
                )
            }
        }
        recomputeCartTotals(cartId)
        return true
    }

    /** Clears the skip flag so a previously-skipped row reappears live. */
    suspend fun unskipCartItem(cartId: String, itemId: String): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val existing = db.cartItemDao().findByCartAndItem(cartId, itemId) ?: return false

        writeLock.withLock {
            db.cartItemDao().update(existing.copy(isSkippedDuringShopping = false))
        }
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `addVaultItemToCartDuringShopping(item:store:price:unit:cart:quantity:)`.
     * Either bumps an existing vault-item row (clearing any skip flag) or
     * inserts a new one with actual fields pre-filled and
     * `addedDuringShopping = true`.
     */
    suspend fun addVaultItemToCartDuringShopping(
        cartId: String,
        item: ItemEntity,
        store: String,
        price: Double,
        unit: String,
        quantity: Double = 1.0,
    ): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false

        val normalizedUnit = UnitSemantics.canonicalUnit(unit)
        writeLock.withLock {
            val existing = db.cartItemDao().findByCartAndItem(cartId, item.id)
            if (existing != null && !existing.isShoppingOnlyItem) {
                db.cartItemDao().update(
                    existing.copy(
                        quantity = existing.quantity + quantity,
                        isSkippedDuringShopping = false,
                        addedAt = Date(),
                        addedDuringShopping = true,
                    ),
                )
            } else {
                db.cartItemDao().insert(
                    CartItemEntity(
                        uid = UUID.randomUUID().toString(),
                        cartId = cartId,
                        itemId = item.id,
                        addedAt = Date(),
                        quantity = quantity,
                        isFulfilled = false,
                        plannedStore = store,
                        plannedPrice = price,
                        plannedUnit = normalizedUnit,
                        actualStore = store,
                        actualPrice = price,
                        actualQuantity = quantity,
                        actualUnit = normalizedUnit,
                        vaultItemNameSnapshot = item.name,
                        vaultItemCategorySnapshot = item.categoryUid
                            ?.let { db.categoryDao().findByUid(it)?.name },
                        addedDuringShopping = true,
                    ),
                )
            }
        }
        ensureStoreExists(store)
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `addShoppingItemToCart(name:store:price:unit:cart:quantity:...)`
     * — the shopping-only variant. Stores the optional image URI the same way
     * the Vault item flow does; `categoryName` is stored verbatim in
     * `shoppingOnlyCategory` (iOS stores either the [GroceryCategory.rawValue]
     * or the custom category name here, which our port also honours).
     */
    suspend fun addShoppingOnlyItemToCart(
        cartId: String,
        name: String,
        store: String,
        price: Double,
        unit: String,
        quantity: Double = 1.0,
        categoryName: String? = null,
        imageUri: String? = null,
    ): Boolean {
        val cart = db.cartDao().findById(cartId) ?: return false
        if (CartStatus.fromRaw(cart.status) != CartStatus.SHOPPING) return false
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false

        val normalizedUnit = UnitSemantics.canonicalUnit(unit)
        val categoryIdentifier = categoryName?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            GroceryCategory.entries.firstOrNull { it.title.equals(raw, ignoreCase = true) }?.rawValue ?: raw
        }

        writeLock.withLock {
            val syntheticId = UUID.randomUUID().toString()
            db.cartItemDao().insert(
                CartItemEntity(
                    uid = UUID.randomUUID().toString(),
                    cartId = cartId,
                    itemId = syntheticId,
                    addedAt = Date(),
                    quantity = quantity,
                    isFulfilled = false,
                    plannedStore = store,
                    plannedPrice = price,
                    plannedUnit = normalizedUnit,
                    actualStore = store,
                    actualPrice = price,
                    actualQuantity = quantity,
                    actualUnit = normalizedUnit,
                    isShoppingOnlyItem = true,
                    shoppingOnlyName = trimmedName,
                    shoppingOnlyStore = store,
                    shoppingOnlyPrice = price,
                    shoppingOnlyUnit = normalizedUnit,
                    shoppingOnlyCategory = categoryIdentifier,
                    shoppingOnlyImageUri = imageUri,
                    addedDuringShopping = true,
                ),
            )
        }
        ensureStoreExists(store)
        recomputeCartTotals(cartId)
        return true
    }

    /**
     * Port of `updateVaultWithActualData(cartItem:)`. When the cart completes
     * with a fulfilled vault item we update (or create) the PriceOption for the
     * `actualStore` so the vault reflects what the user actually paid.
     */
    private suspend fun writeBackVaultPrices(
        itemId: String,
        actualStore: String?,
        actualPrice: Double?,
        actualUnit: String?,
    ) {
        if (actualStore.isNullOrBlank() || actualPrice == null || actualUnit == null) return
        // Single indexed row lookup against the unique (itemId,store) index rather
        // than fetching every price option for the item.
        val existing = db.priceOptionDao().findByItemAndStore(itemId, actualStore)
        val normalizedUnit = UnitSemantics.canonicalUnit(actualUnit)
        if (existing != null) {
            val isContinuous = UnitSemantics.isContinuous(normalizedUnit)
            db.priceOptionDao().update(
                existing.copy(
                    pricePerUnit = existing.pricePerUnit.copy(
                        priceValue = actualPrice,
                        unit = normalizedUnit,
                        packageSizeValue = if (isContinuous) null else existing.pricePerUnit.packageSizeValue,
                        packageSizeUnit = if (isContinuous) null else existing.pricePerUnit.packageSizeUnit,
                    ),
                ),
            )
        } else {
            db.priceOptionDao().insert(
                PriceOptionEntity(
                    uid = UUID.randomUUID().toString(),
                    itemId = itemId,
                    store = actualStore,
                    pricePerUnit = PricePerUnit(
                        priceValue = actualPrice,
                        unit = normalizedUnit,
                    ),
                ),
            )
        }
        val vaultUid = _state.value.vault?.uid ?: return
        insertStoreIfMissing(vaultUid, actualStore)
    }

    // MARK: - Insights (VaultService+Carts.swift::getCartInsights)

    data class PriceChange(
        val itemName: String,
        val plannedPrice: Double,
        val actualPrice: Double,
        val difference: Double,
    )

    data class CartInsights(
        val plannedTotal: Double = 0.0,
        val actualTotal: Double = 0.0,
        val totalDifference: Double = 0.0,
        val priceChanges: List<PriceChange> = emptyList(),
    ) {
        val isOverBudget: Boolean get() = totalDifference > 0.0
        val savings: Double get() = maxOf(0.0, -totalDifference)
        val overspend: Double get() = maxOf(0.0, totalDifference)
    }

    /**
     * Port of `getCartInsights(cart:)`. Mirrors the iOS branching on
     * `wasPlanned`, `wasPurchased`, and `isShoppingOnlyItem`; uses the current
     * vault price options as a fallback so unedited items still roll up.
     */
    suspend fun getCartInsights(cartId: String): CartInsights {
        val items = db.cartItemDao().listByCart(cartId)
        var plannedTotal = 0.0
        var actualTotal = 0.0
        val priceChanges = mutableListOf<PriceChange>()

        // Batched pre-fetch: one IN-list query for every vault item referenced,
        // plus one for the fallback name lookups when the name snapshot is missing.
        val vaultItemIds = items.filterNot { it.isShoppingOnlyItem }.map { it.itemId }.distinct()
        val priceOptionsByItem = priceOptionsFor(vaultItemIds)
        val itemsNeedingNameLookup = items
            .filter { !it.isShoppingOnlyItem && it.vaultItemNameSnapshot == null }
            .map { it.itemId }
            .distinct()
        val itemsById = if (itemsNeedingNameLookup.isEmpty()) emptyMap()
        else db.itemDao().findByIds(itemsNeedingNameLookup).associateBy { it.id }

        for (item in items) {
            val wasPlanned = !item.isShoppingOnlyItem && !item.addedDuringShopping
            val vaultOptions = if (!item.isShoppingOnlyItem) {
                priceOptionsByItem[item.itemId].orEmpty()
            } else emptyList()
            val plannedPrice = if (wasPlanned) {
                item.plannedPrice
                    ?: vaultOptions.firstOrNull { it.store == item.plannedStore }?.pricePerUnit?.priceValue
                    ?: 0.0
            } else 0.0
            val plannedQty = if (wasPlanned) maxOf(0.0, item.originalPlanningQuantity ?: item.quantity) else 0.0

            val wasPurchased = item.isFulfilled && !item.isSkippedDuringShopping
            val actualPrice: Double
            val actualQty: Double
            if (wasPurchased) {
                actualPrice = if (item.isShoppingOnlyItem) {
                    item.shoppingOnlyPrice ?: item.actualPrice ?: item.plannedPrice ?: 0.0
                } else {
                    item.actualPrice
                        ?: item.plannedPrice
                        ?: vaultOptions.firstOrNull {
                            it.store == (item.actualStore ?: item.plannedStore)
                        }?.pricePerUnit?.priceValue
                        ?: 0.0
                }
                actualQty = maxOf(0.0, item.actualQuantity ?: item.quantity)
            } else {
                actualPrice = 0.0
                actualQty = 0.0
            }

            val plannedLine = plannedPrice * plannedQty
            val actualLine = actualPrice * actualQty
            val diff = actualLine - plannedLine

            plannedTotal += plannedLine
            actualTotal += actualLine

            if (wasPlanned && wasPurchased && diff != 0.0) {
                val name = if (item.isShoppingOnlyItem) {
                    item.shoppingOnlyName ?: "Unknown"
                } else {
                    item.vaultItemNameSnapshot
                        ?: itemsById[item.itemId]?.name
                        ?: "Unknown"
                }
                priceChanges += PriceChange(
                    itemName = name,
                    plannedPrice = plannedPrice,
                    actualPrice = actualPrice,
                    difference = diff,
                )
            }
        }

        return CartInsights(
            plannedTotal = plannedTotal,
            actualTotal = actualTotal,
            totalDifference = actualTotal - plannedTotal,
            priceChanges = priceChanges.sortedByDescending { kotlin.math.abs(it.difference) },
        )
    }

    // MARK: - Private helpers

    /**
     * Batched price-option fetch grouped by itemId, used wherever a loop would
     * otherwise call [PriceOptionDao.listForItem] once per item. Returns an
     * empty map for empty inputs so callers can always safely index into it.
     */
    private suspend fun priceOptionsFor(itemIds: List<String>): Map<String, List<PriceOptionEntity>> {
        if (itemIds.isEmpty()) return emptyMap()
        val distinct = itemIds.distinct()
        return db.priceOptionDao().listForItems(distinct).groupBy { it.itemId }
    }

    private suspend fun resolveOrCreateCategory(
        vaultUid: String,
        categoryName: String,
    ): CategoryEntity? {
        if (categoryName.isEmpty()) return null

        val existing = db.categoryDao().findByName(vaultUid, categoryName)
        if (existing != null) return existing

        val nextSortOrder = db.categoryDao().listByVault(vaultUid).size
        val created = CategoryEntity(
            uid = UUID.randomUUID().toString(),
            vaultUid = vaultUid,
            name = categoryName,
            iconKey = GroceryCategory.entries.firstOrNull {
                normalizedCategoryName(it.title) == normalizedCategoryName(categoryName)
            }?.defaultIconKey,
            sortOrder = nextSortOrder,
        )
        db.categoryDao().insert(created)
        return created
    }

    private suspend fun insertStoreIfMissing(vaultUid: String, storeName: String): Boolean {
        val trimmed = storeName.trim()
        if (trimmed.isEmpty()) return false
        val existing = db.storeDao().findByName(vaultUid, trimmed)
        if (existing != null) return false

        db.storeDao().insert(
            StoreEntity(
                uid = UUID.randomUUID().toString(),
                vaultUid = vaultUid,
                name = trimmed,
                createdAt = Date(),
            ),
        )
        return true
    }

    private fun normalizedCategoryName(value: String): String =
        value.trim().lowercase()

    private suspend fun publishSnapshot(user: UserEntity, vault: VaultEntity) {
        _state.value = buildSnapshot(user, vault)
    }

    private suspend fun publishSnapshotRefresh() {
        val snapshot = _state.value
        val user = snapshot.user ?: return
        val vault = snapshot.vault ?: return
        _state.value = buildSnapshot(user, vault)
    }

    private suspend fun buildSnapshot(user: UserEntity, vault: VaultEntity): Snapshot {
        val categories = db.categoryDao().listByVault(vault.uid)
        val stores = db.storeDao().listByVault(vault.uid)
        val items = db.itemDao().listActive(vault.uid)
        val priceOptions = db.priceOptionDao().listActiveByVault(vault.uid)
        val activeCarts = db.cartDao().listActiveByVault(vault.uid)
        val cartItems = if (activeCarts.isEmpty()) {
            emptyList()
        } else {
            db.cartItemDao().listForCarts(activeCarts.map { it.id })
        }
        val deletedItems = db.itemDao().listDeleted(vault.uid)
        val deletedCarts = db.cartDao().listDeleted(vault.uid)
        return Snapshot(
            user = user,
            vault = vault,
            categories = categories,
            stores = stores,
            items = items,
            itemsById = items.associateBy { it.id },
            priceOptionsByItem = priceOptions.groupBy { it.itemId },
            carts = activeCarts,
            cartItemsByCart = cartItems.groupBy { it.cartId },
            deletedItems = deletedItems,
            deletedCarts = deletedCarts,
            isLoading = false,
            error = null,
        )
    }

    private suspend fun refreshStoresSnapshot() {
        val snapshot = _state.value
        val vault = snapshot.vault ?: return
        val stores = db.storeDao().listByVault(vault.uid)
        _state.value = snapshot.copy(stores = stores)
    }

    // MARK: - Targeted refreshes (Phase 3)
    //
    // These patch individual slices of [Snapshot] in-place rather than re-reading
    // every table via [buildSnapshot]. The mutex-based writers hand off to them
    // AFTER their transaction commits, so the database reads they do pick up the
    // latest committed state. Patches are applied via [MutableStateFlow.update]
    // (atomic CAS) so concurrent targeted refreshes for different entities are
    // safe against each other.

    /**
     * Refreshes exactly one cart's row + its items in the current snapshot.
     * Handles every transition (create / update / soft-delete / hard-delete /
     * restore) so callers don't need to case-split.
     */
    private suspend fun refreshCart(cartId: String) {
        val cart = db.cartDao().findById(cartId)
        val items = if (cart == null) emptyList() else db.cartItemDao().listByCart(cartId)
        applyCartPatch(cartId, cart, items)
    }

    /**
     * Refreshes only the cart row (preserves the cached `cartItemsByCart` entry).
     * Used when the mutation only touches cart-level fields (name, budget,
     * fulfillment totals) and cannot have changed any cart item.
     */
    private suspend fun refreshCartRow(cartId: String) {
        val cart = db.cartDao().findById(cartId)
        val items = if (cart == null) emptyList() else _state.value.cartItemsByCart[cartId].orEmpty()
        applyCartPatch(cartId, cart, items)
    }

    private fun applyCartPatch(cartId: String, cart: CartEntity?, items: List<CartItemEntity>) {
        _state.update { s -> patchCart(s, cartId, cart, items) }
    }

    /**
     * Refreshes one item's row + its price options. Handles soft-delete /
     * restore transitions between `items` and `deletedItems`, and hard-delete
     * (which also evicts the price-option map entry since FK cascade removes
     * the rows).
     */
    private suspend fun refreshItem(itemId: String) {
        val item = db.itemDao().findById(itemId)
        val priceOptions = if (item == null) emptyList() else db.priceOptionDao().listForItem(itemId)
        applyItemPatch(itemId, item, priceOptions)
    }

    private fun applyItemPatch(
        itemId: String,
        item: ItemEntity?,
        priceOptions: List<PriceOptionEntity>,
    ) {
        _state.update { s -> patchItem(s, itemId, item, priceOptions) }
    }

    companion object {
        /**
         * Pure snapshot patcher exposed for unit tests. Applies one cart's new
         * state (row + items) to [snapshot], preserving every other field and
         * the DAO-matching ordering for `carts` and `deletedCarts`. Passing
         * `cart = null` represents a hard delete: the cart leaves both lists
         * and the `cartItemsByCart` entry is evicted.
         */
        internal fun patchCart(
            snapshot: Snapshot,
            cartId: String,
            cart: CartEntity?,
            items: List<CartItemEntity>,
        ): Snapshot {
            val activeWithout = snapshot.carts.filterNot { it.id == cartId }
            val deletedWithout = snapshot.deletedCarts.filterNot { it.id == cartId }
            val newActive = if (cart != null && !cart.isDeleted) {
                // Match the DAO's ORDER BY createdAt DESC ordering.
                (activeWithout + cart).sortedByDescending { it.createdAt }
            } else activeWithout
            val newDeleted = if (cart != null && cart.isDeleted) {
                // Match the DAO's ORDER BY deletedAt DESC ordering (nulls last).
                (deletedWithout + cart).sortedByDescending { it.deletedAt }
            } else deletedWithout
            val newItemsByCart = if (cart == null) {
                snapshot.cartItemsByCart - cartId
            } else {
                snapshot.cartItemsByCart + (cartId to items)
            }
            return snapshot.copy(
                carts = newActive,
                deletedCarts = newDeleted,
                cartItemsByCart = newItemsByCart,
            )
        }

        /**
         * Pure snapshot patcher exposed for unit tests. Applies one vault
         * item's new state (row + price options) to [snapshot]. `items` and
         * `itemsById` always stay in sync: soft-deleted and hard-deleted rows
         * leave both. `priceOptionsByItem` is updated when the item exists,
         * evicted when it doesn't.
         */
        internal fun patchItem(
            snapshot: Snapshot,
            itemId: String,
            item: ItemEntity?,
            priceOptions: List<PriceOptionEntity>,
        ): Snapshot {
            val activeWithout = snapshot.items.filterNot { it.id == itemId }
            val deletedWithout = snapshot.deletedItems.filterNot { it.id == itemId }
            val newActive = if (item != null && !item.isDeleted) activeWithout + item else activeWithout
            val newDeleted = if (item != null && item.isDeleted) deletedWithout + item else deletedWithout
            val newPriceMap = when {
                item == null -> snapshot.priceOptionsByItem - itemId
                else -> snapshot.priceOptionsByItem + (itemId to priceOptions)
            }
            // `itemsById` mirrors `items` — active rows only. Removed or
            // soft-deleted items drop out of the lookup.
            val newItemsById = when {
                item == null || item.isDeleted -> snapshot.itemsById - itemId
                else -> snapshot.itemsById + (itemId to item)
            }
            return snapshot.copy(
                items = newActive,
                itemsById = newItemsById,
                deletedItems = newDeleted,
                priceOptionsByItem = newPriceMap,
            )
        }
    }

    private suspend fun refreshCategoriesSnapshot() {
        val vault = _state.value.vault ?: return
        val categories = db.categoryDao().listByVault(vault.uid)
        _state.update { s -> s.copy(categories = categories) }
    }

    /**
     * Port of the private `prePopulateCategories(in:)` helper from
     * `VaultService+Domain.swift`. Seeds the default grocery categories in the
     * canonical iOS order.
     */
    private suspend fun prePopulateCategories(vaultUid: String) {
        val seeds = GroceryCategory.entries.mapIndexed { index, category ->
            CategoryEntity(
                uid = UUID.randomUUID().toString(),
                vaultUid = vaultUid,
                name = category.title,
                iconKey = category.defaultIconKey,
                sortOrder = index,
            )
        }
        db.categoryDao().insertAll(seeds)
    }

    /**
     * Port of `ensureAllCategoriesExist(in:)` from `VaultService+Domain.swift`.
     * Keeps the default categories present and correctly ordered while preserving
     * user-created custom categories (they keep trailing `sortOrder`).
     */
    private suspend fun ensureAllCategoriesExist(vaultUid: String) {
        fun key(name: String) = name.trim().lowercase()

        val existing = db.categoryDao().listByVault(vaultUid)
        val existingByKey = existing.associateBy { key(it.name) }
        val defaultTitles = GroceryCategory.entries.map { it.title }
        val defaultKeys = defaultTitles.map(::key).toSet()

        val updates = ArrayList<CategoryEntity>()
        val inserts = ArrayList<CategoryEntity>()

        for ((index, groceryCategory) in GroceryCategory.entries.withIndex()) {
            val categoryName = groceryCategory.title
            val existingCategory = existingByKey[key(categoryName)]
            if (existingCategory != null) {
                if (existingCategory.name != categoryName || existingCategory.sortOrder != index) {
                    updates += existingCategory.copy(name = categoryName, sortOrder = index)
                }
            } else {
                inserts += CategoryEntity(
                    uid = UUID.randomUUID().toString(),
                    vaultUid = vaultUid,
                    name = categoryName,
                    iconKey = groceryCategory.defaultIconKey,
                    sortOrder = index,
                )
            }
        }

        val extras = existing
            .filter { key(it.name) !in defaultKeys }
            .sortedWith(
                compareBy<CategoryEntity> { it.sortOrder }.thenBy { it.name.lowercase() },
            )

        var nextSortOrder = GroceryCategory.entries.size
        for (extra in extras) {
            if (extra.sortOrder < nextSortOrder) {
                updates += extra.copy(sortOrder = nextSortOrder)
            }
            nextSortOrder++
        }

        if (inserts.isNotEmpty()) db.categoryDao().insertAll(inserts)
        for (category in updates) db.categoryDao().update(category)
    }
}
