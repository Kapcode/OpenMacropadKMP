package com.kapcode.open.macropad.kmps.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.security.auth.x500.X500Principal
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.spec.ECGenParameterSpec
import java.text.SimpleDateFormat

class KeystorePasswordException(message: String, cause: Throwable? = null) : Exception(message, cause)

object KeystoreUtils {
    private const val ALIAS = "open-macropad-server"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private fun getPassword(): CharArray {
        return SecretManager.getOrCreatePassword()
    }

    private fun clearPassword(password: CharArray) {
        password.fill('\u0000')
    }

    /**
     * Sets file permissions to 600 (Owner Read/Write only) on POSIX systems.
     */
    fun setSecurePermissions(file: File) {
        try {
            val path = file.toPath()
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                val permissions = setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                )
                Files.setPosixFilePermissions(path, permissions)
            }
        } catch (e: Exception) {
            println("Warning: Could not set secure permissions for ${file.name}: ${e.message}")
        }
    }

    fun getOrCreateKeystore(workingDir: File, forceRecreate: Boolean = false): KeyStore {
        val keystoreFile = File(workingDir, "server_keystore.p12")
        val keyStore = KeyStore.getInstance("PKCS12")

        if (keystoreFile.exists() && !forceRecreate) {
            val password = getPassword()
            try {
                keystoreFile.inputStream().use { 
                    keyStore.load(it, password) 
                }
                setSecurePermissions(keystoreFile)
                return keyStore
            } catch (e: Exception) {
                throw KeystorePasswordException("Failed to load keystore (possibly wrong password).", e)
            } finally {
                clearPassword(password)
            }
        }

        if (forceRecreate && keystoreFile.exists()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(workingDir, "server_keystore.p12.$timestamp.bak")
            keystoreFile.renameTo(backupFile)
            println("Old keystore backed up to: ${backupFile.name}")
        }

        // Generate new keystore
        keyStore.load(null, null)
        val keyPair = generateKeyPair()
        val certificate = generateCertificate(keyPair)
        val password = getPassword()

        try {
            keyStore.setKeyEntry(
                ALIAS,
                keyPair.private,
                password,
                arrayOf(certificate)
            )

            FileOutputStream(keystoreFile).use {
                keyStore.store(it, password)
            }
        } finally {
            clearPassword(password)
        }
        
        setSecurePermissions(keystoreFile)

        return keyStore
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        return keyPairGenerator.generateKeyPair()
    }

    private fun generateCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val expiry = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 year
        val serialNumber = BigInteger(64, SecureRandom())
        
        val subject = X500Name("CN=OpenMacropadServer")
        val builder = JcaX509v3CertificateBuilder(
            subject,
            serialNumber,
            now,
            expiry,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }
}
