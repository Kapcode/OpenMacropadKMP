package com.kapcode.open.macropad.kmps

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.security.auth.x500.X500Principal

/**
 * Android implementation of IdentityManager using the Android Keystore System.
 * (Vulnerability Fix 3 & 4: Hardware-backed keys and migration from RSA to EC)
 */
actual class IdentityManager actual constructor() {

    actual companion object {
        private const val KEY_ALIAS = "OpenMacropadIdentity"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        actual fun verifySignature(message: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean {
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(message)
            return sig.verify(signature)
        }
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateIdentityKey(): KeyPair {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateIdentityKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    private fun generateIdentityKey() {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setDigests(KeyProperties.DIGEST_SHA256)
            setCertificateSubject(X500Principal("CN=OpenMacropad Device, OU=Kapcode"))
            build()
        }

        kpg.initialize(parameterSpec)
        kpg.generateKeyPair()
    }

    actual fun getIdentityPublicKey(): ByteArray {
        return getOrCreateIdentityKey().public.encoded
    }

    actual fun signMessage(message: ByteArray): ByteArray {
        val privateKey = getOrCreateIdentityKey().private
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(message)
        return signature.sign()
    }
}
