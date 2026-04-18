package com.pocketsave

import android.app.Application
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
    }
}
