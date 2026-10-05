package com.tanesco.metercompanion.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * The dispatchers the application runs on.
 *
 * Passed explicitly rather than referenced as globals so that tests can replace
 * them and so that the choice is visible at construction sites.
 */
class AndroidDispatchers private constructor(
    /**
     * Single-threaded dispatcher for all database work.
     *
     * One thread - rather than [Dispatchers.IO] - is deliberate: it serialises
     * repository access so that transactions cannot interleave, which lets the
     * SQL driver stay simple and removes a whole class of race conditions. The
     * workload is tiny local reads and writes, so a single thread is ample.
     */
    val io: CoroutineDispatcher,
    val computation: CoroutineDispatcher,
) {

    fun shutdown() {
        (io as? java.util.concurrent.ExecutorService)?.shutdown()
        (computation as? java.util.concurrent.ExecutorService)?.shutdown()
    }

    companion object {
        fun create(threadNamePrefix: String = "tanesco-db"): AndroidDispatchers =
            AndroidDispatchers(
                io = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "$threadNamePrefix-io").apply { isDaemon = true }
                }.asCoroutineDispatcher(),
                computation = Dispatchers.Default,
            )
    }
}
