package com.pocketsave

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pocketsave.app.AppContainer

/**
 * Process-scoped owner of the [AppContainer]. Mirrors iOS `PocketSaveApp` in
 * `PocketSave/App/GrockApp.swift`, which builds the SwiftData container and
 * services at launch.
 */
class PocketSaveApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // RevenueCat must be configured from Application.onCreate so any
        // entry point (widget provider, deep link, main activity) can safely
        // read Purchases.sharedInstance immediately. The manager guards
        // against double-configure internally, so this is safe even if the
        // process is restarted while the AppContainer still holds state.
        container.subscriptionManager.start()

        // Refresh entitlement on every app foreground so a subscription
        // bought / cancelled / refunded on another device while we were
        // backgrounded lands as soon as the user re-opens the app. Consecutive
        // calls are safe — the manager coalesces concurrent refreshes. The
        // first ON_START fires right after [start], which already refreshes;
        // running a second refresh that effectively no-ops is cheaper than
        // maintaining a "skip the first" flag.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.subscriptionManager.refresh()
            }
        })
    }
}
