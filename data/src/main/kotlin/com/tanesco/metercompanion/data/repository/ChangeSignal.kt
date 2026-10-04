package com.tanesco.metercompanion.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Change notification for repositories.
 *
 * The persistence port is deliberately synchronous and has no change feed of its
 * own, so a repository increments a revision counter after every write. An
 * observing flow re-runs its query whenever the revision changes.
 *
 * This keeps [SqlDriver] a minimal, easily testable interface at the cost of
 * re-running a query per change - an acceptable trade for a local database
 * holding tens of rows, and one that a driver-level change feed could replace
 * later without touching any repository.
 */
class ChangeSignal {

    private val revision = MutableStateFlow(0L)

    val changes: Flow<Long> get() = revision

    fun notifyChanged() {
        revision.value = revision.value + 1
    }
}

/** Runs a database read on [dispatcher]. */
internal suspend fun <T> CoroutineDispatcher.read(block: () -> T): T = withContext(this) { block() }

/** Runs a database write on [dispatcher]. */
internal suspend fun <T> CoroutineDispatcher.write(block: () -> T): T = withContext(this) { block() }

/** Maps a change signal into a freshly computed query result. */
internal fun <T> Flow<Long>.recompute(query: () -> T): Flow<T> = map { query() }
