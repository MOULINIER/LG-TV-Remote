package com.moulinier.lgremote.tv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {
    fun parseMac(mac: String): ByteArray? {
        val parts = mac.trim().split(':', '-')
        if (parts.size != 6) return null
        return runCatching { ByteArray(6) { parts[it].toInt(16).toByte() } }.getOrNull()
    }

    /** Sends a magic packet to the broadcast address (and directly to the last known IP). */
    suspend fun wake(mac: String, lastHost: String?): Boolean = withContext(Dispatchers.IO) {
        val macBytes = parseMac(mac) ?: return@withContext false
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 6 until packet.size) packet[i] = macBytes[(i - 6) % 6]
        runCatching {
            DatagramSocket().use { s ->
                s.broadcast = true
                val targets = buildList {
                    add("255.255.255.255")
                    lastHost?.let { h ->
                        add(h)
                        add(h.substringBeforeLast('.') + ".255")
                    }
                }
                repeat(3) {
                    for (t in targets) {
                        val addr = InetAddress.getByName(t)
                        s.send(DatagramPacket(packet, packet.size, addr, 9))
                        s.send(DatagramPacket(packet, packet.size, addr, 7))
                    }
                }
            }
            true
        }.getOrDefault(false)
    }
}
