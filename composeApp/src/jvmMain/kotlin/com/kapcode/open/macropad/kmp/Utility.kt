import java.net.InetAddress

class Utility {


    //Function to Find out IP Address
    fun getSystemIP(): String? {
        return try {
            var sysIP: String = ""
            val osName = System.getProperty("os.name")
            if (osName.contains("Windows")) {
                sysIP = InetAddress.getLocalHost().hostAddress
            }

            sysIP
        } catch (E: Exception) {
            System.err.println("System IP Exp : " + E.message)
            null
        }
    }



}