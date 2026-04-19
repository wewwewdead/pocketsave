# PocketSave — Android monetization handoff

Android-only, RevenueCat + Google Play subscriptions, single `pro` entitlement.
This document covers the shipped architecture, the external setup still
required before real purchases can happen, where to find the premium gates,
how to change the paywall, and how to test purchases in an internal track.

For the operational setup checklist (dashboards, product ids, service
accounts), see `BILLING_SETUP.md` alongside this file.

---

## 1 · What was implemented

A complete Android B2C monetization layer delivered in nine phases:

- **Billing foundation** — RevenueCat SDK wired in; a `SubscriptionManager`
  interface with a single concrete implementation
  (`RevenueCatSubscriptionManager`) so the rest of the app never imports
  from `com.revenuecat.purchases.*`.
- **Config surface** — every billing identifier (entitlement, offering,
  product ids) lives in `BillingConfig.kt`. The RevenueCat public API key is
  injected from `local.properties` through `BuildConfig`, so no dashboard-
  specific values live in committed source.
- **State layer** — a single `StateFlow<SubscriptionState>` plus a derived
  `StateFlow<Boolean>` `isPro` that the whole app reads from. Offline-first:
  seeded from a cached flag in DataStore on cold launch so the first frame
  is correct before any network fetch returns.
- **Paywall screen** — Compose UI that sells PocketSave Pro specifically.
  Trigger-aware hero subtitle, monthly + yearly plan selector with a
  computed savings pill, "Manage subscription in Play Store" for
  already-Pro users, and a working Restore flow. All snackbars +
  auto-dismissal are wired.
- **Feature gates** — every premium trigger point in the app routes through
  `PaywallGate.check(feature) { ... }`. Counts come from
  `FeatureLimits.kt`; both live under `com/pocketsave/billing/`.
- **Graceful locked states** — in-place `ProChip` badges on locked
  affordances (Scan, Customize background, Share trip, New category at cap)
  plus a contextual `CapHintBanner` that only surfaces when a free user is
  actually at a count cap.
- **Edge-case hardening** — refresh-on-foreground via `ProcessLifecycleOwner`,
  concurrent-refresh coalescing, `Purchases.configure` failures caught and
  soft-handled, widget refresh on entitlement flip.
- **Analytics hooks** — a minimal `BillingAnalytics` interface with no-op
  default and logcat-during-debug. Eight events land from
  `PaywallViewModel`; swap the sink at a single seam in `AppContainer`.
- **First-run preserved** — onboarding is never gated; the first store /
  item / cart created during onboarding does not count against free caps
  in a way that would ever block the tutorial.

**Every file touched by monetization is under `android/`.** iOS sources
(`../PocketSave/`, `../PocketSaveWidgets/`) were not edited, read, or
referenced by the runtime.

---

## 2 · Architecture map

```
com.pocketsave/
├── PocketSaveApplication.kt       # configures Purchases + ProcessLifecycleOwner refresh
├── app/
│   ├── AppContainer.kt            # owns: subscriptionManager, billingAnalytics,
│   │                              #       widget-refresh-on-isPro collector
│   └── PocketSaveNavHost.kt       # Routes.PAYWALL = "paywall/{trigger}"
│                                  # plumbs subscriptionManager + onOpenPaywall to screens
├── billing/
│   ├── BillingConfig.kt           # entitlement id, offering id, product ids, API key
│   ├── SubscriptionState.kt       # snapshot consumed by paywall / settings
│   ├── SubscriptionManager.kt     # interface
│   ├── RevenueCatSubscriptionManager.kt   # only class that imports RC SDK
│   ├── PurchaseResult.kt          # sealed: Success / UserCancelled / NotConfigured / Error
│   ├── PremiumFeature.kt          # sealed: 8 features, each with trigger copy
│   ├── FeatureLimits.kt           # free-tier caps
│   ├── PaywallGate.kt             # check() / isAllowed() / openPaywall()
│   ├── BillingAnalytics.kt        # fun interface + 9 events + NoOp + Logcat impls
│   └── (data/prefs/AppPreferences.isProCached — cached offline hint)
└── core/paywall/
    ├── PaywallScreen.kt           # hero + benefits + plan selector + CTA + fine print + restore
    ├── PaywallViewModel.kt        # purchase/restore orchestration, emits analytics events
    └── ProIndicators.kt           # shared ProChip + CapHintBanner composables
```

### Flow: free user hits a gate

```
onClick in feature screen
  └─ paywallGate.check(PremiumFeature.X) { allowedAction() }
       └─ if isAllowed(X): allowedAction()
       └─ else:           onNavigateToPaywall(X)
                           └─ navController.navigate(Routes.paywall(X.key))
                                └─ PaywallScreen(trigger = X)
                                     └─ VM.init → analytics.track(PaywallShown)
                                     └─ hero subtitle = X.copy.heroSubtitle
                                     └─ user buys → isPro = true
                                        └─ VM emits PurchaseSucceeded
                                        └─ applyCustomerInfo() persists cache
                                        └─ auto-dismiss → popBackStack() → caller
```

### Flow: app relaunch

```
PocketSaveApplication.onCreate
  └─ AppContainer(this)
       └─ SubscriptionManager constructor (not configured yet)
       └─ AppContainer.init:
            └─ collector on subscriptionManager.isPro (drop 1) → widget refresh
  └─ subscriptionManager.start()
       └─ seed state.isPro from AppPreferences.isProCachedNow() (offline first)
       └─ Purchases.configure() (guarded, skipped on placeholder key)
       └─ Purchases.updatedCustomerInfoListener attached
       └─ refresh() → awaitOfferings + awaitCustomerInfo → state updated + cache written
  └─ ProcessLifecycleOwner.onStart → subscriptionManager.refresh() (coalesced)
```

---

## 3 · Where premium checks live

All paywall-triggering behaviour routes through one of three patterns. No
feature file has a hand-rolled `if (!isPro)` branch.

### Pattern A — direct gate check

Used wherever the screen owns the action.

```kotlin
val gate = rememberPaywallGate(subscriptionManager, vaultService, onOpenPaywall)
// ...
Button(onClick = {
    gate.check(PremiumFeature.CreateActiveTrip) { showCreateCartSheet = true }
})
```

Screens using pattern A today:

| Screen | Gates | File |
|---|---|---|
| Home | New trip (quick actions + ongoing empty state) | `core/home/HomeScreen.kt` |
| Active trips | New trip (summary + empty state) | `core/active/ActiveTripsScreen.kt` |
| Vault | Add item, Create cart from selection, Scanner forward, Category add forward | `core/vault/VaultScreen.kt` |
| Cart detail | Customize background, Share trip | `core/cart/CartDetailScreen.kt` |
| History | Open-paywall on "Unlock full history" row | `core/history/HistoryScreen.kt` |

### Pattern B — lambda forwarding

Used inside reusable sheets so the sheet itself stays billing-agnostic.

```kotlin
// ItemFormSheet signature
onScanRequested: (onAllowed: () -> Unit) -> Unit = { it() }

// ItemFormSheet scan button
TextButton(onClick = { onScanRequested { showScanner = true } })

// VaultScreen caller
onScanRequested = { onAllowed ->
    paywallGate.check(PremiumFeature.Scanner, onAllowed)
}
```

Used by `ItemFormSheet.kt` (Scanner) and `CategoriesManagerSheet.kt`
(AddCustomCategory).

### Pattern C — display-only

Used where the UI shows "Pro" or a cap banner without intercepting input.

```kotlin
ProChip()                                   // next to a label
CapHintBanner(label = "…", onUpgrade = { onOpenPaywall(X) })
```

Used by `CartDetailScreen.kt` overflow items, `CategoriesManagerSheet.kt`
(cap status line), `ItemFormSheet.kt` (Scan button badge),
`VaultScreen.kt` (item-cap banner), `HomeScreen.kt` +
`ActiveTripsScreen.kt` (trip-cap banner), `MoreScreen.kt` (Pro membership
row), `HistoryScreen.kt` (unlock-more row), and
`PurchasedItemsGlanceWidget.kt` (widget Pro upsell).

### Gate definitions

`billing/PaywallGate.kt` — `isAllowed(feature)` returns:

```
isPro → true
CreateActiveTrip → activeCartCount < FREE_ACTIVE_CARTS
AddVaultItem     → vaultItemCount  < FREE_VAULT_ITEMS
AddCustomCategory→ customCategoryCount < FREE_CUSTOM_CATEGORIES
DeepHistory      → completedTripCount <= FREE_HISTORY_ROWS
Scanner / CartTheme / TripShareCard / Widget → false (binary gate)
```

Caps live in `billing/FeatureLimits.kt` as named constants.

---

## 4 · Free vs Pro — what each user sees

| Capability | Free | Pro |
|---|---|---|
| Onboarding (all 7 steps) | ✅ | ✅ |
| Active trips concurrently | 2 | unlimited |
| Finish / reopen / delete trips | ✅ | ✅ |
| Shopping-mode quick-add | ✅ | ✅ |
| Vault items | 10 | unlimited |
| Stores (implicit, via items) | bounded by item cap | unlimited |
| Default categories | ✅ | ✅ |
| Custom categories | 2 | unlimited |
| Scanner / OCR autofill | — | ✅ |
| Cart themes (colour + photo) | default only | ✅ |
| Trip share card | system share only | ✅ |
| Home-screen widget | upsell tile | live cart |
| History rollup numbers | all trips | all trips |
| History — trip list | 10 shown + unlock row | every trip |
| Trash / restore / reset / currency | ✅ | ✅ |
| Manage / Restore subscription | paywall row + More → Pro | same |

No data is ever deleted when a Pro user cancels. Caps only block *adding
more*; existing rows remain visible, editable, deletable.

---

## 5 · External setup still required

None of the code changes require more work to compile or run — the app
launches, stays free, and behaves identically to the pre-monetization state
until the setup below lands. Full checklist with verification steps lives
in `BILLING_SETUP.md`.

### Play Console

- Upload a signed AAB to any test track (internal testing is fine).
- Create two subscription products — **ids must match code exactly**:
  - `pocketsave_pro_monthly` (monthly auto-renewing)
  - `pocketsave_pro_yearly`  (yearly auto-renewing)
- Set price + availability for each base plan and activate both products.
- Setup → License testing → add tester Google accounts.
- Have testers join the internal-testing opt-in URL so Play Billing treats
  them as license testers.

### RevenueCat dashboard

- Create / select a project; add an Android app with package `com.pocketsave`.
- Project settings → API keys → generate / copy the **Android public SDK key**.
- Entitlements → create one with identifier exactly `pro`.
- Products → add the two Play product ids from above; attach both to `pro`.
- Offerings → create an offering with identifier `default` containing
  monthly + annual packages, flagged **Current**.
- Integrations → Google Play → upload the Play Developer API service
  account JSON.

### Local

- Add one line to `android/local.properties` (git-ignored, per-developer):

```
revenuecat.apiKey.android=goog_YourRealAndroidPublicSdkKey
```

- Rebuild. `BillingConfig.isApiKeyConfigured` flips to `true`; the
  subscription manager configures on next `Application.onCreate`.

If any of the above is missing, `RevenueCatSubscriptionManager.start()`
logs a warning under tag `SubscriptionManager` and leaves billing inert —
every gate continues to work, but purchases are unavailable.

---

## 6 · How to change the paywall later

### Copy changes (most common)

- **Trigger subtitles** — edit `TriggerCopy.heroSubtitle` strings in
  `billing/PremiumFeature.kt`. Each `PremiumFeature.data object` owns its
  own line; changes are compile-safe.
- **Default subtitle / hero title / benefits list / fine print** — edit
  `core/paywall/PaywallScreen.kt`. `HeroSection`, `BenefitsCard`,
  `FinePrint` are private composables in the same file.
- **Benefits list items** — edit the `items = listOf(BenefitLine…)` block
  inside `BenefitsCard`. Each line has icon + title + body.

### Design changes

- Paywall screen is pure Compose. All colours come from
  `PocketSaveTokens.pastels` so the paywall tracks the app's theme
  automatically. `PlanCard`, `PrimaryCta`, `RestoreRow`,
  `ManageSubscriptionButton`, `ProChip`, `CapHintBanner` are the swap
  points. The last two live in `core/paywall/ProIndicators.kt` and are
  reused by every locked surface in the app.

### Limit changes

- Edit numbers in `billing/FeatureLimits.kt`:

```kotlin
const val FREE_ACTIVE_CARTS = 2
const val FREE_VAULT_ITEMS = 10
const val FREE_CUSTOM_CATEGORIES = 2
const val FREE_HISTORY_ROWS = 10
```

Every gate and banner reads these — no callsite changes.

### Adding a new gate

1. Add a `data object NewGate : PremiumFeature(key = "new_gate", copy = TriggerCopy(…))`.
2. Add a branch to `PaywallGate.isAllowed`. If it's count-based, add a
   constant to `FeatureLimits` and a private helper.
3. At the call site, `paywallGate.check(PremiumFeature.NewGate) { … }`.

### Switching paywall UI to RevenueCat's `paywalls-ui`

The Phase 4 implementation is hand-rolled on top of `PaywallViewModel` so
design iteration doesn't require RC dashboard edits. To switch to RC's
drop-in paywall:

1. Add dependency `com.revenuecat.purchases:purchases-ui:<same version>`.
2. Configure the paywall in RC dashboard → Paywalls.
3. Replace the body of `PaywallScreen` with `Paywall(offering = … )` from
   RC-UI; keep the VM's analytics emit sites wired.

### Swapping the analytics backend

`billing/BillingAnalytics.kt` is a one-method `fun interface`. Write an
implementation that forwards `BillingEvent` → Firebase / Mixpanel /
PostHog and swap it in one line in `AppContainer.billingAnalytics`. No
feature code or paywall code changes.

---

## 7 · How to test purchases

### Local sanity without real purchases

- Leave `local.properties` without `revenuecat.apiKey.android`. App starts
  with `BillingConfig.isApiKeyConfigured = false`.
- All gates still fire; paywall opens with the `Pricing unavailable` card.
- Useful for verifying trigger-point copy, navigation, close / dismiss
  behaviour, and first-run flows.

### Internal-testing flow with sandbox purchases

1. Complete the Play Console + RevenueCat setup from §5.
2. In Play Console, set up an **Internal testing** track and upload a
   signed AAB (version code ≥ the last one on the track).
3. Add at least one tester email to the internal tester list, send them
   the opt-in URL, and have them accept.
4. In Play Console → Setup → License testing, add the same tester email.
   License testers get sandbox purchases that don't charge.
5. Uninstall any previous Play Store copy from the test device. Install
   the test-track build via the Play Store (not sideload, or RC can't
   resolve the product ids).
6. Open PocketSave → trip to paywall via More → PocketSave Pro, or via
   any gated action.
7. Start the subscription. Play sheet shows "This purchase will not be
   charged" — proceed.
8. `BillingAnalytics` logcat emits the funnel in debug builds:

   ```
   adb logcat -s PocketSaveBilling:I
   ```

   Expected sequence on a happy-path purchase:

   ```
   PaywallShown(trigger=…)
   PurchaseStarted(productId=pocketsave_pro_yearly, trigger=…)
   PurchaseSucceeded(productId=pocketsave_pro_yearly, trigger=…)
   PaywallDismissed(trigger=…)
   ```

9. Confirm `isPro = true` by:
   - Home shows no trip-cap banner regardless of count.
   - Scan button in Item form has no PRO chip.
   - CartDetail overflow has no PRO chips.
   - Widget (re-pinned or waiting for next tick) shows live cart.
   - History shows every trip, no unlock row.
10. Cancel the subscription from the device's Google Play → Subscriptions.
    RC's `updatedCustomerInfoListener` receives the cancellation; the app
    transitions back to free UI at the next `isPro` push (immediate in
    most cases, worst case next foreground refresh).

### Restore flow

1. Sign into the device's Play account with the account that owns the
   subscription.
2. Uninstall + reinstall PocketSave.
3. Open paywall → tap **Restore purchases**.
4. Expected logcat:

   ```
   RestoreStarted
   RestoreSucceeded(isPro=true)
   PaywallDismissed(trigger=…)
   ```

5. App returns to the caller with `isPro = true`.

### Edge cases worth flipping on during QA

- Airplane mode cold start while cached Pro → app reads `isProCached`
  immediately; gates stay open; `PricingUnavailableCard` on paywall opens.
- Airplane mode cold start while cached free → app treats user as free;
  any gate tap shows paywall with `PricingUnavailableCard` → `Try again`.
- Purchase cancelled on Play sheet → VM clears `isPurchasing`; paywall
  stays open; logcat shows `PurchaseCancelled`.
- Paywall opened from More tab → no trigger arg; hero shows default
  "Plan smarter. Shop calmer." subtitle.
- Already-Pro user opens paywall → plan selector + CTA + fine print
  hidden; "Manage subscription in Play Store" shown; Restore still works.

---

## 8 · Architecture summary

- **One entitlement.** `pro`. Everything else derives from it.
- **One abstraction.** `SubscriptionManager` interface; RevenueCat SDK
  imports live only in `RevenueCatSubscriptionManager`.
- **One config home.** Every identifier lives in `BillingConfig.kt`; the
  API key comes from `local.properties` via `BuildConfig`.
- **One gate helper.** `PaywallGate.check(feature) { … }` used by every
  triggering UI in the app. No hand-rolled `if (!isPro)` branches outside
  `billing/`.
- **One analytics seam.** `BillingAnalytics` interface; swap impls in
  `AppContainer` without touching any other file.
- **Offline-first.** `AppPreferences.isProCached` seeds `state.isPro` on
  cold launch so the first frame is correct before any network fetch.
- **Gracefully inert.** Placeholder API key, missing Play Services, or
  network failure leaves `isPro = false`, no exceptions, no crashes.
- **Android-only.** iOS sources are not edited, read, or imported.

This document is the canonical handoff. For the dashboard + store checklist
keep referring to `BILLING_SETUP.md`.
