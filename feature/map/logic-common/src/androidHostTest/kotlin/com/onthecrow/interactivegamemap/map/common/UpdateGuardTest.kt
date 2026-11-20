package com.onthecrow.interactivegamemap.map.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateGuardTest {
    @Test
    fun `an empty update is rejected`() {
        assertEquals("empty", UpdateGuard.rejectReason(newCount = 0, currentCount = null))
    }

    @Test
    fun `an update keeping less than half is rejected`() {
        assertEquals("shrank from 1000 to 400", UpdateGuard.rejectReason(newCount = 400, currentCount = 1000))
    }

    @Test
    fun `a first update and a usual one pass`() {
        assertNull(UpdateGuard.rejectReason(newCount = 10, currentCount = null))
        assertNull(UpdateGuard.rejectReason(newCount = 990, currentCount = 1000))
        assertNull(UpdateGuard.rejectReason(newCount = 500, currentCount = 1000))
    }
}
