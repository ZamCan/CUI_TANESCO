package com.tanesco.metercompanion.core.model

/**
 * Facts about the running application and the device it runs on.
 *
 * Everything here is read from the platform; nothing is hard-coded, so the About
 * screen cannot drift from the APK that is actually installed.
 */
data class AppMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val applicationLabel: String,
    val installerPackage: String?,
    val firstInstalledAtMillis: Long?,
    val lastUpdatedAtMillis: Long?,
) {
    /** e.g. `1.0.0 (1)`. */
    val displayVersion: String get() = "$versionName ($versionCode)"
}

/**
 * Device and runtime facts shown under "Technical information".
 *
 * Assembled on demand because reading some of these is not free, and the About
 * screen is not opened often enough to justify doing it at startup.
 */
data class RuntimeEnvironment(
    val androidRelease: String,
    val androidSdkInt: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val supportedAbis: List<String>,
    val is64Bit: Boolean,
) {
    val primaryAbi: String? get() = supportedAbis.firstOrNull()

    /** One line suitable for a single detail row. */
    val summary: String
        get() = "$deviceManufacturer $deviceModel · Android $androidRelease (API $androidSdkInt)"
}
