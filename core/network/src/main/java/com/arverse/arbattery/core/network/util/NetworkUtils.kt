package com.arverse.arbattery.core.network.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class LanInterfaceInfo(
    val interfaceName: String,
    val ipAddress: String,
    val isWifi: Boolean,
    val isHotspot: Boolean
)

object NetworkUtils {

    const val DEFAULT_HUB_PORT = 8443

    /**
     * Interface name prefixes commonly used for cellular modems,
     * virtual interfaces, VPN tunnels, and 464XLAT translators.
     * These must NEVER be advertised as the local LAN Hub IP.
     */
    private val EXCLUDED_INTERFACE_PREFIXES = listOf(
        "rmnet",     // Qualcomm / Samsung cellular modem data
        "ccmni",     // MediaTek cellular modem
        "dummy",     // Kernel dummy interfaces
        "tun",       // VPN tunnel
        "tap",       // TAP virtual network
        "sit",       // IPv6-in-IPv4 tunnel
        "ip6tnl",    // IPv6 tunnel
        "v4-rmnet",  // 464XLAT translator interface (often 192.0.0.2 / 192.0.0.4)
        "p2p",       // Wi-Fi Direct peer-to-peer interface
        "wwan",      // Wireless WAN (cellular)
        "ppp",       // Point-to-point cellular/dialup
        "cellular"   // Generic cellular interface
    )

    /**
     * Checks if an IP is in the standard RFC 1918 Private IPv4 address spaces:
     * - 10.0.0.0/8 (10.x.x.x)
     * - 172.16.0.0/12 (172.16.x.x - 172.31.x.x)
     * - 192.168.0.0/16 (192.168.x.x)
     */
    fun isPrivateLanIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        val (a, b, _, _) = parts

        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            else -> false
        }
    }

    /**
     * Identifies addresses that are cellular carrier-assigned or special non-LAN IPs:
     * - 192.0.0.0/24 (RFC 7335 / 464XLAT CLAT, e.g. 192.0.0.2, 192.0.0.4)
     * - 100.64.0.0/10 (RFC 6598 Carrier Grade NAT for mobile networks)
     * - 169.254.0.0/16 (RFC 3927 Link-local / APIPA)
     * - 127.0.0.0/8 (Loopback)
     */
    fun isCellularOrSpecialIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return true
        val (a, b, c, _) = parts

        return when {
            a == 127 -> true
            a == 169 && b == 254 -> true
            a == 192 && b == 0 && c == 0 -> true // RFC 7335 192.0.0.x (CLAT - Samsung cellular data)
            a == 100 && b in 64..127 -> true     // CGNAT
            else -> false
        }
    }

    /**
     * Resolves the best IPv4 address to advertise for pairing and companion sync.
     * Prioritizes:
     * 1. Active Wi-Fi or Ethernet network link properties from ConnectivityManager
     * 2. wlan* interfaces with RFC 1918 private addresses
     * 3. ap* / softap* / swlan* interfaces (Wi-Fi Hotspot / SoftAP)
     * 4. Any other non-cellular interface with an RFC 1918 private address
     */
    fun getLocalIpAddress(context: Context? = null): String? {
        // Method 1: Check ConnectivityManager link properties if Context is available
        if (context != null) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (cm != null) {
                    val activeNetwork = cm.activeNetwork
                    if (activeNetwork != null) {
                        val caps = cm.getNetworkCapabilities(activeNetwork)
                        if (caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                            val linkProps = cm.getLinkProperties(activeNetwork)
                            if (linkProps != null) {
                                for (linkAddr in linkProps.linkAddresses) {
                                    val addr = linkAddr.address
                                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                                        val host = addr.hostAddress ?: continue
                                        if (isPrivateLanIp(host) && !isCellularOrSpecialIp(host)) {
                                            return host
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Method 2: Inspect NetworkInterfaces with strict filtering & ranking
        val candidateInterfaces = getAllLanInterfaces()
        if (candidateInterfaces.isNotEmpty()) {
            val best = candidateInterfaces.firstOrNull { it.isWifi }
                ?: candidateInterfaces.firstOrNull { it.isHotspot }
                ?: candidateInterfaces.first()
            return best.ipAddress
        }

        return null
    }

    /**
     * Returns all detected LAN IPv4 addresses with metadata, sorted by relevance.
     */
    fun getAllLanInterfaces(): List<LanInterfaceInfo> {
        val results = mutableListOf<LanInterfaceInfo>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name.lowercase()

                // Exclude cellular modems, dummy and tunnel interfaces
                if (EXCLUDED_INTERFACE_PREFIXES.any { prefix -> name.startsWith(prefix) }) {
                    continue
                }

                val isWifi = name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("en")
                val isHotspot = name.startsWith("ap") || name.startsWith("softap") || name.startsWith("swlan") || name.startsWith("rndis")

                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        // Reject 192.0.0.x (CLAT), 100.64.x (CGNAT), 169.254.x (Link-local), 127.x (Loopback)
                        if (isCellularOrSpecialIp(host)) continue

                        // We prefer RFC 1918 private IPs
                        if (isPrivateLanIp(host)) {
                            results.add(LanInterfaceInfo(intf.name, host, isWifi, isHotspot))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Sort: Wi-Fi interfaces first, then Hotspot, then other private LAN IPs
        return results.sortedWith(
            compareByDescending<LanInterfaceInfo> { it.isWifi }
                .thenByDescending { it.isHotspot }
        )
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
