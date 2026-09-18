package com.example

import com.example.core.util.GradeCalculator
import com.example.core.util.PhoneValidator
import com.example.core.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendMarkUnitTest {

    @Test
    fun testPhoneValidator_validIndiaNumber() {
        val result = PhoneValidator.validate("+919876543210", "IN")
        assertTrue(result.isValid)
        assertEquals("+919876543210", result.e164Number)
    }

    @Test
    fun testPhoneValidator_invalidNumber() {
        val result = PhoneValidator.validate("123", "IN")
        assertFalse(result.isValid)
    }

    @Test
    fun testGradeCalculator() {
        val gradeA = GradeCalculator.calculateGrade(85.0, 100.0)
        assertEquals("A", gradeA.grade)
        assertEquals(85.0, gradeA.percentage, 0.01)
        assertTrue(gradeA.isPassed)

        val gradeFail = GradeCalculator.calculateGrade(20.0, 100.0)
        assertEquals("F", gradeFail.grade)
        assertFalse(gradeFail.isPassed)
    }

    @Test
    fun testRankCalculation() {
        val marks = mapOf(
            "student_1" to 95.0,
            "student_2" to 75.0,
            "student_3" to 85.0
        )
        val ranks = GradeCalculator.computeRanks(marks, 100.0)
        assertEquals(1, ranks["student_1"]?.rank)
        assertEquals(2, ranks["student_3"]?.rank)
        assertEquals(3, ranks["student_2"]?.rank)
    }

    @Test
    fun testWhatsAppUrlEncoding() {
        val phone = "919876543210"
        val message = "Hello Teacher! नमस्ते"
        val url = WhatsAppHelper.buildWaMeUrl(phone, message)
        assertTrue(url.startsWith("https://wa.me/919876543210?text="))
        assertTrue(url.contains("Hello%20Teacher!"))
    }
}
