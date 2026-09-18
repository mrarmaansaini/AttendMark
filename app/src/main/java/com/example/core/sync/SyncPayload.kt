package com.example.core.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val syncCode: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val classes: List<SyncClass> = emptyList(),
    val students: List<SyncStudent> = emptyList(),
    val attendance: List<SyncAttendance> = emptyList(),
    val exams: List<SyncExam> = emptyList(),
    val marks: List<SyncMark> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncClass(
    val id: String,
    val name: String,
    val subject: String,
    val section: String,
    val academicYear: String,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncStudent(
    val id: String,
    val classId: String,
    val name: String,
    val rollNo: String,
    val guardianName: String = "",
    val guardianPhoneE164: String = "",
    val whatsappConsent: Boolean = true,
    val notes: String = "",
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncAttendance(
    val id: String,
    val classId: String,
    val studentId: String,
    val dateIso: String,
    val status: String,
    val remarks: String = "",
    val markedAt: Long = 0L,
    val updatedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncExam(
    val id: String,
    val classId: String,
    val name: String,
    val examType: String,
    val maxMarks: Double,
    val dateIso: String,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncMark(
    val id: String,
    val examId: String,
    val studentId: String,
    val marksObtained: Double,
    val isAbsent: Boolean = false,
    val isExempt: Boolean = false,
    val remarks: String = "",
    val updatedAt: Long = 0L
)
