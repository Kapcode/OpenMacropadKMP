package com.kapcode.open.macropad.kmps

/**
 * Platform-agnostic identity manager for cryptographic signatures.
 * (Vulnerability Fix 3 & 4: Hardware-backed keys and migration from RSA to EC)
 */
expect class IdentityManager() {
    /**
     * Returns the public key encoded as a byte array (X.509).
     */
    fun getIdentityPublicKey(): ByteArray

    /**
     * Signs the given message using the device's private identity key.
     */
    fun signMessage(message: ByteArray): ByteArray
    
    companion object {
        /**
         * Verifies a signature against a public key.
         */
        fun verifySignature(message: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean
    }
}
