package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType(val displayName: String) {
    SMARTWATCH("Smartwatch"),
    TABLET("Tablet"),
    SMARTPHONE("Smartphone"),
    LAPTOP("Laptop"),
    EARBUDS("Earbuds"),
    HEADPHONES("Headphones"),
    ACCESSORY("Accessory"),
    UNKNOWN("Unknown Device")
}
