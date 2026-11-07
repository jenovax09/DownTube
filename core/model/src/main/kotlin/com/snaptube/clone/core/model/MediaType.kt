package com.snaptube.clone.core.model

/** High level media classification used by the library, player and notifications. */
enum class MediaType(val storageValue: Int) {
    VIDEO(0),
    AUDIO(1),
    IMAGE(2),
    OTHER(3);

    companion object {
        fun fromStorage(value: Int): MediaType =
            entries.firstOrNull { it.storageValue == value } ?: OTHER
    }
}
