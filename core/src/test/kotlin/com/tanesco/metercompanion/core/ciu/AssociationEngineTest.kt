package com.tanesco.metercompanion.core.ciu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The association engine across the twelve scenarios the engineering
 * specification requires.
 *
 * The engine is pure, so these run as fast JVM tests: no radio, no emulator and
 * no meter is involved. That is deliberate - the hardest part of a CIU is the
 * state machine, and it must be testable without hardware.
 */
class AssociationEngineTest {

    private fun accepted(result: TransitionResult): AssociationState {
        assertTrue("expected acceptance but was $result", result is TransitionResult.Accepted)
        return (result as TransitionResult.Accepted).state
    }

    private fun refused(
        result: TransitionResult,
        expected: TransitionRefusal = TransitionRefusal.IllegalTransition,
    ): TransitionResult.Refused {
        assertTrue("expected refusal but was $result", result is TransitionResult.Refused)
        val refusal = result as TransitionResult.Refused
        assertEquals(expected, refusal.refusal)
        return refusal
    }

    // --- 1. Unknown meter --------------------------------------------------

    @Test
    fun `unknown meter can be identified`() {
        assertEquals(
            AssociationState.Identified,
            accepted(AssociationEngine.identify(AssociationState.Unknown)),
        )
    }

    @Test
    fun `unknown meter cannot be verified`() {
        refused(AssociationEngine.beginVerification(AssociationState.Unknown))
    }

    // --- 2. Correct meter --------------------------------------------------

    @Test
    fun `matching identity verifies`() {
        assertEquals(
            AssociationState.Verifying,
            accepted(AssociationEngine.beginVerification(AssociationState.Associated)),
        )
        assertEquals(
            AssociationState.Verified,
            accepted(
                AssociationEngine.identityReported(
                    current = AssociationState.Verifying,
                    expectedMeterNumber = "TZ-00012345",
                    reportedMeterNumber = "TZ-00012345",
                ),
            ),
        )
    }

    @Test
    fun `identity comparison ignores case and surrounding whitespace`() {
        // Meter numbers are transcribed by hand from a printed label, so a
        // formatting difference must not read as the wrong meter.
        assertEquals(
            AssociationState.Verified,
            accepted(
                AssociationEngine.identityReported(
                    current = AssociationState.Verifying,
                    expectedMeterNumber = "TZ-00012345",
                    reportedMeterNumber = "  tz-00012345 ",
                ),
            ),
        )
    }

    // --- 3. Wrong meter ----------------------------------------------------

    @Test
    fun `mismatched identity is refused as the wrong meter`() {
        val refusal = refused(
            AssociationEngine.identityReported(
                current = AssociationState.Verifying,
                expectedMeterNumber = "TZ-00012345",
                reportedMeterNumber = "TZ-99999999",
            ),
            TransitionRefusal.WrongMeter,
        )
        // The machine lands on failure rather than staying in Verifying, so a
        // wrong-meter answer can never be mistaken for a usable session.
        assertEquals(AssociationState.CommunicationFailed, refusal.state)
    }

    @Test
    fun `empty reported identity is refused as missing evidence`() {
        refused(
            AssociationEngine.identityReported(
                current = AssociationState.Verifying,
                expectedMeterNumber = "TZ-00012345",
                reportedMeterNumber = "",
            ),
            TransitionRefusal.MissingEvidence,
        )
    }

    // --- 4. Association required -------------------------------------------

    @Test
    fun `identified meter can require association`() {
        assertEquals(
            AssociationState.AssociationRequired,
            accepted(AssociationEngine.requireAssociation(AssociationState.Identified)),
        )
    }

    // --- 5. Association accepted -------------------------------------------

    @Test
    fun `full association path reaches verified`() {
        var state = AssociationState.Unknown
        state = accepted(AssociationEngine.identify(state))
        state = accepted(AssociationEngine.requireAssociation(state))
        state = accepted(AssociationEngine.requestAssociation(state))
        state = accepted(AssociationEngine.acceptAssociation(state))
        state = accepted(AssociationEngine.beginVerification(state))
        state = accepted(
            AssociationEngine.identityReported(
                current = state,
                expectedMeterNumber = "TZ-1",
                reportedMeterNumber = "TZ-1",
            ),
        )
        assertEquals(AssociationState.Verified, state)
        assertTrue(state.isIdentityConfirmed)
        assertTrue(state.canExchangeData)
    }

    // --- 6. Association rejected -------------------------------------------

    @Test
    fun `rejected association returns to identified, not to a fake success`() {
        val refusal = refused(
            AssociationEngine.rejectAssociation(AssociationState.AssociationRequested),
            TransitionRefusal.AssociationRejected,
        )
        assertEquals(AssociationState.Identified, refusal.state)
    }

    // --- 7. Communication timeout ------------------------------------------

    @Test
    fun `timeout moves an engaged session to failure`() {
        assertEquals(
            AssociationState.CommunicationFailed,
            accepted(AssociationEngine.communicationFailed(AssociationState.Verified)),
        )
    }

    @Test
    fun `nothing can fail from a state where nothing is in progress`() {
        refused(AssociationEngine.communicationFailed(AssociationState.Unknown))
    }

    // --- 8. Communication recovery -----------------------------------------

    @Test
    fun `recovery returns a verified session to verified`() {
        assertEquals(
            AssociationState.Verified,
            accepted(
                AssociationEngine.recover(
                    current = AssociationState.CommunicationFailed,
                    lastGoodState = AssociationState.Verified,
                ),
            ),
        )
    }

    @Test
    fun `recovery from a mid-verification failure returns to associated`() {
        assertEquals(
            AssociationState.Associated,
            accepted(
                AssociationEngine.recover(
                    current = AssociationState.CommunicationFailed,
                    lastGoodState = AssociationState.Verifying,
                ),
            ),
        )
    }

    @Test
    fun `recovery is refused when no usable prior state exists`() {
        refused(
            AssociationEngine.recover(
                current = AssociationState.CommunicationFailed,
                lastGoodState = AssociationState.Unknown,
            ),
        )
    }

    // --- 9. Restart after successful association ---------------------------

    @Test
    fun `a persisted verified state is restorable and still permits requests`() {
        // Models an app restart: the state is read back from storage exactly as
        // it was written. Identity knowledge must survive, because re-pairing a
        // meter on every launch would be both slow and wrong.
        val persisted = AssociationState.Verified
        assertTrue(persisted.isIdentityConfirmed)
        assertTrue(persisted.canExchangeData)

        // And it can still be re-verified, which is how a swapped meter behind
        // the same account number is detected.
        assertEquals(
            AssociationState.Verifying,
            accepted(AssociationEngine.beginVerification(persisted)),
        )
    }

    // --- 10. Lost association ----------------------------------------------

    @Test
    fun `disconnect retains identity knowledge`() {
        val result = AssociationEngine.disconnect(AssociationState.Verified)
        assertEquals(AssociationState.Disconnected, accepted(result))
        // Identity knowledge lives in the profile, not in the session, so a
        // disconnect must not throw away the meter number.
        val recovered = accepted(AssociationEngine.identify(AssociationState.Disconnected))
        assertEquals(AssociationState.Identified, recovered)
    }

    @Test
    fun `a failed association can be retried after disconnect`() {
        var state = accepted(AssociationEngine.identify(AssociationState.Unknown))
        state = accepted(AssociationEngine.requireAssociation(state))
        state = accepted(AssociationEngine.requestAssociation(state))
        state = (AssociationEngine.rejectAssociation(state) as TransitionResult.Refused).state
        state = accepted(AssociationEngine.disconnect(state))

        // Retry: the engine accepts a fresh attempt rather than wedging.
        state = accepted(AssociationEngine.identify(state))
        state = accepted(AssociationEngine.requireAssociation(state))
        assertEquals(AssociationState.AssociationRequired, state)
    }

    // --- 11. Unsupported capability ----------------------------------------

    @Test
    fun `only associated or verified states may exchange data`() {
        assertFalse(AssociationState.Identified.canExchangeData)
        assertFalse(AssociationState.AssociationRequired.canExchangeData)
        assertFalse(AssociationState.AssociationRequested.canExchangeData)
        assertFalse(AssociationState.Verifying.canExchangeData)
        assertFalse(AssociationState.CommunicationFailed.canExchangeData)
        assertFalse(AssociationState.Disconnected.canExchangeData)
        assertFalse(AssociationState.Unknown.canExchangeData)

        assertTrue(AssociationState.Associated.canExchangeData)
        assertTrue(AssociationState.Verified.canExchangeData)
    }

    @Test
    fun `being associated is not the same as being verified`() {
        // The distinction the specification insists on: pairing accepted does not
        // mean identity confirmed.
        assertFalse(AssociationState.Associated.isIdentityConfirmed)
        assertTrue(AssociationState.Verified.isIdentityConfirmed)
    }

    // --- 12. Invalid request -----------------------------------------------

    @Test
    fun `association cannot be requested before it is required`() {
        refused(AssociationEngine.requestAssociation(AssociationState.Identified))
    }

    @Test
    fun `association cannot be accepted without an outstanding request`() {
        refused(AssociationEngine.acceptAssociation(AssociationState.Identified))
    }

    @Test
    fun `identity cannot be evaluated outside verification`() {
        refused(
            AssociationEngine.identityReported(
                current = AssociationState.Associated,
                expectedMeterNumber = "TZ-1",
                reportedMeterNumber = "TZ-1",
            ),
        )
    }

    @Test
    fun `disconnect is refused when already disconnected`() {
        refused(AssociationEngine.disconnect(AssociationState.Disconnected))
    }

    @Test
    fun `identifying a second meter requires disconnecting first`() {
        refused(AssociationEngine.identify(AssociationState.Verified))
    }
}
