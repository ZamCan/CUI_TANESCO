package com.tanesco.metercompanion.core.model

/**
 * A single physical quantity together with the origin of that value.
 *
 * This type exists to enforce the project's core honesty rule at the type
 * level: a number can only be rendered if a [Provenance] says where it came
 * from. There is deliberately no "unknown"/"sample" provenance - a value is
 * either read from a meter or it does not exist in the model.
 */
data class Measurement<out T : Number>(
    val value: T,
    val unit: String,
    val provenance: Provenance,
) {
    enum class Provenance {
        /** Reported by a meter over an established connection. */
        MeterReported,

        /** Derived from meter-reported data by a documented calculation. */
        Derived,
    }
}

/**
 * Electrical quantities a meter may report.
 *
 * Every field is nullable because none of these can be populated until a real
 * connection adapter exists. The UI renders "Not available" for `null`; it must
 * never substitute a placeholder number.
 */
data class EnergySnapshot(
    val voltage: Measurement<Double>? = null,
    val current: Measurement<Double>? = null,
    val power: Measurement<Double>? = null,
    val activeEnergy: Measurement<Double>? = null,
    val reactiveEnergy: Measurement<Double>? = null,
    val powerFactor: Measurement<Double>? = null,
    val frequency: Measurement<Double>? = null,
    val creditBalance: Measurement<Double>? = null,
    val capturedAtMillis: Long? = null,
) {
    /** True when nothing at all has been read, which is the expected state today. */
    val isEmpty: Boolean
        get() = voltage == null && current == null && power == null &&
            activeEnergy == null && reactiveEnergy == null &&
            powerFactor == null && frequency == null && creditBalance == null

    companion object {
        /**
         * The only value the application can legitimately hold right now: a
         * snapshot in which nothing has been measured.
         */
        val Empty: EnergySnapshot = EnergySnapshot()
    }
}

/** SI units used across the application, kept as constants so the UI never guesses. */
object Units {
    const val VOLTS = "V"
    const val AMPERES = "A"
    const val WATTS = "W"
    const val KILOWATT_HOURS = "kWh"
    const val HERTZ = "Hz"
    const val TZS = "TZS"
}
