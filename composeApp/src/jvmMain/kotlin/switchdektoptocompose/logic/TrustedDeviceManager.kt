package switchdektoptocompose.logic

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object TrustedDeviceManager {
    private val workingDir = File(System.getProperty("user.home"), ".openmacropad")
    private val trustedDevicesFile = File(workingDir, "trusted_devices.json")
    private val bannedDevicesFile = File(workingDir, "banned_devices.json")
    
    // Map of Fingerprint -> DeviceName
    private val trustedDevices = ConcurrentHashMap<String, String>()
    // Map of Fingerprint -> HardwareMetadata
    private val deviceMetadata = ConcurrentHashMap<String, String>()
    // Map of Fingerprint -> DeviceName
    private val bannedDevices = ConcurrentHashMap<String, String>()

    private val metadataFile = File(workingDir, "device_metadata.json")

    init {
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        load()
    }

    private fun load() {
        loadMap(trustedDevicesFile, trustedDevices)
        loadMap(bannedDevicesFile, bannedDevices)
        loadMap(metadataFile, deviceMetadata)
    }

    private fun loadMap(file: File, map: ConcurrentHashMap<String, String>) {
        if (file.exists()) {
            try {
                val content = file.readText()
                val json = JSONObject(content)
                json.keys().forEach { fingerprint ->
                    map[fingerprint] = json.getString(fingerprint)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun save() {
        saveMap(trustedDevicesFile, trustedDevices)
        saveMap(bannedDevicesFile, bannedDevices)
        saveMap(metadataFile, deviceMetadata)
    }

    private fun saveMap(file: File, map: ConcurrentHashMap<String, String>) {
        try {
            val json = JSONObject()
            map.forEach { (fingerprint, name) ->
                json.put(fingerprint, name)
            }
            file.writeText(json.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isTrusted(fingerprint: String): Boolean {
        return trustedDevices.containsKey(fingerprint)
    }

    fun isBanned(fingerprint: String): Boolean {
        return bannedDevices.containsKey(fingerprint)
    }

    fun addTrustedDevice(fingerprint: String, name: String, metadata: String? = null) {
        bannedDevices.remove(fingerprint) // Remove from ban list if adding to trusted
        trustedDevices[fingerprint] = name
        if (metadata != null) {
            deviceMetadata[fingerprint] = metadata
        }
        save()
    }

    fun getMetadata(fingerprint: String): String? {
        return deviceMetadata[fingerprint]
    }

    fun removeTrustedDevice(fingerprint: String) {
        trustedDevices.remove(fingerprint)
        save()
    }

    fun banDevice(fingerprint: String, name: String) {
        trustedDevices.remove(fingerprint) // Remove from trusted if banning
        bannedDevices[fingerprint] = name
        save()
    }

    fun unbanDevice(fingerprint: String) {
        bannedDevices.remove(fingerprint)
        save()
    }

    fun getTrustedDevices(): Map<String, String> {
        return trustedDevices.toMap()
    }

    fun getBannedDevices(): Map<String, String> {
        return bannedDevices.toMap()
    }
}
