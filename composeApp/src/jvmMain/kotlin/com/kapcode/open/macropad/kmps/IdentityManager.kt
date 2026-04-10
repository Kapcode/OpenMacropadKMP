package com.kapcode.open.macropad.kmps

import com.kapcode.open.macropad.kmps.utils.KeystoreUtils
import com.kapcode.open.macropad.kmps.utils.SecretManager
import java.io.File
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * JVM implementation of IdentityManager.
 * Stores a persistent identity key for the server in a password-protected Keystore.
 * (Vulnerability Fix 3 & 4: Migration from RSA to EC and encrypted storage)
 */
actual class IdentityManager actual constructor() {

    private val workingDir = File(System.getProperty("user.home"), ".openmacropad")
    private val keyStore: KeyStore

    init {
        if (!workingDir.exists()) workingDir.mkdirs()
        
        var ks: KeyStore
        try {
            ks = KeystoreUtils.getOrCreateKeystore(workingDir)
        } catch (e: Exception) {
            // During startup, if we can't load the identity, we initialize an empty one.
            // The DesktopViewModel will catch the error later when trying to start the server.
            ks = KeyStore.getInstance("PKCS12")
            ks.load(null, null)
        }
        keyStore = ks
    }

    actual companion object {
        private const val ALIAS = "open-macropad-identity" 
        private const val ALGORITHM = "EC"

        actual fun verifySignature(message: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean {
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(message)
            return sig.verify(signature)
        }
    }

    private fun getPassword(): CharArray {
        return SecretManager.getOrCreatePassword()
    }

    private fun clearPassword(password: CharArray) {
        password.fill('\u0000')
    }

    private fun getOrCreateIdentityKey(): KeyPair {
        val aliasToUse = if (keyStore.aliases().hasMoreElements()) {
            keyStore.aliases().nextElement()
        } else {
            throw IllegalStateException("Keystore is empty or not loaded.")
        }

        val password = getPassword()
        try {
            val privateKey = keyStore.getKey(aliasToUse, password) as PrivateKey
            val publicKey = keyStore.getCertificate(aliasToUse).publicKey
            return KeyPair(publicKey, privateKey)
        } finally {
            clearPassword(password)
        }
    }

    actual fun getIdentityPublicKey(): ByteArray {
        return getOrCreateIdentityKey().public.encoded
    }

    actual fun signMessage(message: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(getOrCreateIdentityKey().private)
        signature.update(message)
        return signature.sign()
    }
}
