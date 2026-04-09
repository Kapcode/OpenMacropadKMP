package switchdektoptocompose.logic

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object TrustedDeviceManager {
    private val workingDir = File(System.getProperty("user.home"), ".openmacropad")
    private val trustedDevicesFile = File(workingDir, "trusted_devices.json")
    
    // Map of Fingerprint -> DeviceName
    private val trustedDevices = ConcurrentHashMap<String, String>()

    init {
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        load()
    }

    private fun load() {
        if (trustedDevicesFile.exists()) {
            try {
                val content = trustedDevicesFile.readText()
                val json = JSONObject(content)
                json.keys().forEach { fingerprint ->
                    trustedDevices[fingerprint] = json.getString(fingerprint)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun save() {
        try {
            val json = JSONObject()
            trustedDevices.forEach { (fingerprint, name) ->
                json.put(fingerprint, name)
            }
            trustedDevicesFile.writeText(json.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isTrusted(fingerprint: String): Boolean {
        return trustedDevices.containsKey(fingerprint)
    }

    fun addTrustedDevice(fingerprint: String, name: String) {
        trustedDevices[fingerprint] = name
        save()
    }

    fun removeTrustedDevice(fingerprint: String) {
        trustedDevices.remove(fingerprint)
        save()
    }

    fun getTrustedDevices(): Map<String, String> {
        return trustedDevices.toMap()
    }
}
