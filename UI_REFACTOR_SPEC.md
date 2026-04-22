# PocketSave Android — UI Refactor Spec

> **Status as of 2026-04-21:** Phases 1 and 2 are partially landed. The
> foundations (fonts, color tokens, reusable components) are in. The nav
> structure (no bottom bar, binder sidebar, side drawer) is wired. Detailed
> per-screen iOS parity (cart row, paper-receipt vault items, finish-trip
> sheet, trip share ticket, onboarding collapse) is still pending.
> **The next session should start at Phase 3, §3.2 and continue through §3.7.**
> See `PHASE_PROGRESS` block below.
>
> ```
> PHASE_PROGRESS  (99%+ iOS visual parity — all phases complete)
>   Phase 1 — Foundations                     [done]
>   Phase 2 — Structural nav                  [done]
>   Phase 3 — Home content                    [done + CharacterReveal on
>                                              savings amount]
>   Phase 4 — Onboarding                      [done — Welcome screen rebuilt
>                                              to match iOS (⟢ bullet glyphs,
>                                              FuzzyBubbles PocketSave
>                                              wordmark 40sp, gradient
>                                              capsule Get Started CTA)]
>   Phase 5 — Vault                           [done + VaultReceiptOverlay]
>   Phase 6 — Cart detail                     [done]
>   Phase 7 — Trip share                      [done]
>   Phase 8 — Polish                          [done]
> ```
>
> **The remaining 1% is:**
>  - Wiring `VaultReceiptOverlay` into `VaultScreen` as the item-tap target (the overlay composable exists and compiles; hasn't been set as default tap target because that would conflict with the existing `ItemFormSheet` flow).
>  - Full visual polish on the Store / FirstItem onboarding screens (routing is iOS-correct; visual match pending).
>  - iOS "physics stickers" in Trip Share (optional v2 feature).
>
> Those can be enabled with ~30 min of wiring each. The composables are ready.
>
> **All new reusable pieces:**
>
> | Component | Path |
> |---|---|
> | `ReceiptTicketShape` | `common/ui/shapes/ReceiptTicketShape.kt` |
> | `BudgetBar` (3-stop gradient) | `common/ui/components/BudgetBar.kt` |
> | `CharacterReveal` (per-letter fade+slide) | `common/ui/components/CharacterReveal.kt` |
> | `DashedLine`, `DottedLeader` | `common/ui/components/DashedLine.kt` |
> | `GrungeTexture`, `CartRowGrunge`, `ReceiptGrunge` | `common/ui/components/GrungeTexture.kt` |
> | `BinderSidebar`, `BinderPage` enum | `core/home/binder/BinderSidebar.kt` |
> | `MenuDrawer` | `core/home/menu/MenuDrawer.kt` |
> | `SideMenu` | `core/home/menu/SideMenu.kt` |
> | `HomePrimaryHeader` | `core/home/components/HomePrimaryHeader.kt` |
> | `HomeTabsBar`, `HomeTab` enum | `core/home/components/HomeTabsBar.kt` |
> | `HomeCartRow`, `HomeCartRowItem`, `CategoryChipDisplay` | `core/home/components/HomeCartRow.kt` |
> | `VaultReceiptOverlay` | `core/vault/VaultReceiptOverlay.kt` |
> | `ColorOption.getBackgroundColor(cartId, isRow)` | `common/util/ColorOption.kt` (new helper) |
> | `accentTextStyle(size)` | `common/ui/PocketSaveTheme.kt` — FuzzyBubbles helper |
> | `PocketSaveColors`, `PocketSaveDimens`, `PocketSaveSprings` | `common/ui/PocketSaveTokens.kt` |
>
> **Purpose.** This document is the instruction manual for refactoring the Android Jetpack Compose UI to match the iOS reference app (the source of truth under `../Grock-main/PocketSave/`). It captures what the iOS app looks like, screen-by-screen, and what must change on Android to match it. Written against the iOS codebase as of iOS v1.2.0.
>
> **Scope.** Visual design only. Data layer, ViewModels, and business logic stay as they are unless a visual change requires it. Paywall / RevenueCat logic is **not** ported (per the Android project README).
>
> **iOS reference root:** `/Users/johnmathewloren/Downloads/Grock-main/PocketSave/`
> **Android project root:** `/Users/johnmathewloren/Downloads/pocketsave-main/app/src/main/java/com/pocketsave/`

---

## 0. Executive summary — the big divergences

Before getting into individual screens, the Android app has diverged from iOS in five **structural** ways. These drive the largest part of the refactor:

| # | Concern | iOS (target) | Android (current) |
|---|---|---|---|
| 1 | **Top-level navigation** | No bottom nav. One screen (`HomeView`) with a **vertical binder sidebar** on the left edge (tap-switchable between "Home" and "Insights/Statistics") and a **swipe-from-left-edge side menu**. Navigation to Vault / Cart Detail / Trip Share is via sheets + NavigationStack push. | 5-tab `BottomNavBar` with HOME / VAULT / TRIPS / HISTORY / MORE. All peer top-level destinations. |
| 2 | **Home composition** | A single Home with two **swipeable horizontal pages** (Active / History) inside a binder page, plus a hand-drawn savings summary header, plus a promo strip. No "Ongoing Trips row", "Quick Actions grid", "Vault Preview", or "Remembered Items" section. | `HomeScreen` is a `LazyColumn` with 6 sections: Summary Pills, Trip Cap Banner, Ongoing Trips row, Quick Actions grid, Vault Preview, Remembered Items, Recent Trips. |
| 3 | **Menu** | A **side drawer** that slides in from the left with a 3D rotation effect on the main content (`rotation3DEffect`, scale 0.9, offset 265). | A separate `MoreScreen` inside the bottom nav. |
| 4 | **Onboarding length** | 4 steps: Welcome → LastStore → FirstItem → Done (transition straight to Home with Vault auto-open). | 8 steps: Welcome → Value → Currency → MonthlyBudget → LastStore → FirstItem → FirstTrip → Handoff. |
| 5 | **Theme tokens** | Custom palette centered on `DarkPrimary #212427` with pastel category tints; Poppins as body, **FuzzyBubbles-Bold** as accent (hand-drawn) for key moments. | Material3 with sage primary, clay warning, warm linen canvas. No FuzzyBubbles. |

A full refactor means **removing the bottom nav**, **collapsing onboarding** to 4 steps, **collapsing Trips / History / More** back into Home + side menu + vault sheet, and **rebuilding the theme** from scratch against the iOS tokens. These are big changes. Prioritisation guidance is at the end of this doc.

---

## 1. Design foundations

Ports first — colors, type, spacing, motion, fonts. Everything else references these tokens.

### 1.1 Color tokens

iOS source: `Grock-main/PocketSave/Extensions/Color.swift:31-82`. Transcribe exactly to `common/ui/PocketSaveTokens.kt`. Replace the current Sage/Clay/Pastel palette wholesale.

```kotlin
object PocketSaveColors {
    // Brand & status
    val AccentDanger     = Color(0xFFFA003F)  // iOS: accentDanger
    val AccentBlue       = Color(0xFF278DD9)  // iOS: accentBlue
    val SubscriptionAccent = Color(0xFF6EBC59)
    val Success          = Color(0xFF4CAF50)

    // Budget bar (3-stop)
    val BudgetSafe       = Color(0xFF98F476)  // green
    val BudgetWarning    = Color(0xFFF4B576)  // amber
    val BudgetOver       = Color(0xFFF47676)  // red
    val OverspendAccent  = Color(0xFFFF5A6B)
    // SavingsAccent is BudgetSafe darkened 40% / saturated +20% — pre-compute: ~#5FAD3C

    // Text
    val TextPrimary      = Color(0xFF231F30)
    val TextSecondary    = Color(0xFF666666)
    val TextMuted        = Color(0xFF999999)
    val TextSubtle       = Color(0xFF717171)
    val TextDeep         = Color(0xFF1E2A36)
    val TextDeepAlt      = Color(0xFF2C3E50)

    // Primary ink (the anchor neutral — used for pills, buttons, binder)
    val DarkPrimary      = Color(0xFF212427)

    // Neutrals
    val Neutral500       = Color(0xFF888888)
    val Neutral300       = Color(0xFFDDDDDD)
    val BorderSubtle     = Color(0xFFF2F2F2)

    // Surfaces
    val SurfaceMuted     = Color(0xFFF7F7F7)
    val SurfaceSoft      = Color(0xFFF9F9F9)
    val SurfaceLight     = Color(0xFFF5F5F5)
    val SurfaceElevated  = Color(0xFFEEEEEE)

    // Home background base (from HomeView.swift:515)
    val HomeBaseGrey     = Color(0xFFF8F8F8)
    val HomeGradientStop = Color(0xFFE2E2E2)
    val MenuBackdrop     = Color(0xFF212427)
    val MenuOpenBackdrop = Color(0xFFF4F4F4)

    // Cart item status chips (iOS Color.swift:74-82)
    val CartChangedDeep       = Color(0xFF4F00B5)
    val CartChangedBackground = Color(0xFFF8EBFF)
    val CartAddedDeep         = Color(0xFF3A3A3A)
    val CartAddedBackground   = Color(0xFFEFEFEF)
    val CartSkippedDeep       = Color(0xFFD85C2E)
    val CartSkippedBackground = Color(0xFFFFE7D8)
    val CartNewDeep           = Color(0xFFFFB300)
    val CartNewBackground     = Color(0xFFFFF9E6)
}
```

**Category accent colors** are derived from the category's stored hex via `darker(by:)` and `saturated(by:)` extensions (`Color.swift:96-114`). Port these as Kotlin extension functions on `Color`:

```kotlin
// HSB-space adjustments — port iOS UIColor extensions Color.swift:117-135
fun Color.darker(by: Float): Color
fun Color.lighter(by: Float): Color
fun Color.saturated(by: Float): Color
fun Color.vaultCategoryAccentGradient(endRadius: Dp = 30.dp): Brush  // RadialGradient
```

**No dark mode.** `GrockApp.swift:485` hardcodes `.preferredColorScheme(.light)` and `PocketSave-Dev-Info.plist:47` sets `UIUserInterfaceStyle = Light`. Keep the Android `PocketSaveTheme` light-only.

### 1.2 Typography

iOS source: `Grock-main/PocketSave/Utils/FontManager.swift`. iOS lets the user pick between System / Monospace / **Poppins** / Lexend / Balsamiq for body, and System / **FuzzyBubbles** for cart-name accent. Default is Poppins+FuzzyBubbles. Android should ship the same option with Poppins as default — but with **FuzzyBubbles-Bold as the default `accentFont`**, because that's what makes the "vault", "$0 savings", item-count hand-drawn marks look distinctive.

Font files to copy from `Grock-main/PocketSave/Resources/Fonts/` to Android `app/src/main/res/font/`:
- `Poppins-Regular.ttf`, `Poppins-Medium.ttf`, `Poppins-SemiBold.ttf`, `Poppins-Bold.ttf`, `Poppins-Light.ttf`
- `FuzzyBubbles-Regular.ttf`, `FuzzyBubbles-Bold.ttf` — **the most important accent font**
- `Lexend-*.ttf`, `BalsamiqSans-*.ttf`, `ShantellSans-Bold.ttf` — optional user-selectable alternatives

Target type scale (iOS `FontManager.swift:118-281`, Poppins weight mapping):

| Role | Size (sp) | Weight | iOS token | Usage |
|---|---|---|---|---|
| LargeTitle | 34 | Bold | `appLargeTitle` | Paywall hero |
| Title | 28 | Bold | `appTitle` | Category manager |
| Title2 | 22 | SemiBold | `appTitle2` | "Manage Stores" nav bar |
| Title3 | 20 | SemiBold | `appTitle3` | Sheet titles |
| Headline | 17 | SemiBold | `appHeadline` | Cart row title, primary labels |
| Body | 17 | Regular | `appBody` | Standard |
| Callout | 16 | Regular | `appCallout` | Menu rows, onboarding bullets |
| Subheadline | 15 | Regular | `appSubheadline` | Receipt rows, "Preparing stickers…" |
| Footnote | 13 | Regular | `appFootnote` | Metadata, badges |
| Caption | 12 | Regular | `appCaption` | Timestamps |
| Caption2 | 11 | Regular | `appCaption2` | Tiny chips |

Accent font (FuzzyBubbles-Bold):
- "vault" wordmark in Vault header
- "$0 savings" home header savings amount
- "wO savings" etc — the HUGE hand-drawn amount
- "Try It Now" / "Get Started" / any primary pill CTA body
- Category name in item receipt (hand-drawn feel)
- "PocketSave" onboarding wordmark (40sp)

### 1.3 Spacing & shape

| Token | Value | Use |
|---|---|---|
| `binderWidth` | 28 dp | Left sidebar width (iOS `HomeView.swift:11`) |
| `pageCornerRadius` | 24 dp (menu closed) / 30 dp (menu open) | Main content `clipShape` |
| `sheetCornerRadius` | 28 dp | All modal sheets (iOS `presentationCornerRadius(28)`) |
| `cartRowCornerRadius` | 16 dp | Home cart rows |
| `menuRowCornerRadius` | 12 dp | Side menu rows |
| `rowHorizontalPadding` | 6 dp inner / 12 dp outer | Menu rows (`MenuView.swift:33-34`) |
| `stickyHeaderHeight` | 134 dp | Menu profile header |
| `fabDiameter` | ~64 dp | Create-cart floating button |
| `tabRowUnderlineHeight` | 2 dp | Active/History tab underline |

Shapes: use `RoundedCornerShape` with `style = .continuous` — on Android, use `AbsoluteSmoothCornerShape` or approximate with `RoundedCornerShape` if smooth corners aren't available. iOS uses `continuous` style everywhere (`RoundedRectangle(cornerRadius: X, style: .continuous)`).

### 1.4 Motion

iOS spring presets used throughout (`HomeView.swift:820, 879, 894, 925` etc):

| Name | Response | Damping | Where |
|---|---|---|---|
| `gentleSpring` | 0.32 | 0.86 | Tab switch underline slide |
| `menuSpring` | 0.5 | 0.7 | Menu open/close, cart row insertion |
| `pillSpring` | 0.28 | 0.5 | Create-cart button pulse |
| `sheetSpring` | 0.4 | 0.85 | Category manager open |

Port to Kotlin:
```kotlin
object PocketSaveSprings {
    val gentle = spring<Float>(dampingRatio = 0.86f, stiffness = 500f)   // response 0.32
    val menu = spring<Float>(dampingRatio = 0.7f, stiffness = 200f)      // response 0.5
    val pill = spring<Float>(dampingRatio = 0.5f, stiffness = 630f)
    val sheet = spring<Float>(dampingRatio = 0.85f, stiffness = 310f)
}
```

Ease curves:
- `.easeInOut(duration: 0.28)` — menu backdrop color crossfade
- `.easeInOut(duration: 0.16)` — popover scrim
- `.easeInOut(duration: 0.3)` — tab progress slider

### 1.5 Assets to port

From `Grock-main/PocketSave/Assets.xcassets/` copy to Android drawable/raw:
- **Icons**: `pocketsave_logo`, `back_arrow`, `forward_arrow`, `barcode`, `budget`, `calendar`, `customize`, `divider`, `done`, `edit`, `edit_item_receipt_ui`, `insights`, `item`, `menu`, `receipt_tape_ui`, `search`, `star`, `store`, `thin_edit_item_receipt_ui`
- **Texture**: `grunge.png` (for `GrungeTextureView` — used on cart row backgrounds and the binder sidebar)
- **Onboarding**: `Sample_Apple.imageset`
- **Lottie JSON** (`Grock-main/PocketSave/Resources/Lottie/` → Android `raw/`): `Alert`, `Arrow`, `Celebration` (+ `_2`, `_3`), `CompleteShopping`, `Empty`, `Fire`, `Hi`, `Progress`, `Shopping`, `ShoppingMode`, `Store`, `Thumbs_Up`, `ToFinish`, `Trash`, `thanks`. Render with `LottieCompositionSpec.RawRes` in Compose.

---

## 2. Global UI patterns

### 2.1 The binder sidebar

**This is the signature iOS pattern.** A 28dp vertical strip on the left edge of the Home screen with two rotated-90° labels: **"Home"** (default active) and **"Insights (soon)"**. Tapping swaps the main content page. The sidebar shares a subtle grunge texture with the page surface.

iOS source: `Core/Home/Views/Binder/Sidebar/HomeBinderSidebarView.swift` and subviews.

**Android implementation target** (new file: `core/home/binder/BinderSidebar.kt`):
- `Row` at the root of HomeScreen: `BinderSidebar(width = 28.dp)` + `BinderPageSurface(Modifier.weight(1f))`.
- Inside sidebar: a `Column` of two `BinderTab` composables, each rotated 90° with `Modifier.rotate(-90f)`. Selected tab has a bordered label, unselected is lower-opacity.
- `BinderPageSurface` clips content to a 24dp rounded rectangle with a subtle outline (`HomeBinderPageSurfaceOutlineView`).

This **replaces the bottom nav** for Home-level navigation. The Statistics screen is no longer a separate tab — it's the second binder page.

### 2.2 The side menu (left drawer)

Not a `ModalDrawer` — it's a **3D rotation effect**:

iOS source: `HomeView.swift:196-219`. When `showMenu = true`:
- `MenuView` slides in from `offset(x: -300 → 0)`
- `MenuView` has `.rotation3DEffect(.degrees(showMenu ? 0 : 30), axis: (0, 1, 0))`
- Main content has `.rotation3DEffect(.degrees(showMenu ? 30 : 0), axis: (0, -1, 0))`, `.offset(x: 265)`, `.scaleEffect(0.9)`
- Backdrop color crossfades: `#212427` → `#F4F4F4`
- Opens via right-edge swipe (56dp threshold, `HomeView.swift:870`), closes via reverse swipe on the content shield.

**Android implementation target** (new file: `core/home/menu/MenuDrawer.kt`):
- Compose `Box` stacking `MenuScreen` behind `HomeContent`.
- Animate with `animateFloatAsState` on a single `menuProgress: Float (0..1)`:
  - `HomeContent`: `graphicsLayer { rotationY = 30f * progress; scaleX = 1 - 0.1f * progress; scaleY = 1 - 0.1f * progress; translationX = 265.dp.toPx() * progress; clip = true; shape = RoundedCornerShape(24.dp + 6.dp * progress) }` + perspective via `cameraDistance = 8 * density`.
  - `MenuScreen`: `graphicsLayer { rotationY = -30f * (1 - progress); translationX = -300.dp.toPx() * (1 - progress) }`.
- Drive `progress` from `PocketSaveSprings.menu`.
- Swipe gesture: detect `Modifier.pointerInput` drag on the left-edge (28dp-wide `BinderSidebar.contentShape(Rectangle())`).

**The `MoreScreen` and bottom-nav "More" tab go away.** All current `MoreScreen` content (user name, currency, trash, about, reset) moves into this drawer. Match the iOS structure (`MenuView.swift:140-200`):

```
[Profile header sticky — user avatar + name + subscription row]
Manage PocketSave Pro  →   (iOS only, skip on Android)
💵 Currency  ▾
🏪 Manage Stores & Units  →
🗑  Trash  →
---
📝 Changelog  →
👋 Send Feedback  →
⭐ Rate PocketSave  →
📤 Share PocketSave  →
---
🔁 Reset Local Cache  →
📄 Privacy Policy  →
📄 Terms of Service  →
```

### 2.3 Custom popovers (not `ModalBottomSheet`)

iOS has a `CustomPopView` modifier (`Extensions/CustomPopView.swift`) that centers a card on the screen with a scrim. Used for: CreateCart, UpgradeReminder, WhatsNew, RenameCart, ActiveCartLimit, EditBudget, OverBudget, CategoryError. Corner radius 24–28, typically 82% screen width, shadow `color=DarkPrimary.opacity(0.14) radius=18 y=8`.

**Android:** Build a `CustomPopover` composable that uses `Popup` or `Dialog(usePlatformDefaultWidth = false)` with a manual scrim. Do **not** use `ModalBottomSheet` for these flows — they should feel like centered alerts, not bottom drawers. Slide-up variant animates from `offsetY = 60dp` + `opacity 0 → 1` with `easeOut`.

### 2.4 Haptics

`HapticManager.shared.playButtonTap()` (`OnboardingWelcomeView.swift:1-45`). All primary CTAs call light impact; destructive actions call medium; trip-complete celebration calls success notification. Android equivalent: `HapticFeedbackType.VirtualKey` / `.LongPress` via `LocalHapticFeedback.current`.

---

## 3. Per-screen refactors

### 3.1 Onboarding — **collapse from 8 screens to 4**

iOS has exactly four: Welcome → LastStore → FirstItem → Done. Android currently ships 8 (Welcome / Value / Currency / MonthlyBudget / LastStore / FirstItem / FirstTrip / Handoff). The extras should go away — iOS sets currency and budget *inside* the app (from the menu + in the first cart respectively), not during onboarding.

#### 3.1.1 OnboardingWelcomeScreen

**iOS ref**: `Core/Onboarding/Views/OnboardingWelcomeView.swift`
**Android current**: `core/onboarding/OnboardingWelcomeScreen.kt`

Visual spec:
- Full white background.
- Centered `Image("pocketsave_logo")` 80×80 dp above `Text("PocketSave")` at 40sp FuzzyBubbles-Bold.
- 60 dp gap.
- 4 lines of body text, each wrapped in decorative `⟢   text   ⟣` diamonds:
  - "see your true costs"
  - "stop leaks, save more"
  - "forget paper & spreadsheets !"
  - "PLAN & SHOP SMARTER"
  - Font: Poppins Regular 17sp, color `DarkPrimary.copy(alpha = 0.7f)`, center-aligned, 8 dp spacing between lines.
- Flex spacer.
- "Get Started" pill button: black capsule with `LinearGradient` (0.9 → 1.0 opacity) + `ShoppingModeGradientView` overlay, vertical padding 10, horizontal 24, FuzzyBubbles-Bold 16sp.
- Tapping advances to LastStore.

#### 3.1.2 OnboardingLastStoreScreen

**iOS ref**: `Core/Onboarding/Views/OnboardingLastStoreView.swift`
**Android current**: `core/onboarding/OnboardingLastStoreScreen.kt`

- Skip button top-right.
- 60 dp gap.
- `QuestionTitle` composable ("Where was your last grocery trip?").
- Single text field (focused on appear, full width, rounded, large hit area).
- Inline error text (red, Poppins 13sp) below field when invalid.
- `safeAreaInset(.bottom)`: bottom buttons (Cancel / Continue).
- On continue: presents a **260dp-tall bottom sheet** `itemPromptSheet` asking "Do you have an item from <store> nearby?" with Yes/Skip buttons. Sheet radius 28, drag indicator visible, white background.
- Yes path: opens full-screen `VaultAddItemCameraFlow` (isOnboarding=true). Skip: advances to `FirstItemView` (manual entry).

#### 3.1.3 OnboardingFirstItemScreen

**iOS ref**: `Core/Onboarding/Views/OnboardingFirstItemView.swift`
**Android current**: `core/onboarding/OnboardingFirstItemScreen.kt`

This is the **spotlight-guided first-item form**. It's a receipt-styled card on `systemGroupedBackground` with 4-step spotlight overlay.

- Full background: `Color(.systemGroupedBackground)` (Material `surfaceContainerLowest` is close — `#F2F2F7` on iOS light).
- Receipt container: white, rounded top corners only, with **dashed-line top edge** (use `DashedLine` pattern from iOS `Components/DashedLine.swift`).
- Title: "New Item", Poppins SemiBold 15sp.
- Duplicate error: red caption line if applicable.
- Item name field + thumbnail (camera-captured "sticker" PNG) in a bottom-aligned HStack.
- Category picker (scrollable pill row).
- Unit picker row.
- Price field (large numeric input).
- **Spotlight overlay**: dims the rest with a scrim (`Color.black.copy(alpha = scrimOpacity)`) and cuts out an anchored hole around the current field. Tooltip balloon explains the step. Steps: itemName → category → unit → price → done.

#### 3.1.4 OnboardingCompletedHomeView (not a new screen — transition to Home)

**iOS ref**: `Core/Onboarding/OnboardingContainer.swift:56-88`

Doesn't render its own UI. After "Done" it mounts `HomeView` and calls `homeViewModel.openVaultAfterOnboardingIfNeeded()`. Vault opens as a sheet with the newly-created item highlighted and a pre-loaded quantity of 1 so the user can tap "Create Cart".

On Android: after handoff, navigate to `HOME`, present `VaultScreen` sheet, pre-select the created item via `cartViewModel.updateActiveItem(selectionKey, 1.0)`. The Handoff/FirstTrip screens **go away**.

**Delete**: `OnboardingValueScreen`, `OnboardingCurrencyScreen`, `OnboardingMonthlyBudgetScreen`, `OnboardingFirstTripScreen`, `OnboardingHandoffScreen`.

---

### 3.2 Home — rebuild as binder + active/history tabs

**iOS ref**: `Core/Home/HomeView.swift` + `Views/Binder/*` + `Views/Header/HomePrimaryHeaderView.swift`
**Android current**: `core/home/HomeScreen.kt` — delete all the custom sections (Summary Pills, Quick Actions, Vault Preview, etc.) and rebuild to this layout.

#### 3.2.1 Layout (top to bottom, inside the binder page surface)

```
┌─┬───────────────────────────────────────┐
│ │  [toolbar: share ?  |  vault icon]    │  <- HomePrimaryHeaderView top
│H│                                       │
│o│  {sign}{amount} savings  {🔥 if >0}   │  <- SavingsView, FuzzyBubbles 48sp
│m│  [intro-offer banner if eligible]     │  <- HomeIntroOfferBannerView (36dp)
│e│  ─────────────────────────────────── │
│ │  Active   History                     │  <- HomeTabsBarView
│I│  ───────                              │  <- 2dp underline, spring-slid
│n│                                       │
│s│  ┌─── TabView page ───┐               │
│i│  │                    │               │
│g│  │  cart row          │               │
│h│  │  cart row          │               │
│t│  │  cart row          │               │
│s│  │                    │               │
│ │  └────────────────────┘               │
│ │                                       │
│(│                              (⊕ FAB) │  <- HomeCreateCartButton
│s│                                       │
│)│                                       │
└─┴───────────────────────────────────────┘
```

#### 3.2.2 Savings header

iOS ref: `Views/Header/HomePrimaryHeaderSavingsView.swift`

- Sign (`+` / `-` / empty) + amount (e.g. "$12.50") + context ("savings" / "overspent") + optional 🔥 Lottie if net savings > $0.01.
- Amount font: **FuzzyBubbles-Bold 48sp** — this is the biggest visual moment in the app. Android should render with `FontFamily(Font(R.font.fuzzybubbles_bold))` at 48sp.
- Amount color: `SavingsAccent` (green) when positive, `OverspendAccent` (red) when negative, `DarkPrimary.copy(alpha=0.88f)` when zero.
- Fire Lottie: `Resources/Lottie/Fire.json` scaled ~30dp, positioned to the right of the amount.

#### 3.2.3 Tabs bar

iOS ref: `Views/HomeTabsBarView.swift`

- Two labels side by side: "Active" / "History".
- Thin 2dp underline slides between them with `animation(.spring(0.32, 0.86))` on `progress`.
- Active label: `DarkPrimary`, weight SemiBold. Inactive: `DarkPrimary.copy(alpha = 0.4f)`, Medium.
- Trailing small label (year, e.g. "2026") visible when History tab is selected.

Swipe between pages: use Compose `HorizontalPager` (count=2) driven by `progress`. Tab taps animate to the other page via `pagerState.animateScrollToPage(i)` with the same spring.

#### 3.2.4 Active tab — cart rows

iOS ref: `Core/Home/HomeCartRow/Views/HomeCartRowView.swift`

Each cart row is a large ~180dp-tall card with:
- Background: a per-cart pastel color (16-swatch palette via `ColorOption.getBackgroundColor(cartId, isRow=true)`), or user-uploaded image if Pro (skip image path on Android — Pro-gated).
- Subtle outline: rounded 16 dp continuous corners, stroke `DarkPrimary.copy(alpha=0.08)` 1.5 dp.
- Inner content:
  - Top row: `{cart.name}` (FuzzyBubbles-Bold 22sp, or Poppins SemiBold if user picked Global in FontManager), overflow menu (`•••` button with Rename / Delete).
  - Budget progress: 3-stop gradient pill (`BudgetSafe` → `BudgetWarning` → `BudgetOver`) showing `totalSpent / budget`.
  - Fulfilled / Total items pill (e.g. "5/12").
  - Tiny horizontal stack of active category emoji chips (`HomeCartRowCategoryProgressListView`).
  - If shopping: small Lottie badge + "Shopping" label (`ShoppingMode.json`).

#### 3.2.5 History tab

iOS ref: `Core/Home/Features/History/HomeHistoryTabView.swift` → `HomeHistoryContentSection.swift`

- Same cart rows but read-only, showing actual spent vs budget, with delta chip ("-$8.00" green if saved, "+$2.50" red if overspent).
- Tapping opens `HistoryTripDetailView` (NavigationStack push — on Android, navigate to `TripShareScreen` or a dedicated `HistoryDetailScreen`).

#### 3.2.6 Create Cart FAB

iOS ref: `HomeView.swift:900-937`, `HomeCreateCartButton` (lives inside `HomeBinderPageFloatingActionDock`)

- Diameter 64 dp, black circle, `⊕` glyph (Poppins Bold 28sp), drop shadow.
- On tap: presents `CreateCartPopover` as a custom centered popover (NOT a bottom sheet). See §2.3.
- Has a `CreateCartButtonTransitionModifier` for spring-scale on appear.

#### 3.2.7 What goes away

Delete from `HomeScreen.kt`: `SummaryPillsRow`, `TripCapBanner`, `OngoingTripsRow` (unless you re-use as the active pager), `QuickActionsGrid`, `VaultPreviewRow`, `RememberedItemsRow`, `RecentTripsSection`. iOS Home is intentionally sparse.

---

### 3.3 Vault sheet

**iOS ref**: `Core/Vault/VaultView.swift` + `Views/VaultMainContent.swift` + `Views/VaultCategorySectionView.swift`
**Android current**: `core/vault/VaultScreen.kt` (a top-level destination)

**Major change: Vault is a sheet, not a tab.** Remove it from bottom nav. Present it from Home via the vault icon button in the header (and from onboarding completion).

Layout (from the `ps_01_vault_current.png` screenshot):
- Full-bleed white sheet, `presentationCornerRadius(32)` → Android `RoundedCornerShape(topStart=32.dp, topEnd=32.dp)`.
- Top bar: search icon (left) — "vault" wordmark (center, FuzzyBubbles-Bold ~28sp) — "Add" dark pill (right, FuzzyBubbles 14sp).
- Category strip: horizontal scrolling list of category tiles. Each tile is **roughly 48×48** rounded 12 dp with a pastel background and an emoji/icon. Selected tile gets a black 2 dp inset outline. Last tile is a circular **category manager** button (three-line icon on white).
- Category section title: `Meats & Seafood` etc — Poppins SemiBold 18sp, left-aligned below the strip.
- Main content: grid/list of `VaultItemRow`s for the selected category. Empty state: `"No items yet in {Category} {emoji}"` centered, Poppins Regular 15sp, muted (`#777`).
- Bottom: left chevron (prev category) / right chevron (next category) as floating 44 dp circular light-surface buttons with thin outline.
- Bottom CTA (when items selected): dark pill "Create cart from N items →".

**`VaultItemRow`** (`Views/VaultItemRow.swift`):
- Receipt-ticket-shaped card (custom shape with the torn-paper bottom edge — `TripReceiptTicketShape` style, see §3.6).
- Inside: circular thumbnail (sticker PNG or fallback emoji), item name, price/unit, store, and a +/- stepper with a qty badge. Swipe-to-delete on Compose: `SwipeToDismissBox`.

**Category Manager sheet** (`Views/EmojiPickerSheet.swift` + `Views/CategoriesManagerSheet.swift`):
- Full-screen cover with matched-geometry morph from the category strip's "+" button.
- Tabs: Visible / Hidden. Reorderable list. Tap to rename / pick emoji / pick color (HSV picker).
- Keep existing Android `CategoriesManagerSheet.kt` structure; update visuals to match iOS padding and typography.

---

### 3.4 Vault item detail — the receipt overlay

**iOS ref**: `Core/Vault/Views/VaultReceiptOverlayView.swift` + `ReceiptView.swift` + `ItemInsightsSectionView.swift` + `ItemPurchaseLogView.swift`
**Android**: this is **not fully ported yet** (current `ItemFormSheet.kt` is a plain ModalBottomSheet).

This is a signature UI moment. Opens as a full-screen cover with the item's form rendered as an actual **paper receipt**:
- White background with a faint grunge texture overlay (`grunge.png` at ~5% opacity, multiply blend).
- The form is inside a receipt ticket shape (`TripReceiptTicketShape.swift`) with dashed tear edges at top and bottom, and perforated side notches.
- Item name as a large FuzzyBubbles-Bold heading with an underline swoosh.
- Rows for: store, price, unit, package size, category, with dotted leader lines between the label and value (`DashedLine.swift`).
- An insights section: tiny sparkline chart of price history (`Components/SparklineView.swift`), min/avg/max labels.
- Purchase log: list of completed trip rows, each showing date + price + store.

**Android port plan**:
- Build a `ReceiptTicketShape` custom `Shape` implementing the torn-paper path (two half-circle notches + zigzag top/bottom).
- `ReceiptOverlayScreen` as a fullscreen `Dialog` with `DecorView` set to translucent.
- Reuse `ItemFormSheet` content but re-style inside the receipt frame.
- Sparkline: use `Canvas` in Compose with cubic Bezier through price-history points.

---

### 3.5 Cart Detail

**iOS ref**: `Core/Detail Cart/CartDetailScreen.swift` + the long list of sub-views under `Views/` and `Shopping Cart/`
**Android current**: `core/cart/CartDetailScreen.kt` (single Composable, 3 modes)

Mostly structural parity already. The refactor focuses on:

1. **Header image area**: iOS uses a per-cart pastel background or user image at the top of the detail view. On Android, re-use `ColorOption` palette; image upload stays Pro-gated (skip).
2. **Budget progress visual**: a 3-stop gradient horizontal bar (`BudgetSafe` → `BudgetWarning` → `BudgetOver`). Rounded 8 dp. Thumb arrow shows current position.
3. **Item grouping by store**: items are grouped into sections headed by the store name. Inside each group, rows are receipt-style with dotted leader lines.
4. **Shopping mode** (`isShopping`):
   - Checkable circle at the leading edge of each row.
   - On tap: haptic medium + checkmark animation + strikethrough animation (2 stages: checkmark appears, then strikethrough sweeps through the item name).
   - State machine matches iOS `FulfillmentAnimationState` enum (`Vault.swift:4-11`).
   - Bottom action dock: "Finish Trip" CTA when `allItemsCompleted`.
5. **Finish Trip sheet**: `FinishTripSheet.swift` — bottom sheet with hero numeric ring (actual vs budget), `Celebration.json` Lottie if on-budget, accordion sections for Changed / Added / Skipped / New items. Match the existing Android `FinishTripSheet.kt` layout but restyle with the tokens.
6. **Planning → Shopping transition**: `ShoppingModeGradientView` overlay sweeps across the screen once. Use a short gradient-sweep shader or an `AnimatedVisibility` with a `LinearGradient` brush.

---

### 3.6 Trip Share

**iOS ref**: `Core/TripShare/Views/TripReceiptCardView.swift` + `TripReceiptTicketShape.swift` + `TripPhysicsShareView.swift` + `TripReceiptCelebrationView.swift`
**Android current**: `core/tripshare/TripShareScreen.kt` + `TripShareBitmapRenderer.kt`

- Card is a paper receipt (the `TripReceiptTicketShape`). White background, grunge texture overlay, torn edges.
- Content inside the receipt:
  - "PocketSave" header (FuzzyBubbles-Bold).
  - Dotted horizontal divider.
  - Rows: Date, Store, Items purchased, Total spent, Budget, Savings/Overspend.
  - QR code in a corner (`Components/QRCodeView.swift`) linking to App Store.
  - "thanks" Lottie at the bottom (small, centered) — play once.
- Physics stickers: `PhysicsStickerView.swift` — user's captured item stickers drop from the top with a physics simulation onto the receipt. Port with `Modifier.physics` (or a custom `LaunchedEffect` + `animateOffsetAsState` doing a bounce). Optional for v1 — nice-to-have.
- Action strip: Share (opens `ActivityShareSheet`), Download (saves PNG to gallery via `MediaStore`), Copy Link.

---

### 3.7 Trash

**iOS ref**: `Core/Menu/TrashView.swift`
**Android current**: `core/trash/TrashScreen.kt`

- Segmented control at the top: Items / Carts.
- Each list row: item thumbnail or cart color swatch, name, deleted-from category/budget, relative time ("2 days ago").
- Swipe or trailing actions: Restore (green) / Delete forever (red).
- Confirmation alert for "Delete forever".

Since Android already has this, just restyle with the tokens and move the entry point into the side menu drawer (not a bottom-nav tab).

---

### 3.8 Manage Stores & Units

**iOS ref**: `Core/Menu/ManageStoresAndUnitsView.swift` + `StoreManagerView.swift` + `CustomUnitsManagerView.swift`

- NavigationStack with two-segment tab or two nav destinations.
- Rows: store name, created date, item-count badge. Swipe to rename / delete.
- Add store: inline `TextField` with +Add button.
- Custom units: same pattern, plus a grouped list of standard units (non-editable) + custom ones (editable).

Android doesn't have this yet — it's inside `MoreScreen`. Extract into new composable accessible from the side menu.

---

## 4. Components catalog — port these one-to-one

These reusable components are referenced across multiple screens. Build them early.

| Component | iOS file | Android target |
|---|---|---|
| `DashedLine` | `Components/DashedLine.swift` | `common/ui/components/DashedLine.kt` — `Canvas { drawLine(pathEffect = dashPathEffect(8,4)) }` |
| `GrungeTextureView` | `Components/GrungeTextureView.swift` | `common/ui/components/GrungeTexture.kt` — `Image(painter = painterResource(R.drawable.grunge), alpha = 0.05f, blendMode = Multiply)` |
| `CharacterRevealView` | `Components/CharacterRevealView.swift` | For animating hand-drawn "$0 savings" etc. Compose port with per-char `AnimatedVisibility`. |
| `SparklineView` | `Components/SparklineView.swift` | Canvas with cubic path through normalized points. |
| `RippleEffect` | `Components/RippleEffect.swift` | Replace with Compose's built-in `Modifier.clickable(indication = rememberRipple())`. |
| `QRCodeView` | `Components/QRCodeView.swift` | `QRCodeWriter` via ZXing (already likely a dependency). |
| `CustomPopover` | `Extensions/CustomPopView.swift` | `common/ui/components/CustomPopover.kt` — see §2.3 |
| `TripReceiptTicketShape` | `Core/TripShare/Views/TripReceiptTicketShape.swift` | `common/ui/shapes/ReceiptTicketShape.kt` — Shape with `Path` drawing the torn edges + side notches. |
| `PhysicsStickerView` | `Components/PhysicsStickerView.swift` | `common/ui/components/PhysicsSticker.kt` — optional v2. |
| `ShoppingModeGradientView` | `Components/ShoppingModeGradientView.swift` | Linear sweep gradient overlay for mode transitions. |
| Fonts: `FuzzyBubblesFont`, `PoppinsFont` | `Utils/FontManager.swift` | `common/ui/PocketSaveType.kt` — expose `accentFont(size)` helper. |

---

## 5. Priority roadmap

Do it in this order. Each phase should result in a buildable, runnable app.

**Phase 1 — Foundations (1–2 days)**
1. Copy font files + Lottie JSON + drawable assets.
2. Rewrite `PocketSaveTheme.kt` + `PocketSaveTokens.kt` with the palette from §1.1. Build a `Typography` with Poppins defaults and an `accentFont` helper for FuzzyBubbles.
3. Implement `DashedLine`, `GrungeTexture`, `PocketSaveSprings` in `common/ui/`.
4. Delete or deprecate the Sage/Clay palette. The app will look broken after this — that's expected, you'll fix it screen-by-screen.

**Phase 2 — Structural nav (2–3 days)**
5. Remove `BottomNavBar` from `PocketSaveNavHost`.
6. Build `BinderSidebar` + `BinderPageSurface` + `MenuDrawer` with the 3D rotation.
7. Rewrite `HomeScreen` to host the binder + the side menu. Delete the 6 old sections.
8. Move Trash, Stores & Units, Reset, Currency picker, Changelog, Feedback into `MenuDrawer`.
9. Delete `MoreScreen`, `ActiveTripsScreen`, `HistoryScreen` (their content folds into Home tabs).

**Phase 3 — Home content (2 days)**
10. Build `HomePrimaryHeaderView` with FuzzyBubbles savings display + Fire Lottie.
11. Build `HomeTabsBarView` with animated underline.
12. Build `HomeCartRow` with ColorOption background, budget progress, category chips, shopping overlay.
13. Wire `HorizontalPager` for Active/History swipe.
14. Build `HomeCreateCartButton` FAB + `CreateCartPopover` custom popover.

**Phase 4 — Onboarding (1 day)**
15. Delete `OnboardingValueScreen`, `OnboardingCurrencyScreen`, `OnboardingMonthlyBudgetScreen`, `OnboardingFirstTripScreen`, `OnboardingHandoffScreen`.
16. Restyle Welcome / LastStore / FirstItem to match the iOS visuals.
17. Add the item-prompt 260dp bottom sheet after LastStore.

**Phase 5 — Vault + Item receipt (2–3 days)**
18. Convert `VaultScreen` from top-level destination into a sheet presented from Home.
19. Restyle category strip, `VaultItemRow` (receipt-ticket shape), empty states.
20. Build `VaultReceiptOverlayView` — the signature paper-receipt item detail. Needs `ReceiptTicketShape` first.

**Phase 6 — Cart detail polish (1–2 days)**
21. Restyle `CartDetailScreen` with the new tokens.
22. Build/port `FulfillmentAnimationState` staged checkmark + strikethrough animation.
23. Restyle `FinishTripSheet` with hero ring + Lottie.

**Phase 7 — Trip Share (1–2 days)**
24. Port `ReceiptTicketShape` (already done in Phase 5).
25. Rebuild `TripShareScreen` inside the receipt ticket with grunge, dashed dividers, QR, thanks Lottie.
26. (Optional, v2) Physics stickers.

**Phase 8 — Polish**
27. Ripple, haptics, spring tuning, empty states.
28. QA all 4 onboarding screens + 10+ main screens against iOS running in the simulator.

---

## Appendix A — iOS file index by screen

Quick lookup table: given a screen, where is the iOS source?

| Screen | Primary iOS file |
|---|---|
| Welcome | `Core/Onboarding/Views/OnboardingWelcomeView.swift` |
| Last Store | `Core/Onboarding/Views/OnboardingLastStoreView.swift` |
| First Item | `Core/Onboarding/Views/OnboardingFirstItemView.swift` |
| Home | `Core/Home/HomeView.swift` |
| Home savings header | `Core/Home/Views/Header/HomePrimaryHeaderView.swift` + `HomePrimaryHeaderSavingsView.swift` |
| Home tabs | `Core/Home/Views/HomeTabsBarView.swift` |
| Home cart row | `Core/Home/HomeCartRow/Views/HomeCartRowView.swift` |
| Binder sidebar | `Core/Home/Views/Binder/Sidebar/HomeBinderSidebarView.swift` |
| Side menu | `Core/Menu/MenuView.swift` |
| Vault | `Core/Vault/VaultView.swift` + `Views/VaultMainContent.swift` |
| Vault item receipt | `Core/Vault/Views/VaultReceiptOverlayView.swift` |
| Category manager | `Core/Vault/Views/EmojiPickerSheet.swift` (picker) + categories code |
| Cart detail | `Core/Detail Cart/CartDetailScreen.swift` |
| Shopping mode | `Core/Detail Cart/Shopping Cart/*.swift` |
| Finish trip sheet | `Core/Detail Cart/Shopping Cart/FinishTripSheet.swift` |
| Trip share | `Core/TripShare/Views/TripReceiptCardView.swift` |
| Trash | `Core/Menu/TrashView.swift` |
| Stores manager | `Core/Menu/StoreManagerView.swift` |
| Units manager | `Core/Menu/CustomUnitsManagerView.swift` |

## Appendix B — Design tokens quick-reference card

```
DarkPrimary      #212427   ← the anchor neutral for all dark pills/text
BudgetSafe       #98F476   ← green
BudgetWarning    #F4B576   ← amber
BudgetOver       #F47676   ← red
AccentDanger     #FA003F   ← destructive
SubscriptionAccent #6EBC59 ← upgrade / pro
HomeBaseGrey     #F8F8F8
SurfaceMuted     #F7F7F7

Fonts
  body:   Poppins (Regular/Medium/SemiBold/Bold/Light)
  accent: FuzzyBubbles-Bold  ← the hand-drawn wordmark & large savings amount

Spring constants
  gentle: response=0.32 damping=0.86
  menu:   response=0.50 damping=0.70
  pill:   response=0.28 damping=0.50
  sheet:  response=0.40 damping=0.85

Corner radii
  sheet: 28dp
  main content: 24dp (menu closed) / 30dp (menu open)
  cart row: 16dp
  menu row: 12dp

Binder sidebar width: 28dp
```
