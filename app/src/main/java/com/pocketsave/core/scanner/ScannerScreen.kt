package com.pocketsave.core.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketsave.core.scanner.model.ProductScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Android port of `PocketSave/Components/ProductScannerSheet.swift`.
 *
 * iOS uses `VisionKit.DataScannerViewController`; Android uses `CameraX` +
 * `ML Kit Text Recognition` feeding the shared [com.pocketsave.core.scanner.parser.ProductScanParser].
 * The capture flow mirrors the iOS "analyze selected photo" code path —
 * snap → decode → OCR → parse → return [ProductScanResult] to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    textRecognitionService: TextRecognitionService,
    packagingClassifier: com.pocketsave.core.scanner.classifier.PackagingClassifier,
    onResult: (ProductScanResult) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModel.Factory(context, textRecognitionService, packagingClassifier),
    )
    val state by viewModel.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(state.result) {
        val result = state.result
        if (result != null) {
            onResult(result)
            viewModel.clearResult()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.analyzeGallery(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a product") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel scan")
                    }
                },
                actions = {
                    TextButton(onClick = onCancel) { Text("Skip") }
                },
            )
        },
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            if (hasCameraPermission) {
                CameraPreviewWithCapture(
                    isRecognizing = state.isRecognizing,
                    onCapture = { bitmap, rotation -> viewModel.analyzeCapture(bitmap, rotation) },
                    onPickFromGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            } else {
                PermissionFallback(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onPickFromGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewWithCapture(
    isRecognizing: Boolean,
    onCapture: (Bitmap, Int) -> Unit,
    onPickFromGallery: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose { executor.shutdown() }
    }

    LaunchedEffect(Unit) {
        // ProcessCameraProvider.getInstance returns a ListenableFuture; pull it
        // off the main thread so the initial `get()` doesn't stall first paint.
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "CameraX bind failed", t)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        if (isRecognizing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Reading label…",
                        color = Color.White,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPickFromGallery,
                enabled = !isRecognizing,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x99FFFFFF)),
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick from gallery")
            }

            IconButton(
                onClick = {
                    if (isRecognizing) return@IconButton
                    scope.launch {
                        val captured = captureBitmap(context, imageCapture, executor)
                        if (captured != null) onCapture(captured.bitmap, captured.rotationDegrees)
                    }
                },
                enabled = !isRecognizing,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Symmetric spacer so the capture button stays centred.
            Box(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun PermissionFallback(onRequestPermission: () -> Unit, onPickFromGallery: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Allow camera access to scan product labels.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequestPermission) { Text("Grant camera access") }
        TextButton(onClick = onPickFromGallery) { Text("Pick from gallery instead") }
    }
}

private const val TAG = "ScannerScreen"
private const val CAPTURE_TARGET_MAX_DIMENSION = 1600

/**
 * Bundles the decoded capture bitmap with the rotation CameraX reports. We
 * defer applying the rotation so the OCR path can hand it directly to
 * [com.google.mlkit.vision.common.InputImage.fromBitmap], avoiding a second
 * full-size bitmap allocation that a `Bitmap.createBitmap(..., matrix, true)`
 * rotation would otherwise cost.
 */
private data class CapturedImage(val bitmap: Bitmap, val rotationDegrees: Int)

private suspend fun captureBitmap(
    @Suppress("UNUSED_PARAMETER") context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
): CapturedImage? = suspendCancellableCoroutine { cont ->
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val captured = image.use { it.toDownsampledCapture() }
                cont.resume(captured)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
                cont.resume(null)
            }
        },
    )
}

/**
 * Decode [ImageProxy] → JPEG buffer → downsampled ARGB_8888 bitmap. We pair the
 * result with [ImageProxy.getImageInfo]'s rotation so the OCR layer can pass
 * it straight into ML Kit without allocating a rotated copy.
 *
 * A two-pass decode is used: a header-only bounds read to pick
 * [android.graphics.BitmapFactory.Options.inSampleSize], then a single
 * downsampled decode at ~[CAPTURE_TARGET_MAX_DIMENSION] px on the long edge.
 * Typical 12 MP captures drop from a ~48 MB full-resolution bitmap to ~12 MB.
 */
private fun ImageProxy.toDownsampledCapture(): CapturedImage? {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = computeInSampleSize(width, height, CAPTURE_TARGET_MAX_DIMENSION)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
    return CapturedImage(bitmap = bitmap, rotationDegrees = imageInfo.rotationDegrees)
}

private fun computeInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    var w = width
    var h = height
    while (w / 2 >= maxDimension && h / 2 >= maxDimension) {
        w /= 2
        h /= 2
        sampleSize *= 2
    }
    return sampleSize
}

