# PocketSave — Android billing setup

One-time, Android-only. Everything below is **outside the app** — no code
changes are required to work through this list. The app compiles and runs
whether or not any of this is in place; billing simply stays inert until the
API key arrives in `local.properties`.

Single code-side source of truth: `app/src/main/java/com/pocketsave/billing/BillingConfig.kt`.
If you rename anything in a dashboard, update the matching constant there.

---

## 1 · RevenueCat dashboard

1. Create (or select) a RevenueCat **project**.
2. Add an **Android app** with package name `com.pocketsave`.
3. **Project settings → API keys** → copy the *Android public SDK key*
   (starts with `goog_`). Paste it into `android/local.properties` — see §3.
4. **Entitlements** → create one with identifier exactly:
   - `pro`
5. **Products** → add two Google Play products (ids must match Play Console
   exactly — see §2):
   - `pocketsave_pro_monthly`
   - `pocketsave_pro_yearly`

   Attach both to the `pro` entitlement.
6. **Offerings** → create an offering with identifier:
   - `default`

   Add a monthly package (backed by `pocketsave_pro_monthly`) and an annual
   package (backed by `pocketsave_pro_yearly`). Flag this offering as
   **Current**.
7. **Integrations → Google Play** → upload the Google Play Developer API
   service account JSON (from Google Cloud Console → your Play-linked
   project). This lets RevenueCat validate receipts and receive renewal /
   cancellation pushes.

---

## 2 · Google Play Console

1. Upload a signed AAB to any test track (internal testing is fine). You
   can't create subscription products until Play has a release on file.
2. **Monetize → Subscriptions** → create two subscription products:

   | Product id | Base plan | Renewal |
   |---|---|---|
   | `pocketsave_pro_monthly` | Auto-renewing | Monthly |
   | `pocketsave_pro_yearly`  | Auto-renewing | Yearly  |

   Product ids must match the RevenueCat ids exactly — RevenueCat looks
   them up by id.
3. Set prices + availability for each base plan and **activate** the
   products. RevenueCat cannot resolve inactive products.
4. **Setup → License testing** → add your tester Google accounts so
   sandbox purchases don't charge real money.
5. Have testers join the internal-testing track via the opt-in URL so
   Play Billing treats them as license testers.

---

## 3 · Local secrets

Add one line to `android/local.properties` (git-ignored, per-developer):

```
revenuecat.apiKey.android=goog_YourRealAndroidPublicSdkKey
```

Gradle injects this into `BuildConfig.REVENUECAT_ANDROID_API_KEY`, which
`BillingConfig.REVENUECAT_ANDROID_API_KEY` reads at runtime.

If the property is missing, a placeholder is used;
`BillingConfig.isApiKeyConfigured` returns `false`,
`RevenueCatSubscriptionManager.start()` logs a warning and stays inert, and
the app continues to run with `isPro = false` for everyone.

CI pipelines should set the property the same way — either by writing a
`local.properties` file or by passing `-Prevenuecat.apiKey.android=...` and
reading it in `app/build.gradle.kts` (if you go that route, extend the
`Properties.getProperty` call there).

---

## 4 · Config mapping (dashboard → code)

| Dashboard / store | Constant |
|---|---|
| RevenueCat entitlement id `pro` | `BillingConfig.ENTITLEMENT_PRO` |
| RevenueCat offering id `default` (Current) | `BillingConfig.OFFERING_DEFAULT` |
| Play + RC product id `pocketsave_pro_monthly` | `BillingConfig.PRODUCT_ID_MONTHLY` |
| Play + RC product id `pocketsave_pro_yearly`  | `BillingConfig.PRODUCT_ID_YEARLY` |
| RC Android public SDK key | `BillingConfig.REVENUECAT_ANDROID_API_KEY` (from `local.properties`) |

---

## 5 · Verification

After completing §1–3, install a debug build on a device signed into a
license-test Google account and confirm:

- [ ] Logcat tag `SubscriptionManager` emits **no** placeholder-key warning.
- [ ] `SubscriptionManager.state.value.isConfigured == true`.
- [ ] `state.currentOffering` is non-null and contains monthly + yearly
      packages (package count ≥ 2).
- [ ] A sandbox purchase of either package flips `isPro` to `true` within a
      second of the Play sheet dismissing.
- [ ] Clearing app data (or a reinstall) drops `isPro` to `false`, and a
      "Restore purchases" call (wired in a later phase) brings it back
      without needing to re-enter Play.
- [ ] Turning airplane mode on after a cold start leaves a previously-Pro
      user as `isPro = true` (the DataStore cache covers offline launches).

---

## 6 · Values you must fill in later

| Where | What to provide |
|---|---|
| `android/local.properties` | `revenuecat.apiKey.android=goog_…` (RC dashboard → API keys) |
| RevenueCat dashboard | Android public SDK key *(generate once per project)* |
| RevenueCat dashboard | Entitlement id: `pro` |
| RevenueCat dashboard | Offering id: `default` (flagged Current) |
| RevenueCat dashboard | Product ids attached to `pro`: `pocketsave_pro_monthly`, `pocketsave_pro_yearly` |
| Google Play Console | Subscription product ids: `pocketsave_pro_monthly`, `pocketsave_pro_yearly` |
| Google Play Console | Prices + availability per base plan (monthly + yearly) |
| Google Play Console | Signed AAB uploaded to at least one test track |
| Google Cloud Console | Play Developer API service account JSON |
| RevenueCat dashboard | Play integration: upload that service account JSON |

Nothing on this list is code. Once §1–3 are complete, the Phase 2 subscription
foundation starts serving live data automatically on the next cold launch.
