package com.example.core.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

object PhoneValidator {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    data class ValidationResult(
        val isValid: Boolean,
        val e164Number: String = "",
        val formattedNational: String = "",
        val errorMessage: String? = null
    )

    fun validate(phoneNumber: String, defaultRegion: String = "IN"): ValidationResult {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "Phone number is required")
        }

        return try {
            val parsed: Phonenumber.PhoneNumber = phoneUtil.parse(trimmed, defaultRegion)
            val isValid = phoneUtil.isValidNumber(parsed) || phoneUtil.isPossibleNumber(parsed)
            if (isValid) {
                val e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                val national = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
                ValidationResult(
                    isValid = true,
                    e164Number = e164,
                    formattedNational = national
                )
            } else {
                val digitsOnly = trimmed.filter { it.isDigit() }
                if (digitsOnly.length in 7..15) {
                    ValidationResult(
                        isValid = true,
                        e164Number = if (trimmed.startsWith("+")) trimmed else "+$trimmed",
                        formattedNational = trimmed
                    )
                } else {
                    ValidationResult(
                        isValid = false,
                        errorMessage = "Please enter a valid phone number (7-15 digits)"
                    )
                }
            }
        } catch (e: NumberParseException) {
            val digitsOnly = trimmed.filter { it.isDigit() }
            if (digitsOnly.length in 7..15) {
                ValidationResult(
                    isValid = true,
                    e164Number = if (trimmed.startsWith("+")) trimmed else "+$trimmed",
                    formattedNational = trimmed
                )
            } else {
                ValidationResult(
                    isValid = false,
                    errorMessage = "Please enter a valid phone number"
                )
            }
        }
    }

    /**
     * Strips '+' prefix from E.164 for wa.me link: wa.me/<country_code><number>
     */
    fun toWaMeFormat(e164Number: String): String {
        return e164Number.removePrefix("+").replace(Regex("[^0-9]"), "")
    }
}
