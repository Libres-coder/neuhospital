package com.example.neusoft_hospital.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmsSimulatorTest {
    @Test fun `fixed code returns 123456`() {
        val code = SmsSimulator.generateCode("13800000000")
        assertEquals("123456", code)
    }

    @Test fun `verify returns true for fixed code`() {
        assertTrue(SmsSimulator.verify("13800000000", "123456"))
    }

    @Test fun `verify returns false for wrong code`() {
        assertFalse(SmsSimulator.verify("13800000000", "000000"))
    }
}

private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
private fun assertFalse(b: Boolean) = org.junit.Assert.assertFalse(b)