# PocketSave — Android

Android port of the iOS **PocketSave** grocery planner app. Built phase-by-phase
from the iOS source of truth under `../PocketSave/` (and `../PocketSaveWidgets/`).
Feature parity is preserved wherever the iOS app ships the behaviour without
monetisation gating; paywall / free-tier / RevenueCat logic is intentionally
**not** ported.

## Tech stack

| Layer | Choice | Notes |
| --- | --- | --- |
| Language | Kotlin 1.9.24 | |
| UI | Jetpack Compose (Material3) | BOM `2024.06.00` |
| Navigation | `androidx.navigation.compose` | |
| Persistence | Room `2.6.1` | matches iOS `SwiftData` domain 1:1 |
| Preferences | DataStore Preferences | onboarding flag, currency, custom units, per-cart background |
| Camera / OCR | CameraX `1.3.4` + ML Kit Text Recognition | label-scan flow |
| Widget | Jetpack Glance `1.0.0` | purchased-items snapshot grid |
| Images | Coil `2.6.0` | in-app display |
| JSON | kotlinx.serialization | widget snapshot |

## Module layout (single `app/` module)

```
com.pocketsave
├── PocketSaveApplication           # Process-scoped owner of AppContainer
├── MainActivity                    # Compose host
├── app/
│   ├── AppContainer                # iOS `ContentViewServices` equivalent
│   └── PocketSaveNavHost           # Navigation graph
├── common/
│   ├── ui/PocketSaveTheme          # Material3 theme seed
│   └── util/
│       ├── ActiveItemSelectionKey  # Shared key format (iOS parity)
│       ├── ColorOption             # 16-swatch cart background palette
│       ├── ImageStorage            # Off-row JPEG store for item images
│       └── ShareHelper             # Trip share / gallery save
├── core/
│   ├── cart/                       # Cart detail, manage, create, trip share
│   ├── home/                       # Home carts list
│   ├── itemform/                   # Shared item form VM
│   ├── onboarding/                 # Welcome → Last store → First item → Done
│   ├── scanner/                    # CameraX + ML Kit + parser + classifier
│   ├── service/VaultService        # The app's "data brain"
│   ├── tripshare/                  # Receipt bitmap renderer + screen
│   ├── vault/                      # Vault CRUD + category manager
│   └── widget/                     # Purchased-items Glance widget
├── data/
│   ├── local/{entity,dao,db}       # Room entities + DAOs + database
│   └── prefs/                      # AppPreferences + CartBackgroundStore
└── domain/
    ├── model/                      # CartStatus, FulfillmentAnimationState, GroceryCategory
    └── semantics/                  # UnitSemantics, UnitMenuCatalog, PackagingSemantics
```

## Architecture

- **VaultService** is the single source of truth for all domain mutations — the
  Android counterpart of iOS `PocketSave/Service/VaultService.swift`. Screens
  consume a `StateFlow<Snapshot>` that includes the user, vault, categories,
  stores, items, price options, carts, and cart items. Every mutation calls
  `publishSnapshotRefresh()` to re-emit; expensive mutations (item totals,
  widget snapshot) are debounced at the edges.
- **Compose screens** own their presentation state; ViewModels (one per
  feature) are plain classes in `core/.../{feature}ViewModel.kt` with
  `ViewModel.Factory` nested classes that capture the services they need.
- **No DI framework.** `AppContainer` constructs everything at process start
  and hands it to the Compose tree via explicit parameters. Matches iOS
  `ContentViewServices` in spirit.
- **Offline-first.** All persistence is local (Room + DataStore + app-private
  files). No server, no auth.

## iOS parity matrix

| Feature | iOS source | Android status |
| --- | --- | --- |
| Onboarding flow | `Core/Onboarding/**` | ✅ welcome → last store → first item → done |
| Vault CRUD | `Core/Vault/**` | ✅ categories, items, search, image picker |
| Category manager | `Views/CategoriesManager/**` | ✅ default + custom; reordering deferred |
| Item form (add/edit) | `Core/ItemForm/**` + `Core/EditItem/**` | ✅ name/category/store/price/unit/package size/image/scan |
| Scanner | `Components/ProductScannerSheet.swift` | ✅ CameraX + ML Kit + pure-Kotlin parser |
| Packaging classifier | `Utils/PackagingModelClassifier.swift` | ⚠ interface + no-op default; CoreML model port deferred |
| Planning cart | `Core/Detail Cart/CartDetailScreen.swift` | ✅ header, store sections, rename, budget, row editor, add items |
| Shopping cart | `Core/Detail Cart/Shopping Cart/**` | ✅ fulfill toggle, skip/un-skip, actual editor, quick add |
| Finish trip | `Shopping Cart/Finish Trip/FinishTripSheet.swift` | ✅ insights sheet + vault price write-back |
| Reopen / Return to planning | `VaultService+Carts.swift` | ✅ |
| Cart background customisation | `Core/Detail Cart/Views/ModeToggleView.swift` | ✅ colour + image (per-cart DataStore) |
| Trip share | `Core/TripShare/**` | ✅ receipt bitmap + share intent + save to gallery |
| Purchased-items widget | `PocketSaveWidgets/PurchasedItemsGridWidget.swift` | ✅ Glance widget + JSON snapshot + refresh on mutations |
| Home carts list | `Core/Home/**` | ✅ active + completed carts with background painted |
| Currency selection | `Service/CurrencyManager.swift` | ✅ `CurrencyFormatter` + `CurrencyPickerSheet`; Home overflow → Currency |
| History detail | `Core/History/**` | ✅ `HistoryScreen` with rollup (total spent / budget / over-under) and trip cards |
| CSV export | `VaultService+CSVExport.swift` | ❌ explicitly deferred per iOS "temporarily disabled" comment |
| Trash restore | `VaultService+Domain.swift::restoreDeletedItem` | ✅ `TrashScreen` (Items / Carts tabs) + restore / permanent-delete for both |
| Paywall / RevenueCat | `Service/SubscriptionManager.swift` + paywall views | ❌ intentionally not ported |
| UserJot / rating / upgrade prompts | various managers | ❌ intentionally not ported |
| Deep links (`pocketsave://…`) | `App/GrockApp.swift::handleDeepLink` | ✅ manifest intent filters + `DeepLink` parser + `PendingDeepLink` routing (quickadd + finishtrip) |
| Instagram Stories share | `TripShareViewModel.shareToInstagramStory` | ❌ deferred; system share chooser covers the same goal |

## Regression tests

JVM unit tests live under `app/src/test/java/com/pocketsave/` and cover:

- `domain/semantics/UnitSemanticsTest` — canonical units + continuous/discrete split + base-quantity conversions
- `domain/semantics/PackagingSemanticsTest` — `canonicalPackagingUnit`, `clampedConfidence`, `deduplicatedSignals`, `mergedSignals`, `strongestSignal`
- `common/util/ActiveItemSelectionKeyTest` — make/parse/itemId round-trip
- `common/util/ColorOptionTest` — palette invariants, hex parsing
- `core/scanner/model/ProductScanResultTest` — `normalizeScannedItemName` + `replacingName`
- `core/scanner/parser/ProductScanParserTest` — price extraction ($, ₱), unit-price continuous unit, package size, fluid-ounces → mL, multi-pack multiplication, bulk quantity, name inference
- `core/service/VaultServiceTotalsTest` — planned-vs-actual totals across Planning/Shopping/Completed statuses, shopping-only items, skipped rows, `wasEditedDuringShopping`

Run: `./gradlew :app:testDebugUnitTest`

## Build & run

Prerequisites:
- JDK 17
- Android Studio Iguana (or `./gradlew`) with Android Gradle Plugin 8.5.x

```bash
# Open the project in Android Studio and let it generate the Gradle wrapper
# (the repository doesn't check in gradlew / gradlew.bat / gradle-wrapper.jar).
./gradlew :app:assembleDebug        # produces app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # runs JVM regression tests
```

Install on a connected device / emulator via `./gradlew :app:installDebug`.

## Known gaps / follow-ups

Landed since the previous pass: Trash UI ✅, History screen ✅, deep-link
handler ✅, per-user currency override ✅, CategoriesManager UX pass ✅. The
remaining items need external artifacts this project can't ship solo:

1. **Packaging classifier TFLite model** — iOS bundles a CoreML model
   (`GrockPackagingClassifier.mlmodelc`). Porting it is a data-science task:
   retrain with TensorFlow / ML Kit Custom Models, export `.tflite`, then
   swap `DisabledPackagingClassifier` in `AppContainer`. The parser pipeline
   already consumes `PackagingSignal`s, so zero Android code changes once the
   model ships.
2. **Cross-platform snapshot interop** — the widget snapshot JSON is already
   wire-compatible with iOS. Moving the schema into a shared KMP module so
   both apps can depend on `CartKit` is infrastructure work that touches
   iOS + Android build setups together; best handled by both teams in one pass.
3. **Instagram Stories direct share** — iOS-only pasteboard contract; no
   stable Android equivalent. The system share chooser already lists
   Instagram when installed.
4. **CSV export** — iOS ships this temporarily disabled. Track upstream
   before porting.
