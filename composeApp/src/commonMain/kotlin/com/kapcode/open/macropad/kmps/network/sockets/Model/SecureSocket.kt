package Model

import java.io.*
import java.math.BigInteger
import java.net.Socket
import javax.crypto.spec.DHParameterSpec

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
         * @return A Pair containing the SecureSocket and the server's device name.
         */
        fun clientHandshake(socket: Socket, clientName: String): Pair<SecureSocket, String> {
            val encryptionManager = EncryptionManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Receive server's public key, DH parameters, and name
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

            // Initialize our key exchange with server's parameters
            val serverSpec = DHParameterSpec(p, g)
            val (ourPublicKeyBytes, _) = encryptionManager.initializeKeyExchange(serverSpec)
            
            // Send our public key and name
            output.writeInt(ourPublicKeyBytes.size)
            output.write(ourPublicKeyBytes)
            output.writeUTF(clientName)
            output.flush()
            
            // Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKeyBytes)

            return SecureSocket(socket, encryptionManager) to serverName
        }

        /**
         * Perform key exchange and create a SecureSocket
         * Call this on the SERVER side
         * @return A Pair containing the SecureSocket and the client's device name.
         */
        fun serverHandshake(socket: Socket, serverName: String): Pair<SecureSocket, String> {
            val encryptionManager = EncryptionManager()
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Initialize our key exchange and get our parameters
            val (ourPublicKeyBytes, ourParams) = encryptionManager.initializeKeyExchange()
            val p = ourParams!!.p
            val g = ourParams!!.g
            val pBytes = p.toByteArray()
            val gBytes = g.toByteArray()

            // Send our public key, DH parameters, and name
            output.writeInt(ourPublicKeyBytes.size)
            output.write(ourPublicKeyBytes)
            output.writeInt(pBytes.size)
            output.write(pBytes)
            output.writeInt(gBytes.size)
            output.write(gBytes)
            output.writeUTF(serverName)
            output.flush()

            // Receive client's public key and name
            val theirKeySize = input.readInt()
            val theirPublicKeyBytes = ByteArray(theirKeySize)
            input.readFully(theirPublicKeyBytes)
            val clientName = input.readUTF()

            // Complete key exchange
            encryptionManager.completeKeyExchange(theirPublicKeyBytes)

            return SecureSocket(socket, encryptionManager) to clientName
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