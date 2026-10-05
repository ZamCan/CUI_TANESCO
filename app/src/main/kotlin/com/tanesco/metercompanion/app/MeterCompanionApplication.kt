package com.tanesco.metercompanion.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the [AppContainer] for the lifetime of the process.
 *
 * Startup work is launched on an application-scoped [CoroutineScope] rather than
 * on a lifecycle-bound one: recording the startup event must not be cancelled
 * just because the user rotated the device or backgrounded the app.
 */
class MeterCompanionApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.onApplicationStarted()
        }
    }

    override fun onTerminate() {
        // Only called on emulators; present so the container's resources have an
        // explicit owner and a clear teardown path.
        container.close()
        super.onTerminate()
    }
}
