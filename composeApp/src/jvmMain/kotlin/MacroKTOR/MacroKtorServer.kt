package MacroKTOR

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.File
import java.security.KeyStore
import java.time.Duration

fun main() {
    // To generate a keystore for development, you can use the following command:
    // keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048 -ext SAN=DNS:localhost,IP:127.0.0.1
    // Make sure to replace 'password' with a secure password.
    // Place the generated 'keystore.jks' file in a known location.

    val keystoreFile = File("path/to/your/keystore.jks") // CHANGE THIS
    val keyAlias = "selfsigned"
    val keystorePassword = "password" // CHANGE THIS
    val privateKeyPassword = "password" // CHANGE THIS

    val environment = applicationEngineEnvironment {
        if (keystoreFile.exists()) {
            sslConnector(
                keyStore = KeyStore.getInstance(keystoreFile, keystorePassword.toCharArray()),
                keyAlias = keyAlias,
                keyStorePassword = { keystorePassword.toCharArray() },
                privateKeyPassword = { privateKeyPassword.toCharArray() }
            ) {
                port = 8443 // Standard port for HTTPS/WSS
            }
        } else {
            // Fallback to a regular connector if the keystore is not found
            // For production, you might want to throw an error instead.
            println("Warning: Keystore not found. Using unencrypted connection.")
            connector {
                port = 8080
            }
        }
        module {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/ws") {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                println("Received: $text")
                                send(Frame.Text("You said: $text"))
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    embeddedServer(Netty, environment).start(wait = true)
}
