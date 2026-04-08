package com.kapcode.open.macropad.kmps.network.sockets.model

import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.math.BigInteger

/**
 * Manages encryption keys and secure key exchange between client and server
 * Uses Diffie-Hellman for key exchange
 */
class EncryptionManager {
    private var sharedKey: SecretKey? = null
    private var keyPair: KeyPair? = null
    private val keyAgreement: KeyAgreement = KeyAgreement.getInstance("DH")

    companion object {
        private const val KEY_SIZE = 2048 // Increased from 1024 to resist Logjam/discrete log attacks (Fix 2)
        private const val AES_KEY_SIZE = 256

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
            return withPreSharedKey(DataModel.stringToKey(keyString))
        }

        /**
         * Sign data using a private key (ECDSA)
         */
        fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(data)
            return signature.sign()
        }

        /**
         * Verify signature using a public key (ECDSA)
         */
        fun verifySignature(data: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data)
            return signature.verify(signatureBytes)
        }
    }

    /**
     * Initialize Diffie-Hellman key exchange.
     * If params are provided (client), use them.
     * If not (server), generate new ones.
     */
    fun initializeKeyExchange(spec: DHParameterSpec? = null): Pair<ByteArray, DHParameterSpec?> {
        val keyPairGenerator = KeyPairGenerator.getInstance("DH")
        val finalSpec = spec ?: run {
            keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
            val params = keyPairGenerator.generateKeyPair().public as javax.crypto.interfaces.DHPublicKey
            params.params
        }

        keyPairGenerator.initialize(finalSpec, SecureRandom())
        keyPair = keyPairGenerator.generateKeyPair()
        keyAgreement.init(keyPair!!.private)

        return keyPair!!.public.encoded to if (spec == null) finalSpec else null
    }

    /**
     * Complete the key exchange with the other party's public key
     * Call this after receiving the other party's public key
     */
    fun completeKeyExchange(otherPublicKeyBytes: ByteArray) {
        val keyFactory = KeyFactory.getInstance("DH")
        val x509KeySpec = X509EncodedKeySpec(otherPublicKeyBytes)
        val otherPublicKey = keyFactory.generatePublic(x509KeySpec)

        // Vulnerability Fix 5: Input Validation on DH Public Keys
        if (otherPublicKey is javax.crypto.interfaces.DHPublicKey) {
            val y = otherPublicKey.y
            val p = otherPublicKey.params.p
            // Check if y is in the range [2, p-2]
            if (y <= BigInteger.ONE || y >= p.subtract(BigInteger.ONE)) {
                throw InvalidKeyException("DH Public Key is out of valid range")
            }
        }

        keyAgreement.doPhase(otherPublicKey, true)

        // Generate shared secret
        val sharedSecret = keyAgreement.generateSecret()

        // Derive AES key from shared secret using SHA-256
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val keyBytes = messageDigest.digest(sharedSecret).copyOf(AES_KEY_SIZE / 8)

        sharedKey = SecretKeySpec(keyBytes, "AES")
        
        // Security: Zero out sensitive intermediate data to prevent persistence in RAM/ZRAM (Fix 6)
        sharedSecret.fill(0)
        keyBytes.fill(0)
    }

    /**
     * Encrypt a DataModel
     */
    fun encrypt(dataModel: DataModel): ByteArray {
        requireNotNull(sharedKey) { "Shared key not initialized. Call initializeKeyExchange() first." }
        return dataModel.toEncryptedBytes(sharedKey!!)
    }

    /**
     * Decrypt bytes to a DataModel
     */
    fun decrypt(encryptedData: ByteArray): DataModel {
        requireNotNull(sharedKey) { "Shared key not initialized. Call initializeKeyExchange() first." }
        return DataModel.fromEncryptedBytes(encryptedData, sharedKey!!)
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
