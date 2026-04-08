package com.kapcode.open.macropad.kmps.network.sockets.model

import com.kapcode.open.macropad.kmps.IdentityManager
import java.io.*
import java.math.BigInteger
import java.net.Socket
import java.security.SignatureException
import javax.crypto.spec.DHParameterSpec

/**
 * Wrapper around Socket that handles encrypted DataModel transmission
 */
class SecureSocket(
    private val socket: Socket,
    private val encryptionManager: EncryptionManager,
    val peerIdentityKey: ByteArray? = null
) : Closeable {

    private val outputStream: DataOutputStream = DataOutputStream(socket.getOutputStream())
    private val inputStream: DataInputStream = DataInputStream(socket.getInputStream())

    companion object {
        /**
         * Perform authenticated key exchange and create a SecureSocket
         * Call this on the CLIENT side
         * @return A Pair containing the SecureSocket and the server's device name.
         */
        fun clientHandshake(socket: Socket, clientName: String): Pair<SecureSocket, String> {
            val encryptionManager = EncryptionManager()
            val identityManager = IdentityManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // 1. Receive server's DH public key, DH parameters, and name
            val theirKeySize = input.readInt()
            val theirPublicKeyBytes = ByteArray(theirKeySize)
            input.readFully(theirPublicKeyBytes)

            val pSize = input.readInt()
            val pBytes = ByteArray(pSize)
            input.readFully(pBytes)
            val p = BigInteger(pBytes)

            val gSize = input.readInt()
            val gBytes = ByteArray(gSize)
            input.readFully(gBytes)
            val g = BigInteger(gBytes)

            val serverName = input.readUTF()

            // 2. Receive server's identity public key and signature
            val serverIdentityKeySize = input.readInt()
            val serverIdentityKeyBytes = ByteArray(serverIdentityKeySize)
            input.readFully(serverIdentityKeyBytes)

            val signatureSize = input.readInt()
            val signatureBytes = ByteArray(signatureSize)
            input.readFully(signatureBytes)

            // 3. Verify server's signature (Fix 1: Unauthenticated Key Exchange)
            val dataToVerify = theirPublicKeyBytes + pBytes + gBytes + serverName.toByteArray()
            if (!IdentityManager.verifySignature(dataToVerify, signatureBytes, serverIdentityKeyBytes)) {
                throw SignatureException("Server identity verification failed!")
            }

            // 4. Initialize our key exchange with server's parameters
            val serverSpec = DHParameterSpec(p, g)
            val (ourPublicKeyBytes, _) = encryptionManager.initializeKeyExchange(serverSpec)
            
            // 5. Sign our public key and name
            val ourDataToSign = ourPublicKeyBytes + clientName.toByteArray()
            val ourSignature = identityManager.signMessage(ourDataToSign)
            val ourIdentityKey = identityManager.getIdentityPublicKey()

            // 6. Send our DH public key, name, identity key, and signature
            output.writeInt(ourPublicKeyBytes.size)
            output.write(ourPublicKeyBytes)
            output.writeUTF(clientName)
            output.writeInt(ourIdentityKey.size)
            output.write(ourIdentityKey)
            output.writeInt(ourSignature.size)
            output.write(ourSignature)
            output.flush()
            
            // 7. Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKeyBytes)

            return SecureSocket(socket, encryptionManager, serverIdentityKeyBytes) to serverName
        }

        /**
         * Perform authenticated key exchange and create a SecureSocket
         * Call this on the SERVER side
         * @return A Pair containing the SecureSocket and the client's device name.
         */
        fun serverHandshake(socket: Socket, serverName: String): Pair<SecureSocket, String> {
            val encryptionManager = EncryptionManager()
            val identityManager = IdentityManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // 1. Initialize our key exchange and get our parameters
            val (ourPublicKeyBytes, ourParams) = encryptionManager.initializeKeyExchange()
            val p = ourParams!!.p
            val g = ourParams!!.g
            val pBytes = p.toByteArray()
            val gBytes = g.toByteArray()

            // 2. Sign our DH public key, parameters, and name
            val dataToSign = ourPublicKeyBytes + pBytes + gBytes + serverName.toByteArray()
            val signature = identityManager.signMessage(dataToSign)
            val identityKey = identityManager.getIdentityPublicKey()

            // 3. Send our DH public key, DH parameters, name, identity key, and signature
            output.writeInt(ourPublicKeyBytes.size)
            output.write(ourPublicKeyBytes)
            output.writeInt(pBytes.size)
            output.write(pBytes)
            output.writeInt(gBytes.size)
            output.write(gBytes)
            output.writeUTF(serverName)
            output.writeInt(identityKey.size)
            output.write(identityKey)
            output.writeInt(signature.size)
            output.write(signature)
            output.flush()

            // 4. Receive client's DH public key and name
            val theirKeySize = input.readInt()
            val theirPublicKeyBytes = ByteArray(theirKeySize)
            input.readFully(theirPublicKeyBytes)
            val clientName = input.readUTF()

            // 5. Receive client's identity key and signature
            val clientIdentityKeySize = input.readInt()
            val clientIdentityKeyBytes = ByteArray(clientIdentityKeySize)
            input.readFully(clientIdentityKeyBytes)

            val theirSignatureSize = input.readInt()
            val theirSignatureBytes = ByteArray(theirSignatureSize)
            input.readFully(theirSignatureBytes)

            // 6. Verify client's signature (Fix 1: Unauthenticated Key Exchange)
            val theirDataToVerify = theirPublicKeyBytes + clientName.toByteArray()
            if (!IdentityManager.verifySignature(theirDataToVerify, theirSignatureBytes, clientIdentityKeyBytes)) {
                throw SignatureException("Client identity verification failed!")
            }

            // 7. Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKeyBytes)

            return SecureSocket(socket, encryptionManager, clientIdentityKeyBytes) to clientName
        }

        /**
         * Create a SecureSocket with a pre-shared key (skips handshake)
         * Useful for testing or when you have a secure way to share keys
         */
        fun withPreSharedKey(socket: Socket, key: String): SecureSocket {
            val encryptionManager = EncryptionManager.withPreSharedKey(key)
            return SecureSocket(socket, encryptionManager)
        }
    }

    /**
     * Send a DataModel through the socket (automatically encrypted)
     */
    fun send(dataModel: DataModel) {
        val encryptedData = encryptionManager.encrypt(dataModel)
        outputStream.writeInt(encryptedData.size)
        outputStream.write(encryptedData)
        outputStream.flush()
    }

    /**
     * Receive a DataModel from the socket (automatically decrypted)
     * Returns null if connection is closed
     */
    fun receive(): DataModel? {
        return try {
            val length = inputStream.readInt()
            if (length <= 0 || length > 10_000_000) { // 10MB safety limit
                throw IOException("Invalid data length: $length")
            }
            val encryptedData = ByteArray(length)
            inputStream.readFully(encryptedData)
            encryptionManager.decrypt(encryptedData)
        } catch (e: EOFException) {
            null // Connection closed
        }
    }

    fun isConnected(): Boolean = socket.isConnected && !socket.isClosed
    fun getSocket(): Socket = socket
    fun getEncryptionManager(): EncryptionManager = encryptionManager

    override fun close() {
        try {
            outputStream.close()
            inputStream.close()
            socket.close()
        } catch (e: IOException) {
            // Ignore close errors
        } finally {
            encryptionManager.clear()
        }
    }
}