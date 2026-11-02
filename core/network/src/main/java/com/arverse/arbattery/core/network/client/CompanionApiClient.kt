package com.arverse.arbattery.core.network.client

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.network.dto.ApiResponseDto
import com.arverse.arbattery.core.network.dto.BatteryReportDto
import com.arverse.arbattery.core.network.dto.PairingRequestDto
import com.arverse.arbattery.core.network.dto.PairingResponseDto
import com.arverse.arbattery.core.network.dto.SyncRequestDto
import com.arverse.arbattery.core.network.dto.SyncResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class CompanionApiClient(
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5000
            requestTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }

    suspend fun discoverHubOnSubnet(
        localIp: String,
        port: Int = 8443
    ): String? = withContext(dispatchers.io) {
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext null
        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
        val myLastOctet = parts[3].toIntOrNull() ?: -1

        var found: String? = null
        coroutineScope {
            val jobs = (1..254).map { i ->
                if (i == myLastOctet) return@map null
                launch {
                    val candidate = "$prefix$i"
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(candidate, port), 350)
                            val res = checkHubHealth(candidate, port)
                            if (res is AppResult.Success && res.data) {
                                found = candidate
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }.filterNotNull()

            val start = System.currentTimeMillis()
            while (found == null && jobs.any { it.isActive } && (System.currentTimeMillis() - start) < 4000) {
                delay(100)
            }
            jobs.forEach { it.cancel() }
        }
        found
    }

    suspend fun checkHubHealth(hubHost: String, hubPort: Int): AppResult<Boolean> = withContext(dispatchers.io) {
        try {
            val response = client.get("http://$hubHost:$hubPort/api/v1/health")
            AppResult.Success(response.status.value in 200..299)
        } catch (e: Exception) {
            AppResult.Error(e, "Cannot reach Hub at $hubHost:$hubPort: ${e.message}")
        }
    }

    suspend fun requestPairing(
        hubHost: String,
        hubPort: Int,
        deviceId: String,
        deviceName: String,
        deviceType: String
    ): AppResult<PairingResponseDto> = withContext(dispatchers.io) {
        try {
            val request = PairingRequestDto(
                deviceId = deviceId,
                deviceName = deviceName,
                deviceType = deviceType,
                timestamp = System.currentTimeMillis(),
                clientNonce = UUID.randomUUID().toString()
            )
            val response: PairingResponseDto = client.post("http://$hubHost:$hubPort/api/v1/pairing/request") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.status == "ACCEPTED" && response.authToken != null) {
                AppResult.Success(response)
            } else {
                AppResult.Error(IllegalStateException(response.message ?: "Pairing request rejected by Hub"))
            }
        } catch (e: Exception) {
            AppResult.Error(e, "Pairing request failed: ${e.message}")
        }
    }

    suspend fun confirmPairing(
        hubHost: String,
        hubPort: Int,
        deviceId: String,
        authToken: String
    ): AppResult<Boolean> = withContext(dispatchers.io) {
        try {
            val confirm = com.arverse.arbattery.core.network.dto.PairingConfirmDto(
                deviceId = deviceId,
                authToken = authToken,
                timestamp = System.currentTimeMillis()
            )
            val response: ApiResponseDto<Unit> = client.post("http://$hubHost:$hubPort/api/v1/pairing/confirm") {
                contentType(ContentType.Application.Json)
                setBody(confirm)
            }.body()

            if (response.success) {
                AppResult.Success(true)
            } else {
                AppResult.Error(IllegalStateException(response.message ?: "Pairing confirmation rejected"))
            }
        } catch (e: Exception) {
            AppResult.Error(e, "Pairing confirmation failed: ${e.message}")
        }
    }

    suspend fun sendBatteryReport(
        hubHost: String,
        hubPort: Int,
        authToken: String,
        report: BatteryReportDto
    ): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val response: ApiResponseDto<String> = client.post("http://$hubHost:$hubPort/api/v1/devices/battery") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $authToken")
                setBody(report)
            }.body()

            if (response.success) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error(IllegalStateException(response.message ?: "Failed to update battery"))
            }
        } catch (e: Exception) {
            AppResult.Error(e, "Battery sync failed: ${e.message}")
        }
    }

    suspend fun reportBattery(
        host: String,
        port: Int,
        token: String,
        report: BatteryReportDto
    ): AppResult<Unit> = sendBatteryReport(host, port, token, report)

    suspend fun syncReadings(
        hubHost: String,
        hubPort: Int,
        authToken: String,
        request: SyncRequestDto
    ): AppResult<SyncResponseDto> = withContext(dispatchers.io) {
        try {
            val response: SyncResponseDto = client.post("http://$hubHost:$hubPort/api/v1/sync") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $authToken")
                setBody(request)
            }.body()

            AppResult.Success(response)
        } catch (e: Exception) {
            AppResult.Error(e, "Batch sync failed: ${e.message}")
        }
    }

    fun close() {
        client.close()
    }
}
