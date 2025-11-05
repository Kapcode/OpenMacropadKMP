package com.kapcode.open.macropad.kmp.network.ktor

import com.kapcode.open.macropad.kmp.network.sockets.Model.DataModel
import java.security.*
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.*

/**
 * Manages encryption keys and secure key exchange between client and server
 * Uses Diffie-Hellman for key exchange and AES/GCM for data encryption.
 */
class EncryptionManager {
    private var sharedKey: SecretKey? = null
    private var keyPair: KeyPair? = null
    private val keyAgreement: KeyAgreement = KeyAgreement.getInstance("DH")

    companion object {
        private const val KEY_SIZE = 2048
        private const val AES_KEY_SIZE = 256

        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        /**
         * Create a simple encryption manager with a pre-shared key
         * Useful for testing or when you have a secure way to share keys
         */
        fun withPreSharedKey(key: SecretKey): EncryptionManager {
            return EncryptionManager().apply {
                sharedKey = key
            }
        }

        /**
         * Create a simple encryption manager with a pre-shared key string
         */
        fun withPreSharedKey(keyString: String): EncryptionManager {
            // DataModel.stringToKey is still useful here to convert string to SecretKey
            return withPreSharedKey(DataModel.stringToKey(keyString))
        }
    }

    /**
     * Initialize Diffie-Hellman key exchange
     * Call this on both client and server before exchanging keys
     */
    fun initializeKeyExchange(): ByteArray {
        val keyPairGenerator = KeyPairGenerator.getInstance("DH")
        keyPairGenerator.initialize(KEY_SIZE)
        keyPair = keyPairGenerator.generateKeyPair()
        keyAgreement.init(keyPair!!.private)

        return keyPair!!.public.encoded
    }

    /**
     * Complete the key exchange with the other party's public key
     * Call this after receiving the other party's public key
     */
    fun completeKeyExchange(otherPublicKeyBytes: ByteArray) {
        val keyFactory = KeyFactory.getInstance("DH")
        val x509KeySpec = java.security.spec.X509EncodedKeySpec(otherPublicKeyBytes)
        val otherPublicKey = keyFactory.generatePublic(x509KeySpec)

        keyAgreement.doPhase(otherPublicKey, true)

        // Generate shared secret
        val sharedSecret = keyAgreement.generateSecret()

        // Derive AES key from shared secret using SHA-256
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val keyBytes = messageDigest.digest(sharedSecret).copyOf(AES_KEY_SIZE / 8)

        sharedKey = SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypt data using AES-GCM
     * Returns: IV (12 bytes) + Ciphertext (with authentication tag)
     */
    fun encrypt(data: ByteArray): ByteArray {
        requireNotNull(sharedKey) { "Shared key not initialized. Call initializeKeyExchange() first." }

        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey, gcmSpec)

        val ciphertext = cipher.doFinal(data)

        // Prepend IV to ciphertext
        return iv + ciphertext
    }

    /**
     * Decrypt data using AES-GCM
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        requireNotNull(sharedKey) { "Shared key not initialized. Call initializeKeyExchange() first." }

        // Extract IV from the beginning of encrypted data
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, sharedKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Get the current shared key (for backup/storage purposes)
     */
    fun getSharedKey(): SecretKey {
        requireNotNull(sharedKey) { "Shared key not initialized." }
        return sharedKey!!
    }

    /**
     * Get the shared key as a Base64 string
     */
    fun getSharedKeyString(): String {
        return DataModel.keyToString(getSharedKey())
    }

    /**
     * Check if encryption is ready
     */
    fun isReady(): Boolean = sharedKey != null

    /**
     * Clear all keys (for security purposes)
     */
    fun clear() {
        sharedKey = null
        keyPair = null
    }
}
