package com.pocketsave.domain.model

/** Mirrors `FulfillmentAnimationState` from `PocketSave/Models/Vault.swift`. */
enum class FulfillmentAnimationState(val raw: Int) {
    NONE(0),
    CHECKMARK_APPEARING(1),
    CHECKMARK_VISIBLE(2),
    STRIKETHROUGH_ANIMATING(3),
    STRIKETHROUGH_COMPLETE(4),
    REMOVAL_ANIMATING(5);

    companion object {
        fun fromRaw(raw: Int): FulfillmentAnimationState =
            entries.firstOrNull { it.raw == raw } ?: NONE
    }
}
