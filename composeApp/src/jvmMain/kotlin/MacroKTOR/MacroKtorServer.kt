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
    // To generate a keystore for development, use the following command:
    // keytool -genkeypair -alias macropad -keyalg RSA -storetype PKCS12 -keystore keystore.p12 -storepass hoopla -keypass hoopla -validity 365
    // Make sure to replace 'hoopla' with a secure password.

    // Using an ABSOLUTE path to avoid any ambiguity about the file location.
    val keystoreFile = File("/home/kyle/IMPORTED_ANDROID_STUDIO_PROJECTS/OpenMacropadKMP/keystore.p12")
    val keyAlias = "macropad" // This MUST match the alias used during keystore generation
    val keystorePassword = "hoopla" // This MUST match your keystore password
    val privateKeyPassword = "hoopla" // This MUST match your private key password

    val environment = applicationEngineEnvironment {
        if (keystoreFile.exists()) {
            // Explicitly load the PKCS12 keystore. This is more robust than relying on format detection.
            val keyStore = KeyStore.getInstance("PKCS12")
            keystoreFile.inputStream().use {
                keyStore.load(it, keystorePassword.toCharArray())
            }

            sslConnector(
                keyStore = keyStore,
                keyAlias = keyAlias,
                keyStorePassword = { keystorePassword.toCharArray() },
                privateKeyPassword = { privateKeyPassword.toCharArray() }
            ) {
                port = 8443 // Standard port for HTTPS/WSS
            }
        } else {
            // Fallback to a regular connector if the keystore is not found
            // For production, you might want to throw an error instead.
            println("Warning: Keystore not found at ${keystoreFile.absolutePath}. Using unencrypted connection.")
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
