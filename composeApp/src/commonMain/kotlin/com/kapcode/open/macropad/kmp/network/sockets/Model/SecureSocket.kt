package Model

import java.io.*
import java.net.Socket

/**
 * Wrapper around Socket that handles encrypted DataModel transmission
 */
class SecureSocket(
    private val socket: Socket,
    private val encryptionManager: EncryptionManager
) : Closeable {

    private val outputStream: DataOutputStream = DataOutputStream(socket.getOutputStream())
    private val inputStream: DataInputStream = DataInputStream(socket.getInputStream())

    companion object {
        /**
         * Perform key exchange and create a SecureSocket
         * Call this on the CLIENT side
         */
        fun clientHandshake(socket: Socket): SecureSocket {
            val encryptionManager = EncryptionManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Send our public key
            val ourPublicKey = encryptionManager.initializeKeyExchange()
            output.writeInt(ourPublicKey.size)
            output.write(ourPublicKey)
            output.flush()

            // Receive server's public key
            val theirKeySize = input.readInt()
            val theirPublicKey = ByteArray(theirKeySize)
            input.readFully(theirPublicKey)

            // Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKey)

            return SecureSocket(socket, encryptionManager)
        }

        /**
         * Perform key exchange and create a SecureSocket
         * Call this on the SERVER side
         */
        fun serverHandshake(socket: Socket): SecureSocket {
            val encryptionManager = EncryptionManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Receive client's public key
            val theirKeySize = input.readInt()
            val theirPublicKey = ByteArray(theirKeySize)
            input.readFully(theirPublicKey)

            // Send our public key
            val ourPublicKey = encryptionManager.initializeKeyExchange()
            output.writeInt(ourPublicKey.size)
            output.write(ourPublicKey)
            output.flush()

            // Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKey)

            return SecureSocket(socket, encryptionManager)
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

        // Send length first, then data
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

    /**
     * Check if the socket is connected
     */
    fun isConnected(): Boolean = socket.isConnected && !socket.isClosed

    /**
     * Get the underlying socket
     */
    fun getSocket(): Socket = socket

    /**
     * Get the encryption manager (for advanced usage)
     */
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