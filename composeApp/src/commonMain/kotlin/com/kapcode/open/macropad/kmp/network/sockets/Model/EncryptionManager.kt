package Model

import java.math.BigInteger
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages encryption keys and secure key exchange between client and server
 * Uses Diffie-Hellman for key exchange
 */
class EncryptionManager {
    private var sharedKey: SecretKey? = null
    private var keyPair: KeyPair? = null
    private val keyAgreement: KeyAgreement = KeyAgreement.getInstance("DH")

    companion object {
        private const val KEY_SIZE = 2048
        private const val AES_KEY_SIZE = 256

        // Standard Diffie-Hellman parameters (RFC 5114, 2048-bit MODP Group)
        // Using these pre-defined parameters ensures both client and server use the same DH parameters.
        private val DH_P = BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9F81F260E9195F119E457582B6B58F48DBF32733A030000000000000000000000000000000000000000000000000000000000000000", 16)
        private val DH_G = BigInteger("2")
        private val DH_PARAMETER_SPEC = DHParameterSpec(DH_P, DH_G)

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
    }

    /**
     * Initialize Diffie-Hellman key exchange
     * Call this on both client and server before exchanging keys
     */
    fun initializeKeyExchange(): ByteArray {
        val keyPairGenerator = KeyPairGenerator.getInstance("DH")
        keyPairGenerator.initialize(DH_PARAMETER_SPEC, SecureRandom()) // Initialize with fixed parameters and a secure random source
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
        val x509KeySpec = X509EncodedKeySpec(otherPublicKeyBytes)
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