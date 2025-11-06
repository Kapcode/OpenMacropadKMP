package Model

import java.io.*
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.*

/**
 * Sealed class representing different types of messages that can be sent
 * between client and server
 */


/**
 * Extension functions and convenience methods for DataModel
 */

// Quick builders for common message types


sealed class MessageType : Serializable {
    data class Text(val content: String) : MessageType()
    data class Command(val command: String, val parameters: Map<String, String> = emptyMap()) : MessageType()
    data class Data(val key: String, val value: ByteArray) : MessageType() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Data) return false
            if (key != other.key) return false
            if (!value.contentEquals(other.value)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }
    data class Response(val success: Boolean, val message: String, val data: Any? = null) : MessageType()
    data class Heartbeat(val timestamp: Long = System.currentTimeMillis()) : MessageType()
}

/**
 * Main data model for encrypted communication between server and client
 */
data class DataModel(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType,
    val metadata: Map<String, String> = emptyMap(),
    val priority: Priority = Priority.NORMAL
) : Serializable {

    enum class Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        /**
         * Generate a new AES key for encryption
         */
        fun generateKey(keySize: Int = 256): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(keySize)
            return keyGenerator.generateKey()
        }

        /**
         * Convert a key to Base64 string for storage/transmission
         */
        fun keyToString(key: SecretKey): String {
            return Base64.getEncoder().encodeToString(key.encoded)
        }

        /**
         * Restore a key from Base64 string
         */
        fun stringToKey(keyString: String): SecretKey {
            val keyBytes = Base64.getDecoder().decode(keyString)
            return SecretKeySpec(keyBytes, ALGORITHM)
        }

        /**
         * Deserialize an encrypted DataModel
         */
        fun fromEncryptedBytes(encryptedData: ByteArray, key: SecretKey): DataModel {
            val decryptedBytes = decrypt(encryptedData, key)
            return fromBytes(decryptedBytes)
        }

        /**
         * Deserialize a DataModel from bytes
         */
        fun fromBytes(bytes: ByteArray): DataModel {
            ByteArrayInputStream(bytes).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    return ois.readObject() as DataModel
                }
            }
        }

        /**
         * Decrypt data using AES-GCM
         */
        private fun decrypt(encryptedData: ByteArray, key: SecretKey): ByteArray {
            // Extract IV from the beginning of encrypted data
            val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            return cipher.doFinal(ciphertext)
        }
    }

    /**
     * Serialize the DataModel to bytes
     */
    fun toBytes(): ByteArray {
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(this)
                oos.flush()
                return baos.toByteArray()
            }
        }
    }

    /**
     * Encrypt and serialize the DataModel for transmission
     */
    fun toEncryptedBytes(key: SecretKey): ByteArray {
        val dataBytes = toBytes()
        return encrypt(dataBytes, key)
    }

    /**
     * Encrypt data using AES-GCM
     * Returns: IV (12 bytes) + Ciphertext (with authentication tag)
     */
    private fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val ciphertext = cipher.doFinal(data)

        // Prepend IV to ciphertext
        return iv + ciphertext
    }

    /**
     * Create a response DataModel based on this message
     */
    fun createResponse(success: Boolean, message: String, data: Any? = null): DataModel {
        return DataModel(
            messageType = MessageType.Response(success, message, data),
            metadata = mapOf("responseToId" to id)
        )
    }

    override fun toString(): String {
        return "DataModel(id=$id, timestamp=$timestamp, type=${messageType::class.simpleName}, priority=$priority)"
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
    
    fun response(success: Boolean, message: String, data: Any? = null) = apply {
        this.messageType = MessageType.Response(success, message, data)
    }
    
    fun heartbeat() = apply { this.messageType = MessageType.Heartbeat() }
    
    fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }
    
    fun metadata(metadata: Map<String, String>) = apply { this.metadata.putAll(metadata) }
    
    fun priority(priority: DataModel.Priority) = apply { this.priority = priority }
    
    fun id(id: String) = apply { this.id = id }
    
    fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }

    fun build(): DataModel {
        require(messageType != null) { "MessageType must be set" }
        return DataModel(
            id = id ?: UUID.randomUUID().toString(),
            timestamp = timestamp ?: System.currentTimeMillis(),
            messageType = messageType!!,
            metadata = metadata,
            priority = priority
        )
    }

}


