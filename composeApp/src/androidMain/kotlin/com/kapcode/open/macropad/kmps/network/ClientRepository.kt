package com.kapcode.open.macropad.kmps.network

import com.kapcode.open.macropad.kmps.network.sockets.MacroKtorClient
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.kapcode.open.macropad.kmps.ServerStorage
import com.kapcode.open.macropad.kmps.TokenManager
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.models.MarketplaceItem
import com.kapcode.open.macropad.kmps.network.sockets.model.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException

class ClientRepository(private val context: Context) {
    private var client: MacroKtorClient? = null
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun connect(
        ipAddress: String,
        port: Int,
        deviceName: String,
        isSecure: Boolean,
        discoveryFingerprint: String?,
        serverName: String? = null,
        onUpdate: (status: String, serverName: String?, reason: String?, verificationCode: String?) -> Unit,
        onMacrosReceived: (List<String>) -> Unit,
        onActiveProcessChanged: (String?) -> Unit,
        onCurrencyUpdate: (Long) -> Unit,
        onExecutionStart: (String) -> Unit,
        onExecutionComplete: (String) -> Unit,
        onExecutionFailed: (String, String) -> Unit,
        onPacksReceived: (List<MacroPack>) -> Unit,
        onMarketplaceItemsReceived: (List<MarketplaceItem>) -> Unit,
        onNotificationReceived: (String) -> Unit
    ) {
        clientJob?.cancel()
        Log.i("ClientRepository", "Previous connection job cancelled. Starting new connection to $ipAddress")
        clientJob = scope.launch {
            var backoffMillis = 1000L
            val maxBackoffMillis = 16000L
            var retryCount = 0
            val maxRetries = 5
            val initialServerName = serverName

            while (isActive) {
                var tempClient: MacroKtorClient? = null
                try {
                    val ktorHttpClient = HttpClient(OkHttp) {
                        install(WebSockets)
                        engine {
                            if (isSecure) {
                                val savedFingerprint = ServerStorage.getServerFingerprint(context, "$ipAddress:$port")
                                    ?: discoveryFingerprint

                                if (savedFingerprint != null) {
                                    preconfigured = createPinnedOkHttpClient(savedFingerprint)
                                } else {
                                    preconfigured = createUnsafeOkHttpClient()
                                }
                            }
                        }
                    }

                    tempClient = MacroKtorClient(ktorHttpClient, ipAddress, port, isSecure)
                    this@ClientRepository.client = tempClient

                    Log.i("ClientRepository", "Starting connection to $ipAddress:$port")
                    onUpdate("Connecting...", initialServerName ?: ipAddress, null, null)
                    withContext(Dispatchers.IO) {
                        tempClient.connect(deviceName)
                    }

                    Log.i("ClientRepository", "WebSocket established. Waiting for Auth/Pairing...")
                    onUpdate("Authenticating...", initialServerName ?: ipAddress, null, null)

                    backoffMillis = 1000L
                    retryCount = 0
                    var lastHeartbeat = System.currentTimeMillis()
                    var currentVerificationCode: String? = null

                    val tokenManager = TokenManager.getInstance(context)
                    tempClient.send(dataMessage("currency_update", tokenManager.tokenBalance.value.toLong().toString().encodeToByteArray()).toBytes())

                    val macroFetchJob = launch {
                        while (isActive) {
                            tempClient.send(getMacrosRequest().toBytes())
                            delay(5000)
                        }
                    }

                    val watchdogJob = launch {
                        while (isActive) {
                            delay(5000)
                            if (System.currentTimeMillis() - lastHeartbeat > 40000) {
                                Log.w("ClientRepository", "Heartbeat timeout! Reconnecting...")
                                tempClient.close()
                                this@launch.cancel()
                            }
                        }
                    }

                    Log.i("ClientRepository", "Starting message collection...")
                    tempClient.incomingMessages.receiveAsFlow().collect { frame ->
                        if (frame is Frame.Binary) {
                            try {
                                lastHeartbeat = System.currentTimeMillis()
                                val bytes = frame.readBytes()
                                val dataModel = DataModel.fromBytes(bytes)
                                Log.d("ClientRepository", "Received message: ${dataModel.messageType}")
                                
                                dataModel.handle(
                                    onControl = { command, params ->
                                        Log.i("ClientRepository", "Control command: $command")
                                        when (command) {
                                            ControlCommand.AUTH_CHALLENGE -> {
                                                val fingerprint = params["fingerprint"]
                                                if (fingerprint != null) {
                                                    ServerStorage.saveServerFingerprint(context, "$ipAddress:$port", fingerprint)
                                                }
                                            }
                                            ControlCommand.PAIRING_PENDING -> {
                                                Log.i("ClientRepository", "Pairing pending. Resetting macros.")
                                                onMacrosReceived(emptyList())
                                                val code = params["code"]
                                                currentVerificationCode = code
                                                onUpdate("Pending Approval", initialServerName, null, code)
                                            }
                                            ControlCommand.PAIRING_CODE_MATCHED -> {
                                                Log.i("ClientRepository", "Pairing code matched.")
                                                onUpdate("Code Matched", initialServerName, null, currentVerificationCode)
                                            }
                                            ControlCommand.PAIRING_APPROVED -> {
                                                Log.i("ClientRepository", "Pairing approved!")
                                                onUpdate("Connected", initialServerName ?: ipAddress, null, null)
                                                
                                                // Trigger macro fetch immediately upon approval
                                                scope.launch {
                                                    delay(100) // Small delay to let server state settle
                                                    Log.d("ClientRepository", "Requesting macros after approval/auth")
                                                    this@ClientRepository.client?.send(textMessage("getMacros").toBytes())
                                                }
                                            }
                                            ControlCommand.PAIRING_REJECTED -> {
                                                onMacrosReceived(emptyList())
                                                onUpdate("Pairing Denied", initialServerName, params["reason"] ?: "Server rejected pairing.", null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.BANNED -> {
                                                onMacrosReceived(emptyList())
                                                val reason = params["reason"] ?: "Device is banned"
                                                onUpdate("Banned", initialServerName, reason, null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.MARKETPLACE_LIST -> {
                                                // We'll handle this in onData for now since it might be a large JSON blob
                                            }
                                            ControlCommand.DISCONNECT -> {
                                                onMacrosReceived(emptyList())
                                                onUpdate("Disconnected", initialServerName, params["reason"] ?: "Disconnected by Server.", null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.EXECUTION_START -> {
                                                params["macro"]?.let { onExecutionStart(it) }
                                            }
                                            ControlCommand.EXECUTION_COMPLETE -> {
                                                params["macro"]?.let { onExecutionComplete(it) }
                                            }
                                            ControlCommand.EXECUTION_FAILED -> {
                                                val macro = params["macro"] ?: "Unknown"
                                                val error = params["error"] ?: "Unknown error"
                                                onExecutionFailed(macro, error)
                                            }
                                            ControlCommand.SERVER_INFO -> {
                                                val version = params["version"] ?: "Unknown"
                                                val platform = params["platform"] ?: "Unknown"
                                                Log.i("ClientRepository", "Connected to Server $version on $platform")
                                            }
                                            else -> {}
                                        }
                                    },
                                    onText = { text ->
                                        Log.d("ClientRepository", "Text message received: ${text.take(50)}")
                                        if (dataModel.metadata["type"] == "toast") {
                                            onNotificationReceived(text)
                                        } else if (text.startsWith("macros:")) {
                                            val macroNames = text.substringAfter("macros:").split(",").filter { it.isNotBlank() }
                                            Log.i("ClientRepository", "Received ${macroNames.size} macros. Marking as Connected.")
                                            
                                            // 1. Reset disconnect state if macros are found
                                            // 2. Set connected status
                                            // 3. Notify UI
                                            onUpdate("Connected", initialServerName ?: ipAddress, null, null)
                                            onMacrosReceived(macroNames)
                                            macroFetchJob.cancel()
                                        }
                                    },
                                    onHeartbeat = {
                                        Log.v("ClientRepository", "Heartbeat received")
                                        lastHeartbeat = System.currentTimeMillis()
                                    },
                                    onCommand = { command, params ->
                                        Log.d("ClientRepository", "Command received: $command")
                                        if (command == "active_process") {
                                            onActiveProcessChanged(params["name"])
                                        }
                                    },
                                    onData = { key, value ->
                                        Log.d("ClientRepository", "Data received: $key")
                                        when (key) {
                                            "currency_update" -> {
                                                try {
                                                    val balance = value.decodeToString().toLong()
                                                    onCurrencyUpdate(balance)
                                                } catch (e: Exception) {
                                                    Log.e("ClientRepository", "Failed to parse currency_update", e)
                                                }
                                            }
                                            "marketplace_items" -> {
                                                try {
                                                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                                    val items = json.decodeFromString<List<MarketplaceItem>>(value.decodeToString())
                                                    onMarketplaceItemsReceived(items)
                                                } catch (e: Exception) {
                                                    Log.e("ClientRepository", "Failed to parse marketplace_items", e)
                                                }
                                            }
                                            "installed_packs" -> {
                                                try {
                                                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                                    val packs = json.decodeFromString<List<MacroPack>>(value.decodeToString())
                                                    onPacksReceived(packs)
                                                } catch (e: Exception) {
                                                    Log.e("ClientRepository", "Failed to parse installed_packs", e)
                                                }
                                            }
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("ClientRepository", "Error parsing DataModel", e)
                            }
                        }
                    }
                    watchdogJob.cancel()
                    macroFetchJob.cancel()

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i("ClientRepository", "Connection job cancelled.")
                        throw e
                    }
                    Log.e("ClientRepository", "Connection error: ${e.message}", e)
                    retryCount++
                    if (retryCount > maxRetries) {
                        onUpdate("Failed", null, "Max retries reached: ${e.message}", null)
                        this@launch.cancel()
                        return@launch
                    }
                    onUpdate("Connecting...", null, "Retrying ($retryCount/$maxRetries)...", null)
                } finally {
                    Log.i("ClientRepository", "Cleaning up connection...")
                    tempClient?.close()
                    this@ClientRepository.client = null
                }

                delay(backoffMillis)
                backoffMillis = (backoffMillis * 2).coerceAtMost(maxBackoffMillis)
            }
        }
    }

    fun disconnect() {
        clientJob?.cancel()
        client?.close()
        client = null
    }

    fun sendMacro(macroName: String) {
        scope.launch {
            client?.send(commandMessage("play:$macroName").toBytes())
        }
    }

    fun submitPairingCode(code: String) {
        scope.launch {
            val msg = controlMessage(ControlCommand.PAIRING_RESPONSE, mapOf("code" to code))
            client?.send(msg.toBytes())
        }
    }

    fun requestMacros() {
        scope.launch {
            client?.send(getMacrosRequest().toBytes())
        }
    }

    fun requestMarketplace() {
        scope.launch {
            client?.send(commandMessage("getMarketplace").toBytes())
        }
    }

    fun downloadMarketplaceItem(itemId: String) {
        scope.launch {
            client?.send(commandMessage("downloadMarketplaceItem", mapOf("id" to itemId)).toBytes())
        }
    }

    fun upgradeServer(jarBytes: ByteArray) {
        scope.launch(Dispatchers.Default) {
            val hash = calculateHash(jarBytes)
            client?.send(upgradeServerMessage(jarBytes, hash).toBytes())
        }
    }

    fun sendTestUpgrade() {
        scope.launch(Dispatchers.Default) {
            val dummyData = "This is dummy upgrade data for testing".encodeToByteArray()
            val hash = calculateHash(dummyData)
            client?.send(testUpgradeMessage(dummyData, hash).toBytes())
        }
    }

    private fun calculateHash(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun sendData(key: String, value: String) {
        scope.launch {
            client?.send(dataMessage(key, value.encodeToByteArray()).toBytes())
        }
    }

    private fun createPinnedOkHttpClient(expectedFingerprint: String): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain == null || chain.isEmpty()) throw java.security.cert.CertificateException("Empty certificate chain")
                val cert = chain[0]
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val fingerprint = digest.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
                if (fingerprint != expectedFingerprint) {
                    throw java.security.cert.CertificateException("Certificate fingerprint mismatch!")
                }
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<X509TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0])
            .hostnameVerifier { _, _ -> true }.build()
    }
}
