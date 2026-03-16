package org.dazn.simpletvapp.presentation.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsVisibilityTest {

    @Test
    fun visibleWhenExplicitlyRequestedAtNormalSpeed() {
        assertTrue(controlsVisible(requested = true, playbackSpeed = 1f))
    }

    @Test
    fun hiddenWhenNotRequestedAtNormalSpeed() {
        assertFalse(controlsVisible(requested = false, playbackSpeed = 1f))
    }

    @Test
    fun visibleDuringFastForwardEvenWhenNotRequested() {
        assertTrue(controlsVisible(requested = false, playbackSpeed = 15f))
    }

    @Test
    fun visibleDuringHighSpeedFastForwardEvenWhenNotRequested() {
        assertTrue(controlsVisible(requested = false, playbackSpeed = 130f))
    }

    @Test
    fun visibleDuringRewindEvenWhenNotRequested() {
        assertTrue(controlsVisible(requested = false, playbackSpeed = -15f))
    }

    @Test
    fun visibleDuringHighSpeedRewindEvenWhenNotRequested() {
        assertTrue(controlsVisible(requested = false, playbackSpeed = -130f))
    }

    @Test
    fun visibleWhenBothRequestedAndInTrickPlay() {
        assertTrue(controlsVisible(requested = true, playbackSpeed = 45f))
    }
}

class ParseAspectRatioTest {

    @Test
    fun parsesWidescreen() {
        assertEquals(16f / 9f, parseAspectRatio("16:9")!!, 0.001f)
    }

    @Test
    fun parsesStandardDef() {
        assertEquals(4f / 3f, parseAspectRatio("4:3")!!, 0.001f)
    }

    @Test
    fun returnsNullForNull() {
        assertNull(parseAspectRatio(null))
    }

    @Test
    fun returnsNullForMalformedSeparator() {
        assertNull(parseAspectRatio("16x9"))
    }

    @Test
    fun returnsNullForSingleSegment() {
        assertNull(parseAspectRatio("16"))
    }

    @Test
    fun returnsNullForNonNumericParts() {
        assertNull(parseAspectRatio("abc:def"))
    }

    @Test
    fun returnsNullForZeroHeight() {
        assertNull(parseAspectRatio("16:0"))
    }
}
