package com.example.neusoft_hospital.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdCardValidatorTest {
    @Test fun `valid id card returns true`() {
        // 11010519491231002X (校验位算法生成的合法示例)
        assertTrue(IdCardValidator.isValid("11010519491231002X"))
    }

    @Test fun `invalid id card returns false`() {
        assertFalse(IdCardValidator.isValid("110105194912310020"))
    }

    @Test fun `short id card returns false`() {
        assertFalse(IdCardValidator.isValid("123"))
    }

    @Test fun `birth date extracted correctly`() {
        val date = IdCardValidator.extractBirthDate("11010519491231002X")
        assertTrue(date == "1949-12-31")
    }
}