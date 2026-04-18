package com.pocketsave.core.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Port of `OnboardingLastStoreView`. Captures the user's last grocery store;
 * the "Skip" button clears the store name and jumps straight to the First Item
 * step, mirroring `viewModel.resetForSkip()` + `navigateToFirstItemDataScreen`.
 */
@Composable
fun OnboardingLastStoreScreen(viewModel: OnboardingViewModel) {
    val form = viewModel.formViewModel

    Column(modifier = Modifier.fillMaxSize()) {
        // Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = {
                viewModel.resetForSkip()
                viewModel.navigateToFirstItemDataScreen()
            }) {
                Text("Skip")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Where was your last grocery trip?",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = form.storeName,
            onValueChange = { new ->
                form.storeName = new
                if (form.isValidStoreName) viewModel.showError = false
            },
            placeholder = { Text("e.g. Public Market...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
        )

        if (viewModel.showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Store name needs at least 1 valid character",
                color = Color(0xFFFF6F71),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(1.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Button(
                onClick = {
                    if (!form.isValidStoreName) {
                        viewModel.triggerStoreNameError()
                    } else {
                        viewModel.navigateToFirstItemDataScreen()
                    }
                },
                enabled = form.isValidStoreName,
                modifier = Modifier.padding(16.dp),
            ) {
                Text("Next")
            }
        }
    }
}
