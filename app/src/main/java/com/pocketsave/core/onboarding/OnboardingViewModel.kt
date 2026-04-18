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

enum class OnboardingStep { WELCOME, LAST_STORE, FIRST_ITEM, DONE }

/**
 * Port of `PocketSave/Core/Onboarding/ViewModel/OnboardingViewModel.swift`.
 *
 * Keeps one instance of [ItemFormViewModel] as its child (matching the iOS
 * `formViewModel` property) and drives the step machine + debounced duplicate
 * check. Animation/haptic controls collapse into a few `mutableStateOf` flags;
 * screens drive their own Compose animations from there.
 */
class OnboardingViewModel(
    private val vaultService: VaultService,
    private val preferences: AppPreferences,
) : ViewModel() {

    val formViewModel = ItemFormViewModel(requiresPortion = false, requiresStore = true)

    var currentStep: OnboardingStep by mutableStateOf(OnboardingStep.WELCOME)
        private set

    var showPageIndicator: Boolean by mutableStateOf(false)
    var showError: Boolean by mutableStateOf(false)
    var duplicateError: String? by mutableStateOf(null)
        private set
    var isCheckingDuplicate: Boolean by mutableStateOf(false)
        private set

    /**
     * Set to true once the first item has been persisted, so the host can swap
     * the Onboarding NavHost destination for Home. Mirrors the role of
     * `UserDefaults.standard.hasCompletedOnboarding = true` on iOS.
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

    // MARK: - Navigation

    fun navigateToWelcome() {
        currentStep = OnboardingStep.WELCOME
        showPageIndicator = false
    }

    fun navigateToLastStore() {
        currentStep = OnboardingStep.LAST_STORE
        showPageIndicator = true
    }

    fun navigateToFirstItemDataScreen() {
        currentStep = OnboardingStep.FIRST_ITEM
    }

    fun navigateToDone() {
        showPageIndicator = false
        currentStep = OnboardingStep.DONE
    }

    fun navigateBack() {
        currentStep = OnboardingStep.LAST_STORE
    }

    // MARK: - Store error (iOS `triggerStoreNameError` without haptics/shake)

    fun triggerStoreNameError() {
        showError = true
        viewModelScope.launch {
            delay(2000)
            showError = false
        }
    }

    // MARK: - Duplicate validation (iOS `checkForDuplicateItemName`)

    /**
     * Port of `checkForDuplicateItemName`. Debounces 500ms by default so every
     * keystroke doesn't hit the database.
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

    /** Port of `validateFinalItemName`. */
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

    // MARK: - Finish

    /**
     * Port of the two iOS calls made by `FirstItemFinishButton`:
     * `viewModel.saveInitialData(vaultService:)` + `viewModel.saveOnboardingItemData()`.
     * Writes the store and the item to Room, persists the onboarding completion
     * flag, then flips [onboardingComplete] so the host can navigate to Home.
     */
    fun finishOnboarding() {
        viewModelScope.launch {
            if (!validateFinalItemName()) return@launch

            val categoryName = formViewModel.selectedCategoryName ?: return@launch
            val price = formViewModel.itemPrice.toDoubleOrNull() ?: return@launch
            val payload = formViewModel.packageSizePayloadForPersistence

            // Persist the first store explicitly so `vault.stores` is populated
            // even if the user backs out before the item save finishes.
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

            preferences.setHasCompletedOnboarding(true)
            preferences.setUserName(vaultService.state.value.user?.name ?: "Default User")
            navigateToDone()
            onboardingComplete = true
        }
    }

    // MARK: - Reset

    fun resetOnboarding() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = null

        currentStep = OnboardingStep.WELCOME
        showPageIndicator = false
        showError = false
        duplicateError = null
        isCheckingDuplicate = false
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
