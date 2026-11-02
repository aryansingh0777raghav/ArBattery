package com.arverse.arbattery.core.common.util

import com.arverse.arbattery.core.common.model.ReadingQuality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BatteryFormatting {

    fun formatPercentage(percentage: Int?): String {
        return percentage?.let { "$it%" } ?: "--"
    }

    fun isValidPercentage(percentage: Int?): Boolean {
        return percentage != null && percentage in 0..100
    }

    fun computeFreshness(lastUpdated: Long, currentTime: Long = System.currentTimeMillis()): ReadingQuality {
        if (lastUpdated <= 0L) return ReadingQuality.UNAVAILABLE
        val diffMs = currentTime - lastUpdated
        if (diffMs < 0L) return ReadingQuality.LIVE // clock skew safety
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)

        return when {
            diffMinutes < 2 -> ReadingQuality.LIVE
            diffMinutes < 15 -> ReadingQuality.RECENT
            else -> ReadingQuality.STALE
        }
    }

    fun formatRelativeTime(timestamp: Long, currentTime: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L) return "Never"
        val diffMs = currentTime - timestamp
        if (diffMs < 0L) return "Just now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            seconds < 30 -> "Just now"
            minutes < 1 -> "${seconds}s ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
