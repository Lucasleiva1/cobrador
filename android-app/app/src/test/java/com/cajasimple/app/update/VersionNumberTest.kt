package com.cajasimple.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionNumberTest {
    @Test fun `detecta una version superior`() {
        assertTrue(VersionNumber.isNewer("v1.2.0", "1.1.9"))
    }

    @Test fun `no ofrece la misma version`() {
        assertFalse(VersionNumber.isNewer("1.1.0", "1.1.0"))
    }

    @Test fun `no ofrece una version anterior`() {
        assertFalse(VersionNumber.isNewer("1.0.9", "1.1.0"))
    }
}
