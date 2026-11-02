package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}
