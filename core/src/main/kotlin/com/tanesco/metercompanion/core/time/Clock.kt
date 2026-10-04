package com.tanesco.metercompanion.core.time

/**
 * Source of wall-clock time.
 *
 * The domain layer never calls `System.currentTimeMillis()` directly. Time
 * arrives through this port so that domain rules - token age, session
 * duration, event ordering, "recent activity" windows - are deterministic under
 * test and identical on any platform.
 */
fun interface Clock {

    /** Milliseconds since the Unix epoch, UTC. */
    fun nowMillis(): Long

    companion object {
        /** Production clock. */
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}

/**
 * Source of opaque identifiers.
 *
 * Kept behind a port so that persistence tests can assert on stable values
 * instead of random ones.
 */
fun interface IdGenerator {

    fun newId(): String

    companion object {
        /**
         * Random 128-bit identifier rendered as a 32 character hex string.
         *
         * `UUID.randomUUID()` is used rather than `SecureRandom` directly so the
         * implementation stays obvious; these identifiers are local object keys,
         * not secrets and not meter credentials.
         */
        val Random: IdGenerator = IdGenerator {
            java.util.UUID.randomUUID().toString().replace("-", "")
        }
    }
}

/** Formats [millis] as an ISO-8601 UTC timestamp, e.g. `2026-10-04T21:15:04Z`. */
fun formatIso8601Utc(millis: Long): String {
    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return format.format(java.util.Date(millis))
}
