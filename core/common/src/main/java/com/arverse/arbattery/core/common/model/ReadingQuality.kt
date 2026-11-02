package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReadingQuality {
    LIVE,
    RECENT,
    STALE,
    UNAVAILABLE,
    UNSUPPORTED,
    ERROR
}
