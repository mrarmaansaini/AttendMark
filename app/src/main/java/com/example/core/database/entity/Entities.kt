package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassRoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val section: String,
    @ColumnInfo(name = "academic_year") val academicYear: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["class_id"]),
        Index(value = ["class_id", "roll_no"])
    ]
)
data class StudentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "class_id") val classId: String,
    val name: String,
    @ColumnInfo(name = "roll_no") val rollNo: String,
    @ColumnInfo(name = "photo_uri") val photoUri: String? = null,
    @ColumnInfo(name = "guardian_name") val guardianName: String = "",
    @ColumnInfo(name = "guardian_phone_e164") val guardianPhoneE164: String = "",
    @ColumnInfo(name = "whatsapp_consent") val whatsappConsent: Boolean = true,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = ClassRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["class_id", "student_id", "date_iso"], unique = true),
        Index(value = ["class_id", "date_iso"]),
        Index(value = ["student_id"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "date_iso") val dateIso: String, // Format: YYYY-MM-DD
    val status: String, // PRESENT, ABSENT, LATE, LEAVE, HOLIDAY
    val remarks: String = "",
    @ColumnInfo(name = "marked_at") val markedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ClassRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["class_id"])]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "class_id") val classId: String,
    val name: String,
    @ColumnInfo(name = "exam_type") val examType: String,
    @ColumnInfo(name = "max_marks") val maxMarks: Double,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mark_entries",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exam_id", "student_id"], unique = true),
        Index(value = ["exam_id"]),
        Index(value = ["student_id"])
    ]
)
data class MarkEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "marks_obtained") val marksObtained: Double = 0.0,
    @ColumnInfo(name = "is_absent") val isAbsent: Boolean = false,
    @ColumnInfo(name = "is_exempt") val isExempt: Boolean = false,
    val remarks: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "send_queue_items",
    indices = [Index(value = ["batch_id"])]
)
data class SendQueueItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "batch_id") val batchId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "recipient_name") val recipientName: String,
    @ColumnInfo(name = "recipient_phone_e164") val recipientPhoneE164: String,
    @ColumnInfo(name = "message_text") val messageText: String,
    @ColumnInfo(name = "queue_status") val queueStatus: String, // PENDING, OPENED, SENT, SKIPPED
    @ColumnInfo(name = "skip_reason") val skipReason: String? = null,
    @ColumnInfo(name = "sequence_index") val sequenceIndex: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "opened_at") val openedAt: Long? = null
)

@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "recipient_phone_e164") val recipientPhoneE164: String,
    @ColumnInfo(name = "template_type") val templateType: String,
    @ColumnInfo(name = "message_body") val messageBody: String,
    @ColumnInfo(name = "status") val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_type") val templateType: String, // ABSENCE, RESULT, MONTHLY_SUMMARY, LOW_ATTENDANCE
    @ColumnInfo(name = "language_code") val languageCode: String, // en, hi, pa
    val title: String,
    @ColumnInfo(name = "body_template") val bodyTemplate: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = true
)
