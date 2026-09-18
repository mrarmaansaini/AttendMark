package com.example.core.model

enum class AttendanceStatus(val code: String, val displayName: String) {
    PRESENT("P", "Present"),
    ABSENT("A", "Absent"),
    LATE("L", "Late"),
    LEAVE("LV", "Leave"),
    HOLIDAY("H", "Holiday")
}

enum class ExamType(val displayName: String) {
    QUIZ("Quiz"),
    UNIT_TEST("Unit Test"),
    MID_TERM("Mid Term"),
    FINAL_EXAM("Final Exam"),
    ASSIGNMENT("Assignment")
}

enum class WhatsAppTargetApp(val packageName: String, val displayName: String) {
    STANDARD("com.whatsapp", "WhatsApp"),
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business"),
    ANY("", "Auto Detect / Browser")
}

enum class QueueItemStatus {
    PENDING,
    OPENED,
    SENT,
    SKIPPED
}

data class CountryCode(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String
)

object CountryCodes {
    val list = listOf(
        CountryCode("IN", "India", "+91", "🇮🇳"),
        CountryCode("US", "United States", "+1", "🇺🇸"),
        CountryCode("GB", "United Kingdom", "+44", "🇬🇧"),
        CountryCode("CA", "Canada", "+1", "🇨🇦"),
        CountryCode("AU", "Australia", "+61", "🇦🇺"),
        CountryCode("AE", "United Arab Emirates", "+971", "🇦🇪"),
        CountryCode("PK", "Pakistan", "+92", "🇵🇰"),
        CountryCode("BD", "Bangladesh", "+880", "🇧🇩"),
        CountryCode("NP", "Nepal", "+977", "🇳🇵"),
        CountryCode("LK", "Sri Lanka", "+94", "🇱🇰"),
        CountryCode("SG", "Singapore", "+65", "🇸🇬"),
        CountryCode("MY", "Malaysia", "+60", "🇲🇾"),
        CountryCode("ZA", "South Africa", "+27", "🇿🇦"),
        CountryCode("NG", "Nigeria", "+234", "🇳🇬"),
        CountryCode("KE", "Kenya", "+254", "🇰🇪")
    )
    val default = list.first { it.code == "IN" }
}
