package com.pocketsave.core.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.core.onboarding.components.CategoryPicker
import com.pocketsave.core.onboarding.components.ItemNameField
import com.pocketsave.core.onboarding.components.PackageSizeFields
import com.pocketsave.core.onboarding.components.PriceField
import com.pocketsave.core.onboarding.components.UnitPicker
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection
import com.pocketsave.core.onboarding.motion.onboardingCelebrationPulse
import com.pocketsave.domain.semantics.UnitSemantics
import kotlinx.coroutines.flow.drop

/**
 * First item step. Pre-fills the store pill from the previous step so the
 * user is reminded of the context. The save CTA kicks off persistence through
 * [OnboardingViewModel.saveItemAndContinueToTrip] — on success the screen
 * celebrates briefly via [onboardingCelebrationPulse] before the step
 * transition swaps in the Trip screen.
 */
@Composable
fun OnboardingFirstItemScreen(viewModel: OnboardingViewModel) {
    val form = viewModel.formViewModel

    // Debounced duplicate check whenever the item name changes. Drop(1) so we
    // ignore the initial empty-state emission that snapshotFlow fires on
    // composition.
    LaunchedEffect(Unit) {
        snapshotFlow { form.itemName }.drop(1).collect { name ->
            if (viewModel.duplicateError != null) viewModel.clearDuplicateError()
            viewModel.checkForDuplicateItemName(name)
        }
    }

    // Match existing default-unit promotion from "" → "g" on first appear.
    LaunchedEffect(Unit) {
        if (form.unit.isEmpty()) form.unit = "g"
    }

    OnboardingScaffold(
        progress = viewModel.progressForStep,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(
                onClick = {
                    if (form.attemptSubmission()) viewModel.saveItemAndContinueToTrip()
                },
                enabled = viewModel.isFormValidForCompletion,
                modifier = Modifier.onboardingCelebrationPulse(viewModel.itemCelebrationTrigger),
            ) {
                Text("Save item", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            OnboardingSection(delayMs = 40) {
                Text(
                    text = "Your first item.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            OnboardingSection(delayMs = 140) {
                Text(
                    text = "Anything recent is fine — coffee, apples, bread. We'll remember what you paid.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                )
            }

            if (form.storeName.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                OnboardingSection(delayMs = 220) {
                    StoreContextPill(storeName = form.storeName)
                }
            }

            Spacer(Modifier.height(20.dp))

            OnboardingSection(delayMs = 300) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ItemNameField(
                        value = form.itemName,
                        onChange = { form.itemName = it },
                        duplicateError = viewModel.duplicateError,
                    )

                    CategoryPicker(
                        selected = form.selectedCategoryName,
                        onSelect = { form.selectedCategoryName = it.title },
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PriceField(
                                value = form.itemPrice,
                                onChange = { form.itemPrice = it },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            UnitPicker(
                                selected = form.unit,
                                onSelect = { canonical ->
                                    form.unit = canonical
                                    form.resetPackageSize(soldAsUnit = canonical)
                                },
                            )
                        }
                    }

                    if (!UnitSemantics.isContinuous(form.unit)) {
                        PackageSizeFields(
                            soldAsUnit = form.unit,
                            value = form.packageSizeValue,
                            unit = form.packageSizeUnit,
                            onValueChange = { form.packageSizeValue = it },
                            onUnitChange = { form.packageSizeUnit = it },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StoreContextPill(storeName: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = "At $storeName",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
