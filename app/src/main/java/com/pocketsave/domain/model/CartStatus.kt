package com.pocketsave.domain.model

/**
 * Mirrors the iOS `CartStatus` enum declared in `PocketSave/Models/Vault.swift`.
 * Raw integer values must match the iOS values so SwiftData exports remain interchangeable.
 */
enum class CartStatus(val raw: Int) {
    PLANNING(0),
    SHOPPING(1),
    COMPLETED(2);

    val displayName: String
        get() = when (this) {
            PLANNING -> "Planning"
            SHOPPING -> "Shopping"
            COMPLETED -> "Completed"
        }

    companion object {
        fun fromRaw(raw: Int): CartStatus =
            entries.firstOrNull { it.raw == raw } ?: PLANNING
    }
}
