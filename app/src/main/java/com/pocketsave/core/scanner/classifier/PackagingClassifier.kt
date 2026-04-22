package com.pocketsave.core.scanner.classifier

import android.graphics.Bitmap
import com.pocketsave.domain.semantics.PackagingSignal

/**
 * Port of the iOS `PackagingModelClassifier` contract
 * (`PocketSave/Utils/PackagingModelClassifier.swift`) as an interface. The iOS
 * implementation backs onto a CoreML-compiled Vision model bundled with the
 * app; the Android runtime doesn't have the compiled model yet, so Phase 9
 * ships the no-op [DisabledPackagingClassifier] as the default injection.
 *
 * When a TensorFlow-Lite or ML Kit custom-model port lands, swap the
 * AppContainer wiring to a real implementation — the parser pipeline already
 * consumes [PackagingSignal]s so the rest of the stack needs no changes.
 */
interface PackagingClassifier {
    val isModelAvailable: Boolean

    suspend fun classify(bitmap: Bitmap): List<PackagingSignal>
}

/**
 * Default implementation — always returns no signals. Keeps the parser happy
 * when the packaging classifier model hasn't been ported yet (the parser
 * already accepts an empty signal list and falls back to OCR heuristics).
 */
object DisabledPackagingClassifier : PackagingClassifier {
    override val isModelAvailable: Boolean = false
    override suspend fun classify(bitmap: Bitmap): List<PackagingSignal> = emptyList()
}
