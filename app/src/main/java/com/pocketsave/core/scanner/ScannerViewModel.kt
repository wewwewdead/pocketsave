package com.pocketsave.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketsave.core.scanner.classifier.PackagingClassifier
import com.pocketsave.core.scanner.model.ProductScanResult
import com.pocketsave.core.scanner.parser.ProductScanParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Port of the iOS `ProductScannerSheet.ProductScannerViewModel` role — holds
 * the scanner pipeline state, coordinates OCR + packaging classification, and
 * exposes the finalised [ProductScanResult] to the Compose layer.
 */
class ScannerViewModel(
    private val context: Context,
    private val textRecognition: TextRecognitionService,
    private val packagingClassifier: PackagingClassifier,
) : ViewModel() {

    data class UiState(
        val isRecognizing: Boolean = false,
        val errorMessage: String? = null,
        val result: ProductScanResult? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * Runs OCR (+ optional packaging classification) on an in-memory bitmap —
     * used by the CameraX capture path.
     */
    fun analyzeCapture(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = UiState(isRecognizing = true)
            runCatching {
                val blocks = textRecognition.recognize(bitmap)
                val packagingSignals = if (packagingClassifier.isModelAvailable) {
                    withContext(Dispatchers.Default) { packagingClassifier.classify(bitmap) }
                } else emptyList()
                ProductScanParser.parse(blocks, packagingSignals)
            }.onSuccess {
                _state.value = UiState(result = it)
            }.onFailure {
                _state.value = UiState(errorMessage = it.message ?: "Scan failed")
            }
        }
    }

    /** Gallery-picker path: decode + OCR + parse in one go. */
    fun analyzeGallery(uri: Uri) {
        viewModelScope.launch {
            _state.value = UiState(isRecognizing = true)
            runCatching {
                val blocks = textRecognition.recognize(context, uri)
                ProductScanParser.parse(blocks, emptyList())
            }.onSuccess {
                _state.value = UiState(result = it)
            }.onFailure {
                _state.value = UiState(errorMessage = it.message ?: "Scan failed")
            }
        }
    }

    fun clearResult() {
        _state.value = UiState()
    }

    class Factory(
        private val context: Context,
        private val textRecognition: TextRecognitionService,
        private val packagingClassifier: PackagingClassifier,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ScannerViewModel::class.java))
            return ScannerViewModel(context.applicationContext, textRecognition, packagingClassifier) as T
        }
    }
}
