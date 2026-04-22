package com.pocketsave.core.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.core.itemform.ItemFormViewModel
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.AppPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The eight-step onboarding flow. [WELCOME], [VALUE] and [HANDOFF] are framing
 * moments (no progress bar); [CURRENCY], [MONTHLY_BUDGET], [STORE], [ITEM] and
 * [TRIP] are the data-entry steps the progress bar tracks.
 */
enum class OnboardingStep { WELCOME, VALUE, CURRENCY, MONTHLY_BUDGET, STORE, ITEM, TRIP, HANDOFF }

/**
 * Host for the onboarding state machine and the persistence calls that run as
 * the user advances through the flow. Holds one child [ItemFormViewModel] for
 * the first-item step, and captures the created cart id so the landing Home
 * screen can point its first-run hints at the real trip.
 */
class OnboardingViewModel(
    private val vaultService: VaultService,
    private val preferences: AppPreferences,
) : ViewModel() {

    val formViewModel = ItemFormViewModel(requiresPortion = false, requiresStore = true)

    var currentStep: OnboardingStep by mutableStateOf(OnboardingStep.WELCOME)
        private set

    var showError: Boolean by mutableStateOf(false)
    var duplicateError: String? by mutableStateOf(null)
        private set
    var isCheckingDuplicate: Boolean by mutableStateOf(false)
        private set

    // Currency — persisted at the end of the CURRENCY step. Held in VM state
    // so going back to change it doesn't require re-reading from DataStore.
    var currencyCode: String? by mutableStateOf(null)
        private set
    var currencySymbol: String? by mutableStateOf(null)
        private set

    // Monthly budget — captured on the MONTHLY_BUDGET step. Held as raw text
    // so the field round-trips without losing trailing decimals. Empty means
    // "skip for now" — persisted as 0.0 which the rest of the app reads as
    // "budget not set".
    var monthlyBudgetInput: String by mutableStateOf("")

    // Trip — captured on the TRIP step.
    var tripName: String by mutableStateOf("")
    var tripBudget: String by mutableStateOf("")
    var tripError: String? by mutableStateOf(null)
        private set

    /**
     * Monotonically-incrementing counters that feed
     * [com.pocketsave.core.onboarding.motion.onboardingCelebrationPulse].
     * Flipping a boolean is enough for a single fire, but counters mean we can
     * trigger again later (e.g. if the user backs up and re-saves) without
     * extra state plumbing.
     */
    var itemCelebrationTrigger: Int by mutableStateOf(0)
        private set
    var tripCelebrationTrigger: Int by mutableStateOf(0)
        private set

    /** Set once the ITEM step persists so later steps can attach to it. */
    var createdItemId: String? by mutableStateOf(null)
        private set

    /** Set once the TRIP step persists; read by Home first-run hints. */
    var createdCartId: String? by mutableStateOf(null)
        private set

    /**
     * Flips true once the HANDOFF step is entered. The container has a
     * LaunchedEffect on this flag that invokes the nav callback to Home.
     */
    var onboardingComplete: Boolean by mutableStateOf(false)
        private set

    private var duplicateCheckJob: Job? = null

    // MARK: - Computed

    val calculatedTotal: Double
        get() {
            val portionValue = if (formViewModel.requiresPortion) (formViewModel.portion ?: 0.0) else 1.0
            val priceValue = formViewModel.itemPrice.toDoubleOrNull() ?: 0.0
            return portionValue * priceValue
        }

    val questionText: String
        get() = if (formViewModel.storeName.isEmpty()) {
            "One item you usually buy for grocery"
        } else {
            "One item you bought from ${formViewModel.storeName}"
        }

    val isFormValidForCompletion: Boolean
        get() = formViewModel.isFormValid && duplicateError == null

    /** 0f..1f progress across the five data-entry steps, or null for framing steps. */
    val progressForStep: Float?
        get() = when (currentStep) {
            OnboardingStep.WELCOME, OnboardingStep.VALUE, OnboardingStep.HANDOFF -> null
            OnboardingStep.CURRENCY -> 0.20f
            OnboardingStep.MONTHLY_BUDGET -> 0.40f
            OnboardingStep.STORE -> 0.60f
            OnboardingStep.ITEM -> 0.80f
            OnboardingStep.TRIP -> 1.00f
        }

    // MARK: - Navigation

    fun navigateToWelcome() { currentStep = OnboardingStep.WELCOME }
    fun navigateToValue() { currentStep = OnboardingStep.VALUE }
    fun navigateToCurrency() { currentStep = OnboardingStep.CURRENCY }
    fun navigateToMonthlyBudget() { currentStep = OnboardingStep.MONTHLY_BUDGET }
    fun navigateToStore() { currentStep = OnboardingStep.STORE }
    fun navigateToItem() { currentStep = OnboardingStep.ITEM }
    fun navigateToTrip() { currentStep = OnboardingStep.TRIP }
    fun navigateToHandoff() { currentStep = OnboardingStep.HANDOFF }

    /**
     * Smart back — steps one place closer to Welcome within the 4-step iOS
     * flow. The VALUE / CURRENCY / MONTHLY_BUDGET / TRIP steps are bypassed
     * (they're Android-only and no longer routed to), so their back fallthrough
     * jumps to the closest iOS-visible step.
     */
    fun navigateBack() {
        currentStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.STORE -> OnboardingStep.WELCOME
            OnboardingStep.ITEM -> OnboardingStep.STORE
            OnboardingStep.HANDOFF -> OnboardingStep.HANDOFF
            // Deprecated Android-only steps — jump to the nearest iOS step.
            OnboardingStep.VALUE -> OnboardingStep.WELCOME
            OnboardingStep.CURRENCY -> OnboardingStep.WELCOME
            OnboardingStep.MONTHLY_BUDGET -> OnboardingStep.WELCOME
            OnboardingStep.TRIP -> OnboardingStep.ITEM
        }
    }

    // MARK: - Currency persistence (CURRENCY step)

    fun selectCurrency(code: String, symbol: String) {
        currencyCode = code
        currencySymbol = symbol
    }

    fun commitCurrencyAndContinue() {
        val code = currencyCode
        val symbol = currencySymbol
        if (code.isNullOrBlank() || symbol.isNullOrBlank()) return
        viewModelScope.launch {
            preferences.setCurrency(code, symbol)
            navigateToMonthlyBudget()
        }
    }

    // MARK: - Monthly budget persistence (MONTHLY_BUDGET step)

    /**
     * Persists the entered monthly budget (or clears it if the field is blank
     * or non-numeric — DataStore stores anything <= 0 as "not set") and
     * advances to the store step. Budget is intentionally optional: a blank
     * entry is a valid "skip for now" that the home pill handles gracefully.
     */
    fun commitMonthlyBudgetAndContinue() {
        val parsed = monthlyBudgetInput.trim().toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            preferences.setMonthlyBudget(parsed)
            navigateToStore()
        }
    }

    // MARK: - Store errors (animated error tint in-form)

    fun triggerStoreNameError() {
        showError = true
        viewModelScope.launch {
            delay(2000)
            showError = false
        }
    }

    // MARK: - Duplicate validation

    /**
     * Debounces 500ms by default so every keystroke doesn't hit the database.
     */
    fun checkForDuplicateItemName(itemName: String, debounce: Boolean = true) {
        duplicateCheckJob?.cancel()

        val trimmed = itemName.trim()
        if (trimmed.isEmpty()) {
            duplicateError = null
            isCheckingDuplicate = false
            return
        }

        if (!debounce) {
            duplicateCheckJob = viewModelScope.launch {
                val result = vaultService.validateItemName(
                    name = trimmed,
                    store = formViewModel.storeName,
                    categoryName = formViewModel.selectedCategoryName,
                )
                isCheckingDuplicate = false
                duplicateError = if (result.isValid) null else result.errorMessage
            }
            return
        }

        isCheckingDuplicate = true
        duplicateCheckJob = viewModelScope.launch {
            delay(500)
            val result = vaultService.validateItemName(
                name = trimmed,
                store = formViewModel.storeName,
                categoryName = formViewModel.selectedCategoryName,
            )
            isCheckingDuplicate = false
            duplicateError = if (result.isValid) null else result.errorMessage
        }
    }

    fun clearDuplicateError() {
        duplicateError = null
    }

    suspend fun validateFinalItemName(): Boolean {
        val result = vaultService.validateItemName(
            name = formViewModel.itemName,
            store = formViewModel.storeName,
            categoryName = formViewModel.selectedCategoryName,
        )
        if (!result.isValid) {
            duplicateError = result.errorMessage
            return false
        }
        return true
    }

    // MARK: - Skip

    fun resetForSkip() {
        formViewModel.storeName = ""
    }

    // MARK: - Item step persistence

    /**
     * Persists the first store + first item through [VaultService], fires the
     * save celebration, and advances to the Trip step. The cart + completion
     * flags are set later by [createTripAndFinish].
     *
     * Guards: item name must validate, category + price must be filled. On
     * failure the VM publishes [duplicateError] so the screen renders a hint.
     */
    fun saveItemAndContinueToTrip() {
        viewModelScope.launch {
            if (createdItemId != null) {
                // Re-entering after a back navigation — the item is already
                // persisted. Skip straight to the trip step.
                // iOS flow: FirstItem → Done (Handoff). Trip creation happens
            // post-onboarding from Home instead.
            onboardingComplete = true
                return@launch
            }

            if (!validateFinalItemName()) return@launch

            val categoryName = formViewModel.selectedCategoryName ?: return@launch
            val price = formViewModel.itemPrice.toDoubleOrNull() ?: return@launch
            val payload = formViewModel.packageSizePayloadForPersistence

            vaultService.addStore(formViewModel.storeName)

            val created = vaultService.addItem(
                name = formViewModel.itemName,
                toCategoryName = categoryName,
                store = formViewModel.storeName,
                price = price,
                unit = formViewModel.unit,
                packageSizeValue = payload.value,
                packageSizeUnit = payload.unit,
                packagingMetadata = formViewModel.metadataForPersistence,
            )

            if (created == null) {
                val recheck = vaultService.validateItemName(
                    name = formViewModel.itemName,
                    store = formViewModel.storeName,
                    categoryName = formViewModel.selectedCategoryName,
                )
                duplicateError = recheck.errorMessage ?: "Couldn't save item. Please try again."
                return@launch
            }

            createdItemId = created.id
            itemCelebrationTrigger += 1
            // A gentle default trip name the user can still edit on the next step.
            if (tripName.isBlank()) {
                tripName = suggestedTripName(formViewModel.storeName)
            }
            // iOS flow: FirstItem → Done (Handoff). Trip creation happens
            // post-onboarding from Home instead.
            onboardingComplete = true
        }
    }

    // MARK: - Trip step persistence

    /**
     * Creates the first shopping trip, adds the saved first item to it, marks
     * onboarding complete, and advances to the Handoff step. The container
     * observes [onboardingComplete] and routes to Home once we set it.
     */
    fun createTripAndFinish() {
        val itemId = createdItemId
        if (itemId == null) {
            tripError = "Save your first item before creating a trip."
            return
        }

        val trimmedName = tripName.trim()
        if (trimmedName.isEmpty()) {
            tripError = "Give your trip a name."
            return
        }
        val budget = tripBudget.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            tripError = null

            val cart = vaultService.createCart(name = trimmedName, budget = budget)
            if (cart == null) {
                tripError = "Couldn't start the trip. Please try again."
                return@launch
            }

            val item = vaultService.state.value.items.firstOrNull { it.id == itemId }
            if (item != null) {
                vaultService.addVaultItemToCart(
                    cartId = cart.id,
                    item = item,
                    quantity = 1.0,
                    selectedStore = formViewModel.storeName.ifBlank { null },
                )
            }

            createdCartId = cart.id
            tripCelebrationTrigger += 1
            preferences.setHasCompletedOnboarding(true)
            preferences.setShouldShowFirstRunHints(true)
            preferences.setUserName(vaultService.state.value.user?.name ?: "Default User")
            navigateToHandoff()
            onboardingComplete = true
        }
    }

    private fun suggestedTripName(storeName: String): String {
        if (storeName.isBlank()) return "Weekly shop"
        return "$storeName run"
    }

    // MARK: - Reset

    fun resetOnboarding() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = null

        currentStep = OnboardingStep.WELCOME
        showError = false
        duplicateError = null
        isCheckingDuplicate = false
        currencyCode = null
        currencySymbol = null
        monthlyBudgetInput = ""
        tripName = ""
        tripBudget = ""
        tripError = null
        itemCelebrationTrigger = 0
        tripCelebrationTrigger = 0
        createdItemId = null
        createdCartId = null
        onboardingComplete = false
        formViewModel.resetForm()
    }

    // MARK: - Factory

    class Factory(
        private val vaultService: VaultService,
        private val preferences: AppPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            return OnboardingViewModel(vaultService, preferences) as T
        }
    }
}
