package com.kapcode.open.macropad.kmps.network.sockets.model

import com.kapcode.open.macropad.kmps.generateUuid
import com.kapcode.open.macropad.kmps.currentTimeMillis
import com.kapcode.open.macropad.kmps.models.AutomationRoutine
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Sealed class representing different types of messages that can be sent
 * between client and server
 */
@Serializable
sealed class MessageType {
    @Serializable
    @SerialName("text")
    data class Text(val content: String) : MessageType()
    
    @Serializable
    @SerialName("command")
    data class Command(
        val command: String,
        val parameters: Map<String, String> = emptyMap(),
    ) : MessageType()

    @Serializable
    @SerialName("automation_routine")
    data class AutomationRoutineMsg(val routine: AutomationRoutine) : MessageType()

    @Serializable
    @SerialName("layer_update")
    data class LayerUpdate(val activeLayerId: String) : MessageType()

    @Serializable
    @SerialName("system_query")
    data class SystemQuery(val query: String) : MessageType()
    
    @Serializable
    @SerialName("sync_state")
    data class SyncState(
        val activeRewardSessionId: String?,
        val expirationTimestamp: Long?,
        val isPremium: Boolean = false,
    ) : MessageType()

    @Serializable
    @SerialName("claim_session")
    data class ClaimSession(val deviceId: String) : MessageType()

    @Serializable
    @SerialName("session_claimed")
    data class SessionClaimed(
        val sessionId: String,
        val expirationTimestamp: Long,
    ) : MessageType()

    @Serializable
    @SerialName("data")
    data class Data(
        val key: String,
        val value: ByteArray,
    ) : MessageType() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Data &&
                key == other.key &&
                value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }
    
    @Serializable
    @SerialName("response")
    data class Response(
        val success: Boolean,
        val message: String,
        val data: String? = null,
    ) : MessageType()
    
    @Serializable
    @SerialName("control")
    data class Control(
        val command: ControlCommand,
        val parameters: Map<String, String> = emptyMap(),
    ) : MessageType()
    
    @Serializable
    @SerialName("heartbeat")
    data class Heartbeat(val timestamp: Long = currentTimeMillis()) : MessageType()
}

/**
 * Commands specifically for connection lifecycle and security
 */
@Serializable
enum class ControlCommand {
    PAIRING_REQUEST,
    PAIRING_PENDING,
    PAIRING_RESPONSE,
    PAIRING_CODE_MATCHED,
    PAIRING_APPROVED,
    PAIRING_REJECTED,
    AUTH_CHALLENGE,
    AUTH_RESPONSE,
    BANNED,
    DISCONNECT,
    EXECUTION_START,
    EXECUTION_COMPLETE,
    EXECUTION_FAILED,
    MARKETPLACE_LIST,
    SERVER_INFO,
}

/**
 * Main data model for encrypted communication between server and client
 */
@Serializable
data class DataModel(
    val id: String = generateUuid(),
    val timestamp: Long = currentTimeMillis(),
    val messageType: MessageType,
    val metadata: Map<String, String> = emptyMap(),
    val priority: Priority = Priority.NORMAL,
) {

    enum class Priority {
        LOW, NORMAL, HIGH, CRITICAL,
    }

    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * Deserialize a DataModel from bytes (JSON)
         */
        fun fromBytes(bytes: ByteArray): DataModel {
            return json.decodeFromString(bytes.decodeToString())
        }
    }

    /**
     * Serialize the DataModel to bytes (JSON)
     */
    fun toBytes(): ByteArray {
        return json.encodeToString(this).encodeToByteArray()
    }

    override fun toString(): String {
        return "DataModel(id=$id, timestamp=$timestamp, type=$messageType, priority=$priority)"
    }
}

/**
 * Builder class for creating DataModel instances
 */
class DataModelBuilder {
    private var messageType: MessageType? = null
    private var metadata: MutableMap<String, String> = mutableMapOf()
    private var priority: DataModel.Priority = DataModel.Priority.NORMAL
    private var id: String? = null
    private var timestamp: Long? = null

    fun text(content: String) = apply { this.messageType = MessageType.Text(content) }
    
    fun command(command: String, parameters: Map<String, String> = emptyMap()) = apply {
        this.messageType = MessageType.Command(command, parameters)
    }
    
    fun data(key: String, value: ByteArray) = apply {
        this.messageType = MessageType.Data(key, value)
    }
    
    fun response(success: Boolean, message: String, data: String? = null) = apply {
        this.messageType = MessageType.Response(success, message, data)
    }

    fun control(command: ControlCommand, parameters: Map<String, String> = emptyMap()) = apply {
        this.messageType = MessageType.Control(command, parameters)
    }

    fun heartbeat() = apply { this.messageType = MessageType.Heartbeat() }
    
    fun syncState(activeRewardSessionId: String?, expirationTimestamp: Long?, isPremium: Boolean = false) = apply {
        this.messageType = MessageType.SyncState(activeRewardSessionId, expirationTimestamp, isPremium)
    }

    fun claimSession(deviceId: String) = apply {
        this.messageType = MessageType.ClaimSession(deviceId)
    }

    fun sessionClaimed(sessionId: String, expirationTimestamp: Long) = apply {
        this.messageType = MessageType.SessionClaimed(sessionId, expirationTimestamp)
    }

    fun systemQuery(query: String) = apply { this.messageType = MessageType.SystemQuery(query) }

    fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }
    
    fun metadata(metadata: Map<String, String>) = apply { this.metadata.putAll(metadata) }
    
    fun priority(priority: DataModel.Priority) = apply { this.priority = priority }
    
    fun id(id: String) = apply { this.id = id }

    fun build(): DataModel {
        val type = messageType ?: throw IllegalStateException("MessageType must be set")
        return DataModel(
            id = id ?: generateUuid(),
            timestamp = timestamp ?: currentTimeMillis(),
            messageType = type,
            metadata = metadata,
            priority = priority
        )
    }
}
