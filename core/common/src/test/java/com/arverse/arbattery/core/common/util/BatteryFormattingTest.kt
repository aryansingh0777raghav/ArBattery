package com.arverse.arbattery.core.common.util

import com.arverse.arbattery.core.common.model.ReadingQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryFormattingTest {

    private val now = 1_700_000_000_000L // Realistic epoch timestamp in ms

    @Test
    fun `formatPercentage returns formatted string or double dash`() {
        assertEquals("75%", BatteryFormatting.formatPercentage(75))
        assertEquals("0%", BatteryFormatting.formatPercentage(0))
        assertEquals("100%", BatteryFormatting.formatPercentage(100))
        assertEquals("--", BatteryFormatting.formatPercentage(null))
    }

    @Test
    fun `isValidPercentage validates 0 to 100 range`() {
        assertTrue(BatteryFormatting.isValidPercentage(0))
        assertTrue(BatteryFormatting.isValidPercentage(50))
        assertTrue(BatteryFormatting.isValidPercentage(100))
        assertFalse(BatteryFormatting.isValidPercentage(-1))
        assertFalse(BatteryFormatting.isValidPercentage(101))
        assertFalse(BatteryFormatting.isValidPercentage(null))
    }

    @Test
    fun `computeFreshness categorizes live recent and stale correctly`() {
        assertEquals(ReadingQuality.LIVE, BatteryFormatting.computeFreshness(now - 30_000L, now))
        assertEquals(ReadingQuality.LIVE, BatteryFormatting.computeFreshness(now - 90_000L, now))
        assertEquals(ReadingQuality.RECENT, BatteryFormatting.computeFreshness(now - 5 * 60_000L, now))
        assertEquals(ReadingQuality.RECENT, BatteryFormatting.computeFreshness(now - 14 * 60_000L, now))
        assertEquals(ReadingQuality.STALE, BatteryFormatting.computeFreshness(now - 20 * 60_000L, now))
        assertEquals(ReadingQuality.STALE, BatteryFormatting.computeFreshness(now - 24 * 3600_000L, now))
        assertEquals(ReadingQuality.UNAVAILABLE, BatteryFormatting.computeFreshness(0L, now))
    }

    @Test
    fun `formatRelativeTime produces readable strings`() {
        assertEquals("Never", BatteryFormatting.formatRelativeTime(0L, now))
        assertEquals("Just now", BatteryFormatting.formatRelativeTime(now - 10_000L, now))
        assertEquals("45s ago", BatteryFormatting.formatRelativeTime(now - 45_000L, now))
        assertEquals("5m ago", BatteryFormatting.formatRelativeTime(now - 5 * 60_000L, now))
        assertEquals("3h ago", BatteryFormatting.formatRelativeTime(now - 3 * 3600_000L, now))
    }
}
