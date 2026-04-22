import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

/**
 * Per-developer billing secrets live in `android/local.properties` (which is
 * git-ignored). The RevenueCat Android public SDK key is technically embeddable
 * in the APK — it is designed to be shipped to clients — but routing it via
 * Gradle keeps the committed source free of dashboard-specific values and
 * makes it trivial to swap dev vs release keys per environment.
 *
 * Key read: `revenuecat.apiKey.android=goog_...`.
 * If the property is missing, a placeholder is used; `BillingConfig` detects
 * it and leaves the subscription manager inert so the app still runs.
 */
val billingProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val revenueCatAndroidKey: String = billingProperties
    .getProperty("revenuecat.apiKey.android", "goog_YOUR_REVENUECAT_ANDROID_KEY")

// Release signing also routes through local.properties so the keystore and
// its passwords never get committed. All four keys must be present for a
// real signed AAB; if any are missing the release build falls back to
// unsigned (which Play Console rejects with the generic "no bundles added"
// error — exactly the trap we hit on the first upload attempts).
val releaseStoreFilePath: String? = billingProperties.getProperty("signing.storeFile")
val releaseStorePassword: String? = billingProperties.getProperty("signing.storePassword")
val releaseKeyAlias: String? = billingProperties.getProperty("signing.keyAlias")
val releaseKeyPassword: String? = billingProperties.getProperty("signing.keyPassword")
val hasReleaseSigning: Boolean = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.pocketsave"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pocketsave"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.1.2"
        vectorDrawables { useSupportLibrary = true }

        // Surfaced as `BuildConfig.REVENUECAT_ANDROID_API_KEY`; read by
        // `com.pocketsave.billing.BillingConfig`. Keeping it here means there
        // is exactly one wiring path between local.properties → runtime.
        buildConfigField(
            type = "String",
            name = "REVENUECAT_ANDROID_API_KEY",
            value = "\"$revenueCatAndroidKey\"",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Required so `buildConfigField` above emits a real BuildConfig class.
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    // ProcessLifecycleOwner — drives the foreground entitlement refresh in
    // PocketSaveApplication so subscription state stays fresh after long
    // backgrounds without any screen having to opt in.
    implementation("androidx.lifecycle:lifecycle-process:2.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // CameraX + ML Kit text recognition for the scanner phase.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    // Subject Segmentation — Android's counterpart to iOS Vision's
    // `VNGenerateForegroundInstanceMaskRequest`. Returns a pre-composited
    // foreground bitmap with alpha, which we turn into a Buldak-style
    // sticker via edge feathering + white outline. Still in beta; first
    // call downloads the model (<10 MB) asynchronously.
    //
    // Ships only as Play Services-unbundled (`com.google.android.gms:
    // play-services-mlkit-*`), NOT the standalone `com.google.mlkit:*`
    // group that Text Recognition uses. The public API namespace
    // (`com.google.mlkit.vision.segmentation.subject.*`) is shared across
    // both distribution paths, so the Kotlin imports don't care which
    // artifact supplies the classes.
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    implementation("com.google.guava:guava:33.2.1-android")

    // Glance home-screen widget.
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito:mockito-core:5.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // RevenueCat — Google Play subscriptions wrapper (Android-only).
    // Bundles Google Play Billing; no separate billing-client dep needed.
    // If Gradle can't resolve this, check latest at
    // https://revenuecat.github.io/purchases-android/ and bump this line.
    implementation("com.revenuecat.purchases:purchases:8.10.8")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
