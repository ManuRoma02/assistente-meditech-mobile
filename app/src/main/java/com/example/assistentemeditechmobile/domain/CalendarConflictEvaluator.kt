package com.example.assistentemeditechmobile.domain

object CalendarConflictEvaluator {
    /**
     * Determines whether a proposed time slot conflicts with an existing event.
     * Considers non-cancelled events and an optional buffer time (e.g. 10 minutes between appointments).
     */
    fun hasConflict(
        existingStart: Long,
        existingEnd: Long,
        isCancelled: Boolean,
        proposedStart: Long,
        proposedEnd: Long,
        bufferMinutes: Long = 10L
    ): Boolean {
        if (isCancelled) return false
        val bufferMillis = bufferMinutes * 60_000L
        val windowMin = proposedStart - bufferMillis
        val windowMax = proposedEnd + bufferMillis
        return existingStart < windowMax && existingEnd > windowMin
    }
}
