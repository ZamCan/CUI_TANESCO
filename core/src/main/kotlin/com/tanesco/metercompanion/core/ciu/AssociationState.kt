package com.tanesco.metercompanion.core.ciu

/**
 * The association lifecycle between a Digital CIU host and one meter.
 *
 * ## Why this is a separate state machine
 *
 * The specification this project is induced from draws a hard distinction that
 * most naive implementations collapse:
 *
 *  - *knowing* a meter number        - the user typed it
 *  - *identifying* a meter           - a transport returned a serial
 *  - *being associated* with a meter - the pairing was accepted
 *  - *communicating* with a meter    - a request/response round trip succeeded
 *  - *being authorised*              - the meter accepted our credentials
 *
 * Treating any of these as "connected" is the classic CIU bug: it produces a UI
 * that claims success against the wrong physical meter. Each is modelled here as
 * its own state, and [AssociationEngine] refuses transitions that would skip a
 * step.
 *
 * States are a closed set so that presentation can map them exhaustively - a new
 * state becomes a compile error in every screen that renders it.
 */
enum class AssociationState(val label: String) {
    /** Nothing is known about any meter. */
    Unknown("Unknown"),

    /**
     * A meter number is known but nothing has been read from a device.
     *
     * Distinct from [Associated]: the number came from the user, not a meter.
     */
    Identified("Identified"),

    /** The meter requires a pairing step before it will answer. */
    AssociationRequired("Association required"),

    /** A pairing request has been sent and no answer has arrived yet. */
    AssociationRequested("Association requested"),

    /** The meter accepted the pairing. Still not yet verified. */
    Associated("Associated"),

    /** The meter number is being read back for comparison. */
    Verifying("Verifying identity"),

    /** The meter confirmed its own identity matches what we expect. */
    Verified("Verified"),

    /**
     * A request was issued and did not complete.
     *
     * Deliberately *not* terminal: recovery returns the machine to
     * [Associated] or [Verified] rather than starting over.
     */
    CommunicationFailed("Communication failed"),

    /** No session is open, and association status is unknown. */
    Disconnected("Disconnected"),
    ;

    /** True when the meter will answer requests in this state. */
    val canExchangeData: Boolean
        get() = this == Verified || this == Associated

    /** True when identity has been confirmed against the meter itself. */
    val isIdentityConfirmed: Boolean
        get() = this == Verified

    /** True when nothing is in progress and no failure is outstanding. */
    val isSettled: Boolean
        get() = this == Verified || this == Unknown || this == Disconnected
}

/** Why a transition was refused. */
enum class TransitionRefusal(val code: String) {
    /** The transition is not permitted from the current state. */
    IllegalTransition("association.illegal_transition"),

    /** The transition needs information the caller did not supply. */
    MissingEvidence("association.missing_evidence"),

    /** The meter answered, and it is not the meter that was expected. */
    WrongMeter("association.wrong_meter"),

    /** The meter refused the pairing. */
    AssociationRejected("association.rejected"),
}

/**
 * The result of asking the engine to change state.
 *
 * A failure is a value carrying a stable [TransitionRefusal.code], never an
 * exception, because "the meter said no" is an expected outcome that the UI must
 * render rather than crash on.
 */
sealed interface TransitionResult {

    data class Accepted(val state: AssociationState) : TransitionResult

    data class Refused(
        val state: AssociationState,
        val refusal: TransitionRefusal,
        /** Developer-facing detail. Safe for diagnostics; not user copy. */
        val detail: String,
    ) : TransitionResult

    val stateOrNull: AssociationState?
        get() = when (this) {
            is Accepted -> state
            is Refused -> state
        }
}

/**
 * The association state machine.
 *
 * Pure and total: it holds no I/O, no clock and no state of its own. Callers
 * apply the returned [AssociationState] to whatever they persist. That is what
 * makes all twelve required scenarios testable without a radio, an emulator or
 * a meter.
 *
 * ### Permitted transitions
 *
 * ```
 *  Unknown            ──identify──────────────▶ Identified
 *  Identified         ──requireAssociation───▶ AssociationRequired
 *  AssociationRequired──requestAssociation───▶ AssociationRequested
 *  AssociationRequested ──accept──────────────▶ Associated
 *  Associated         ──beginVerify───────────▶ Verifying
 *  Verifying          ──identityMatches───────▶ Verified
 *  (any engaged)      ──fail─────────────────▶ CommunicationFailed
 *  (any)              ──disconnect────────────▶ Disconnected
 *  CommunicationFailed ──recover──────────────▶ Associated | Verified
 * ```
 *
 * A verified identity can be re-verified, because a meter may be swapped at the
 * same account number - and that is exactly the case a CIU must not miss.
 */
object AssociationEngine {

    /** The customer supplied a meter number. Nothing has been read yet. */
    fun identify(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.Unknown,
            AssociationState.Disconnected,
            AssociationState.CommunicationFailed,
            -> TransitionResult.Accepted(AssociationState.Identified)

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "A meter is already identified; disconnect before identifying another.",
            )
        }

    /** The meter declared that pairing is needed. */
    fun requireAssociation(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.Identified ->
                TransitionResult.Accepted(AssociationState.AssociationRequired)

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "Association can only be required for an identified meter.",
            )
        }

    /** A pairing request is being sent. */
    fun requestAssociation(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.AssociationRequired ->
                TransitionResult.Accepted(AssociationState.AssociationRequested)

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "No association request is outstanding.",
            )
        }

    /** The meter accepted the pairing. */
    fun acceptAssociation(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.AssociationRequested ->
                TransitionResult.Accepted(AssociationState.Associated)

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "There is no outstanding association request to accept.",
            )
        }

    /** The meter refused the pairing. */
    fun rejectAssociation(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.AssociationRequested ->
                TransitionResult.Refused(
                    state = AssociationState.Identified,
                    refusal = TransitionRefusal.AssociationRejected,
                    detail = "The meter refused the association request.",
                )

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "There is no outstanding association request to reject.",
            )
        }

    /** Identity verification is starting. */
    fun beginVerification(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.Associated, AssociationState.Verified ->
                TransitionResult.Accepted(AssociationState.Verifying)

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "Only an associated meter can be verified.",
            )
        }

    /**
     * The meter reported its identity.
     *
     * [expectedMeterNumber] is what the user configured; [reportedMeterNumber] is
     * what the device said. They are compared as trimmed, case-folded strings -
     * physical meter labels are transcribed by hand, so surrounding whitespace
     * and letter case must not cause a false mismatch.
     */
    fun identityReported(
        current: AssociationState,
        expectedMeterNumber: String?,
        reportedMeterNumber: String?,
    ): TransitionResult {
        if (current != AssociationState.Verifying) {
            return refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "Identity can only be evaluated while verifying.",
            )
        }
        val expected = expectedMeterNumber?.trim()?.uppercase()
        val reported = reportedMeterNumber?.trim()?.uppercase()
        if (expected.isNullOrEmpty() || reported.isNullOrEmpty()) {
            return TransitionResult.Refused(
                state = current,
                refusal = TransitionRefusal.MissingEvidence,
                detail = "A meter number was expected but the meter reported none.",
            )
        }
        return if (expected == reported) {
            TransitionResult.Accepted(AssociationState.Verified)
        } else {
            TransitionResult.Refused(
                state = AssociationState.CommunicationFailed,
                refusal = TransitionRefusal.WrongMeter,
                detail = "The device answered with meter number '$reported', " +
                    "but '$expected' was expected.",
            )
        }
    }

    /**
     * A request did not complete.
     *
     * The caller records the pre-failure state and passes it back to [recover],
     * so recovery returns to precisely where the session was rather than to a
     * generic starting point.
     */
    fun communicationFailed(current: AssociationState): TransitionResult =
        when (current) {
            AssociationState.CommunicationFailed, AssociationState.Unknown ->
                refuse(
                    current,
                    TransitionRefusal.IllegalTransition,
                    "There is nothing in progress to fail.",
                )

            else -> TransitionResult.Accepted(AssociationState.CommunicationFailed)
        }

    /** A reconnect succeeded; returns to the state the failure interrupted. */
    fun recover(current: AssociationState, lastGoodState: AssociationState): TransitionResult =
        when (current) {
            AssociationState.CommunicationFailed ->
                when (lastGoodState) {
                    AssociationState.Verified ->
                        TransitionResult.Accepted(AssociationState.Verified)

                    AssociationState.Associated,
                    AssociationState.Verifying,
                    -> TransitionResult.Accepted(AssociationState.Associated)

                    else -> refuse(
                        current,
                        TransitionRefusal.IllegalTransition,
                        "Cannot recover to $lastGoodState; there is no usable prior state.",
                    )
                }

            else -> refuse(
                current,
                TransitionRefusal.IllegalTransition,
                "Recovery only applies to a failed communication.",
            )
        }

    /** The session closed. Association knowledge itself is retained. */
    fun disconnect(current: AssociationState): TransitionResult = when (current) {
        AssociationState.Unknown -> TransitionResult.Accepted(AssociationState.Disconnected)

        AssociationState.Disconnected -> TransitionResult.Refused(
            state = current,
            refusal = TransitionRefusal.IllegalTransition,
            detail = "Already disconnected.",
        )

        else -> TransitionResult.Accepted(AssociationState.Disconnected)
    }

    private fun refuse(
        current: AssociationState,
        refusal: TransitionRefusal,
        detail: String,
    ): TransitionResult.Refused = TransitionResult.Refused(current, refusal, detail)
}
