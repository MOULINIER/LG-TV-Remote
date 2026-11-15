package com.moulinier.lgremote.tv

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class DiscoveredTv(val host: String, val name: String)

/** SSDP discovery of LG webOS TVs on the local network. */
object Discovery {
    private const val ST = "urn:lge-com:service:webos-second-screen:1"

    suspend fun scan(context: Context, durationMs: Long = 3500): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("lgremote-ssdp").apply { setReferenceCounted(false); acquire() }
        val found = LinkedHashMap<String, String?>() // host -> LOCATION
        try {
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.soTimeout = 500
                val msg = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: $ST\r\n\r\n"
                    ).toByteArray()
                val group = InetAddress.getByName("239.255.255.250")
                repeat(2) { sock.send(DatagramPacket(msg, msg.size, group, 1900)) }
                val buf = ByteArray(2048)
                val end = System.currentTimeMillis() + durationMs
                while (System.currentTimeMillis() < end) {
                    val pkt = DatagramPacket(buf, buf.size)
                    try {
                        sock.receive(pkt)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val text = String(pkt.data, 0, pkt.length)
                    if (!text.contains("webos", ignoreCase = true) && !text.contains("LG", ignoreCase = false)) continue
                    val host = pkt.address.hostAddress ?: continue
                    val location = text.lineSequence()
                        .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                        ?.substringAfter(':')?.trim()
                    if (host !in found) found[host] = location
                }
            }
        } catch (_: Exception) {
        } finally {
            lock.release()
        }
        found.map { (host, location) -> DiscoveredTv(host, friendlyName(location) ?: "LG TV ($host)") }
    }

    private fun friendlyName(location: String?): String? {
        if (location.isNullOrBlank()) return null
        return runCatching {
            WebOsClient.sharedHttp.newCall(Request.Builder().url(location).build()).execute().use { r ->
                val body = r.body?.string() ?: return null
                Regex("<friendlyName>(.*?)</friendlyName>").find(body)?.groupValues?.get(1)
            }
        }.getOrNull()
    }
}
