package com.example.assistentemeditechmobile

import com.example.assistentemeditechmobile.domain.CalendarConflictEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarConflictTest {

    // Base timestamps: 10:00 to 10:30 (30 mins = 1,800,000 ms)
    private val baseStart = 1_700_000_000_000L
    private val baseEnd = baseStart + (30 * 60_000L) // 10:30

    @Test
    fun testDirectOverlapReturnsConflict() {
        // Proposed: 10:15 to 10:45 (partially overlaps base 10:00-10:30)
        val propStart = baseStart + (15 * 60_000L)
        val propEnd = propStart + (30 * 60_000L)

        val hasConflict = CalendarConflictEvaluator.hasConflict(
            existingStart = baseStart,
            existingEnd = baseEnd,
            isCancelled = false,
            proposedStart = propStart,
            proposedEnd = propEnd,
            bufferMinutes = 10L
        )

        assertTrue(hasConflict)
    }

    @Test
    fun testBufferProximityReturnsConflict() {
        // Proposed: 10:35 to 11:00 (5 minutes after base ends, buffer is 10 min)
        val propStart = baseEnd + (5 * 60_000L)
        val propEnd = propStart + (25 * 60_000L)

        val hasConflict = CalendarConflictEvaluator.hasConflict(
            existingStart = baseStart,
            existingEnd = baseEnd,
            isCancelled = false,
            proposedStart = propStart,
            proposedEnd = propEnd,
            bufferMinutes = 10L
        )

        assertTrue("Should detect conflict due to 10-minute buffer window", hasConflict)
    }

    @Test
    fun testSufficientGapReturnsNoConflict() {
        // Proposed: 10:45 to 11:15 (15 minutes after base ends, exceeds 10 min buffer)
        val propStart = baseEnd + (15 * 60_000L)
        val propEnd = propStart + (30 * 60_000L)

        val hasConflict = CalendarConflictEvaluator.hasConflict(
            existingStart = baseStart,
            existingEnd = baseEnd,
            isCancelled = false,
            proposedStart = propStart,
            proposedEnd = propEnd,
            bufferMinutes = 10L
        )

        assertFalse("Should not conflict when gap exceeds buffer", hasConflict)
    }

    @Test
    fun testCancelledEventNeverConflicts() {
        // Same time slot, but existing event was cancelled by user
        val hasConflict = CalendarConflictEvaluator.hasConflict(
            existingStart = baseStart,
            existingEnd = baseEnd,
            isCancelled = true,
            proposedStart = baseStart,
            proposedEnd = baseEnd,
            bufferMinutes = 10L
        )

        assertFalse("Cancelled events should never trigger a conflict", hasConflict)
    }

    @Test
    fun testEnclosingEventReturnsConflict() {
        // Proposed spans 09:30 to 11:00, completely covering 10:00-10:30
        val propStart = baseStart - (30 * 60_000L)
        val propEnd = baseEnd + (30 * 60_000L)

        val hasConflict = CalendarConflictEvaluator.hasConflict(
            existingStart = baseStart,
            existingEnd = baseEnd,
            isCancelled = false,
            proposedStart = propStart,
            proposedEnd = propEnd,
            bufferMinutes = 10L
        )

        assertTrue(hasConflict)
    }
}
