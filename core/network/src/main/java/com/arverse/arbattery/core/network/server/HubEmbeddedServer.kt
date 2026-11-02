package com.arverse.arbattery.core.network.server

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.database.repository.BatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DeviceRepository
import com.arverse.arbattery.core.database.repository.PairingRepository
import com.arverse.arbattery.core.network.dto.ApiResponseDto
import com.arverse.arbattery.core.network.dto.BatteryReportDto
import com.arverse.arbattery.core.network.dto.PairingConfirmDto
import com.arverse.arbattery.core.network.dto.PairingRequestDto
import com.arverse.arbattery.core.network.dto.PairingResponseDto
import com.arverse.arbattery.core.network.dto.SyncRequestDto
import com.arverse.arbattery.core.network.dto.SyncResponseDto
import com.arverse.arbattery.core.network.security.CryptoTokenManager
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed interface HubServerState {
    data object Stopped : HubServerState
    data class Running(val host: String, val port: Int) : HubServerState
    data class Error(val message: String) : HubServerState
}

class HubEmbeddedServer(
    private val deviceRepository: DeviceRepository,
    private val pairingRepository: PairingRepository,
    private val historyRepository: BatteryHistoryRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    private var engine: CIOApplicationEngine? = null

    private val _serverState = MutableStateFlow<HubServerState>(HubServerState.Stopped)
    val serverState: StateFlow<HubServerState> = _serverState.asStateFlow()

    suspend fun start(port: Int = 8443, host: String = "0.0.0.0") = withContext(dispatchers.io) {
        if (engine != null) return@withContext

        try {
            val server = embeddedServer(CIO, port = port, host = host) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }

                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponseDto<Unit>(success = false, message = cause.message ?: "Internal error")
                        )
                    }
                }

                routing {
                    route("/api/v1") {

                        get("/health") {
                            call.respond(HttpStatusCode.OK, mapOf("status" to "OK", "version" to "1.0.0"))
                        }

                        // PAIRING REQUEST (Step 1)
                        post("/pairing/request") {
                            val request = call.receive<PairingRequestDto>()
                            if (request.deviceId.isBlank() || request.deviceName.isBlank()) {
                                call.respond(HttpStatusCode.BadRequest, PairingResponseDto("REJECTED", "ArBattery Hub", null, "Invalid device info"))
                                return@post
                            }

                            // Generate new pairing token
                            val token = CryptoTokenManager.generateSecureToken()
                            val tokenHash = CryptoTokenManager.hashToken(token)

                            pairingRepository.savePairing(request.deviceId, tokenHash)

                            call.respond(
                                HttpStatusCode.OK,
                                PairingResponseDto(
                                    status = "ACCEPTED",
                                    hubName = "ArBattery Hub",
                                    authToken = token,
                                    message = "Pairing successful"
                                )
                            )
                        }

                        // PAIRING CONFIRM (Step 2)
                        post("/pairing/confirm") {
                            val confirm = call.receive<PairingConfirmDto>()
                            val tokenHash = CryptoTokenManager.hashToken(confirm.authToken)
                            val valid = pairingRepository.validateToken(confirm.deviceId, tokenHash)

                            if (valid) {
                                pairingRepository.updateLastSync(confirm.deviceId, System.currentTimeMillis())
                                call.respond(HttpStatusCode.OK, ApiResponseDto<Unit>(success = true, message = "Device verified"))
                            } else {
                                call.respond(HttpStatusCode.Unauthorized, ApiResponseDto<Unit>(success = false, message = "Invalid token"))
                            }
                        }

                        // RECEIVE BATTERY REPORT
                        post("/devices/battery") {
                            val authHeader = call.request.header("Authorization")
                            val token = authHeader?.removePrefix("Bearer ")?.trim()

                            if (token.isNullOrBlank()) {
                                call.respond(HttpStatusCode.Unauthorized, ApiResponseDto<Unit>(success = false, message = "Missing auth token"))
                                return@post
                            }

                            val report = call.receive<BatteryReportDto>()

                            // Validate device identity against pairing token
                            val tokenHash = CryptoTokenManager.hashToken(token)
                            val isPaired = pairingRepository.validateToken(report.deviceId, tokenHash)
                            if (!isPaired) {
                                call.respond(HttpStatusCode.Forbidden, ApiResponseDto<Unit>(success = false, message = "Device unauthenticated"))
                                return@post
                            }

                            // Save device battery
                            val domainModel = report.toDomainModel()
                            deviceRepository.updateBatteryReading(domainModel)

                            // Record history
                            historyRepository.recordReading(
                                deviceId = report.deviceId,
                                percentage = domainModel.batteryPercentage ?: 0,
                                chargingState = domainModel.chargingState,
                                source = "WIFI"
                            )

                            pairingRepository.updateLastSync(report.deviceId, System.currentTimeMillis())

                            call.respond(HttpStatusCode.OK, ApiResponseDto(success = true, data = "Battery updated"))
                        }

                        // BATCH SYNC
                        post("/sync") {
                            val authHeader = call.request.header("Authorization")
                            val token = authHeader?.removePrefix("Bearer ")?.trim()

                            if (token.isNullOrBlank()) {
                                call.respond(HttpStatusCode.Unauthorized, ApiResponseDto<Unit>(success = false, message = "Missing token"))
                                return@post
                            }

                            val syncReq = call.receive<SyncRequestDto>()
                            val tokenHash = CryptoTokenManager.hashToken(token)
                            val isPaired = pairingRepository.validateToken(syncReq.deviceId, tokenHash)

                            if (!isPaired) {
                                call.respond(HttpStatusCode.Forbidden, ApiResponseDto<Unit>(success = false, message = "Unauthenticated"))
                                return@post
                            }

                            var count = 0
                            for (rep in syncReq.reports) {
                                val model = rep.toDomainModel()
                                deviceRepository.updateBatteryReading(model)
                                historyRepository.recordReading(
                                    deviceId = rep.deviceId,
                                    percentage = model.batteryPercentage ?: 0,
                                    chargingState = model.chargingState,
                                    source = "SYNC"
                                )
                                count++
                            }

                            pairingRepository.updateLastSync(syncReq.deviceId, System.currentTimeMillis())

                            call.respond(HttpStatusCode.OK, SyncResponseDto(status = "SUCCESS", processedCount = count))
                        }

                        // GET ALL MONITORED DEVICES
                        get("/devices") {
                            val devices = deviceRepository.getAllDevices()
                            call.respond(HttpStatusCode.OK, devices)
                        }

                        // GET DEVICE BY ID
                        get("/devices/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponseDto<Unit>(success = false, message = "Missing ID"))
                                return@get
                            }
                            val device = deviceRepository.getDeviceById(id)
                            if (device != null) {
                                call.respond(HttpStatusCode.OK, device)
                            } else {
                                call.respond(HttpStatusCode.NotFound, ApiResponseDto<Unit>(success = false, message = "Not found"))
                            }
                        }

                        // DELETE DEVICE
                        delete("/devices/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponseDto<Unit>(success = false, message = "Missing ID"))
                                return@delete
                            }
                            deviceRepository.deleteDevice(id)
                            pairingRepository.deletePairing(id)
                            call.respond(HttpStatusCode.OK, ApiResponseDto<Unit>(success = true, message = "Device removed"))
                        }
                    }
                }
            }

            server.start(wait = false)
            engine = server
            _serverState.value = HubServerState.Running(host, port)
        } catch (e: Exception) {
            _serverState.value = HubServerState.Error(e.message ?: "Failed to start server")
        }
    }

    suspend fun stop() = withContext(dispatchers.io) {
        engine?.stop(1000, 2000)
        engine = null
        _serverState.value = HubServerState.Stopped
    }
}
