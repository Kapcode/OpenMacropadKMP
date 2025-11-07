package com.kapcode.open.macropad.kmps

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.util.Date

class IdentityManager(private val context: Context) {

    private val keystoreFile = File(context.filesDir, "client_identity.bks")
    private val keystorePassword = "changeit".toCharArray()
    private val keyAlias = "clientkey"

    init {
        // Ensure BouncyCastle provider is present
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    fun getClientKeyStore(): KeyStore {
        return if (keystoreFile.exists()) {
            loadKeyStore()
        } else {
            createNewKeyStore()
        }
    }

    private fun loadKeyStore(): KeyStore {
        // THE FINAL FIX: Use the standard "BKS" format with the "BC" provider
        val keyStore = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME)
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

        // 2. Define the certificate's properties
        val subject = X500Name("CN=${android.os.Build.MODEL}, OU=OpenMacropad, O=Kapcode, C=US")
        val serial = BigInteger.valueOf(SecureRandom().nextLong())
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 100L * 365 * 24 * 60 * 60 * 1000) // 100 years

        // 3. Use BouncyCastle to build the self-signed certificate
        val certBuilder = JcaX509v3CertificateBuilder(subject, serial, notBefore, notAfter, subject, keyPair.public)
        val contentSigner = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))

        // 4. Create and save the KeyStore
        // THE FINAL FIX: Use the standard "BKS" format with the "BC" provider
        val keyStore = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME)
        keyStore.load(null, keystorePassword)
        keyStore.setKeyEntry(keyAlias, keyPair.private, keystorePassword, arrayOf(certificate))

        keystoreFile.outputStream().use {
            keyStore.store(it, keystorePassword)
        }
        return keyStore
    }
}
