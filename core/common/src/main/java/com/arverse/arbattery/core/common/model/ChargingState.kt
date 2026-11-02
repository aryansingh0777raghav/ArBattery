package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChargingState {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}
