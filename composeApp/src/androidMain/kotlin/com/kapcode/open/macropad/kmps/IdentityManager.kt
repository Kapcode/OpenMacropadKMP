package com.kapcode.open.macropad.kmps

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.util.Date

/**
 * Manages the cryptographic identity of the Android client.
 *
 * This class creates and loads a JKS (Java KeyStore) file stored in the app's private
 * directory. This provides a stable, long-term identity for the client, which is
 * essential for a pairing-based security model (mTLS).
 */
class IdentityManager(private val context: Context) {

    private val keystoreFile = File(context.filesDir, "client_identity.jks")
    private val keystorePassword = "changeit".toCharArray() // In a real app, this should be stored securely
    private val keyAlias = "clientkey"

    init {
        // Add BouncyCastle as a security provider if it's not already there.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Gets the client's KeyStore.
     * If the keystore file doesn't exist, a new one is created.
     * Otherwise, the existing one is loaded.
     */
    fun getClientKeyStore(): KeyStore {
        return if (keystoreFile.exists()) {
            loadKeyStore()
        } else {
            createNewKeyStore()
        }
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("JKS")
        keystoreFile.inputStream().use {
            keyStore.load(it, keystorePassword)
        }
        return keyStore
    }

    private fun createNewKeyStore(): KeyStore {
        // 1. Generate a new RSA key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        // 2. Define the certificate's properties (subject, issuer, validity)
        val subject = X500Name("CN=${android.os.Build.MODEL}, OU=OpenMacropad, O=Kapcode, C=US")
        val serial = BigInteger.valueOf(SecureRandom().nextLong())
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 100L * 365 * 24 * 60 * 60 * 1000) // 100 years

        // 3. Use BouncyCastle to build the self-signed certificate
        val certBuilder = JcaX509v3CertificateBuilder(subject, serial, notBefore, notAfter, subject, keyPair.public)
        val contentSigner = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(contentSigner))

        // 4. Create a new KeyStore and add the private key and certificate
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(null, keystorePassword)
        keyStore.setKeyEntry(keyAlias, keyPair.private, keystorePassword, arrayOf(certificate))

        // 5. Save the new keystore to a file in the app's private storage
        keystoreFile.outputStream().use {
            keyStore.store(it, keystorePassword)
        }
        return keyStore
    }
}
