package com.arverse.arbattery.core.network.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `isCellularOrSpecialIp correctly identifies CLAT 192_0_0_x cellular IPs`() {
        assertTrue(NetworkUtils.isCellularOrSpecialIp("192.0.0.2"))
        assertTrue(NetworkUtils.isCellularOrSpecialIp("192.0.0.4"))
        assertTrue(NetworkUtils.isCellularOrSpecialIp("192.0.0.1"))
    }

    @Test
    fun `isCellularOrSpecialIp correctly identifies loopback, link-local and CGNAT`() {
        assertTrue(NetworkUtils.isCellularOrSpecialIp("127.0.0.1"))
        assertTrue(NetworkUtils.isCellularOrSpecialIp("169.254.10.20"))
        assertTrue(NetworkUtils.isCellularOrSpecialIp("100.64.0.1"))
        assertTrue(NetworkUtils.isCellularOrSpecialIp("100.100.5.5"))
    }

    @Test
    fun `isCellularOrSpecialIp returns false for legitimate LAN IPs`() {
        assertFalse(NetworkUtils.isCellularOrSpecialIp("192.168.1.100"))
        assertFalse(NetworkUtils.isCellularOrSpecialIp("192.168.29.1"))
        assertFalse(NetworkUtils.isCellularOrSpecialIp("10.0.0.15"))
        assertFalse(NetworkUtils.isCellularOrSpecialIp("172.20.10.2"))
    }

    @Test
    fun `isPrivateLanIp returns true only for RFC 1918 addresses`() {
        assertTrue(NetworkUtils.isPrivateLanIp("192.168.1.1"))
        assertTrue(NetworkUtils.isPrivateLanIp("192.168.0.254"))
        assertTrue(NetworkUtils.isPrivateLanIp("10.0.0.1"))
        assertTrue(NetworkUtils.isPrivateLanIp("10.254.254.254"))
        assertTrue(NetworkUtils.isPrivateLanIp("172.16.0.1"))
        assertTrue(NetworkUtils.isPrivateLanIp("172.31.255.254"))

        // Cellular and public IPs are not private LAN
        assertFalse(NetworkUtils.isPrivateLanIp("192.0.0.2"))
        assertFalse(NetworkUtils.isPrivateLanIp("8.8.8.8"))
        assertFalse(NetworkUtils.isPrivateLanIp("1.1.1.1"))
        assertFalse(NetworkUtils.isPrivateLanIp("172.32.0.1"))
        assertFalse(NetworkUtils.isPrivateLanIp("127.0.0.1"))
    }
}
