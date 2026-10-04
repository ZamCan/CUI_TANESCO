package com.tanesco.metercompanion.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tanesco.metercompanion.core.model.AppMetadata
import com.tanesco.metercompanion.core.model.RuntimeEnvironment

/**
 * Reads application and device facts from the Android platform.
 *
 * Every lookup is defensive: a missing or unreadable value yields `null` or a
 * labelled placeholder rather than an exception, because the About screen must
 * render even when the package manager returns something unexpected.
 */
class AndroidAppMetadataProvider(
    private val context: Context,
) {

    fun appMetadata(): AppMetadata {
        val packageName = context.packageName
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
        }.getOrNull()

        val label = packageInfo?.applicationInfo
            ?.let { info -> runCatching { info.loadLabel(context.packageManager).toString() }.getOrNull() }
            ?: context.packageName

        return AppMetadata(
            packageName = packageName,
            versionName = packageInfo?.versionName ?: UNKNOWN,
            versionCode = packageInfo?.let { info ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            } ?: 0L,
            applicationLabel = label,
            installerPackage = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.packageManager.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getInstallerPackageName(packageName)
                }
            }.getOrNull(),
            firstInstalledAtMillis = packageInfo?.firstInstallTime,
            lastUpdatedAtMillis = packageInfo?.lastUpdateTime,
        )
    }

    fun runtimeEnvironment(): RuntimeEnvironment = RuntimeEnvironment(
        androidRelease = Build.VERSION.RELEASE ?: UNKNOWN,
        androidSdkInt = Build.VERSION.SDK_INT,
        deviceManufacturer = Build.MANUFACTURER ?: UNKNOWN,
        deviceModel = Build.MODEL ?: UNKNOWN,
        supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
        is64Bit = Build.SUPPORTED_64_BIT_ABIS?.isNotEmpty() == true,
    )

    private companion object {
        const val UNKNOWN = "Unknown"
    }
}
