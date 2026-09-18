package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.AttendanceDao
import com.example.core.database.dao.ClassDao
import com.example.core.database.dao.ExamDao
import com.example.core.database.dao.MarkDao
import com.example.core.database.dao.MessageLogDao
import com.example.core.database.dao.MessageTemplateDao
import com.example.core.database.dao.SendQueueDao
import com.example.core.database.dao.StudentDao
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.MessageLogEntity
import com.example.core.database.entity.MessageTemplateEntity
import com.example.core.database.entity.SendQueueItemEntity
import com.example.core.database.entity.StudentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ClassRoomEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        ExamEntity::class,
        MarkEntryEntity::class,
        SendQueueItemEntity::class,
        MessageLogEntity::class,
        MessageTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AttendMarkDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun examDao(): ExamDao
    abstract fun markDao(): MarkDao
    abstract fun sendQueueDao(): SendQueueDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun messageTemplateDao(): MessageTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AttendMarkDatabase? = null

        fun getDatabase(context: Context): AttendMarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendMarkDatabase::class.java,
                    "attendmark_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default templates
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.messageTemplateDao()?.insertTemplates(defaultTemplates)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val defaultTemplates = listOf(
            // English Templates
            MessageTemplateEntity(
                id = "tpl_absent_en",
                templateType = "ABSENCE",
                languageCode = "en",
                title = "Absence Alert (English)",
                bodyTemplate = "Dear Guardian, please be informed that your child {student_name} (Roll: {roll_no}) was marked ABSENT for {class} on {date}. Kindly reply with the reason.\n\n- {teacher_name}, {school_name}"
            ),
            MessageTemplateEntity(
                id = "tpl_result_en",
                templateType = "RESULT",
                languageCode = "en",
                title = "Exam Result (English)",
                bodyTemplate = "Dear Guardian, result for {student_name} in {exam} ({class}):\nMarks: {marks}/{max_marks} ({percentage}%)\nGrade: {grade} | Rank: {rank}\n\n- {teacher_name}, {school_name}"
            ),
            MessageTemplateEntity(
                id = "tpl_monthly_en",
                templateType = "MONTHLY_SUMMARY",
                languageCode = "en",
                title = "Monthly Attendance Summary",
                bodyTemplate = "Dear Guardian, monthly attendance update for {student_name} ({class}):\nAttendance: {percentage}%\nClasses Attended: {attended_days}/{total_days} days.\n\n- {teacher_name}, {school_name}"
            ),
            MessageTemplateEntity(
                id = "tpl_low_en",
                templateType = "LOW_ATTENDANCE",
                languageCode = "en",
                title = "Low Attendance Warning",
                bodyTemplate = "URGENT: Dear Guardian, {student_name}'s attendance in {class} has dropped to {percentage}%, which is below the mandatory 75% threshold. Please meet the teacher.\n\n- {teacher_name}, {school_name}"
            ),

            // Hindi Templates (हिन्दी)
            MessageTemplateEntity(
                id = "tpl_absent_hi",
                templateType = "ABSENCE",
                languageCode = "hi",
                title = "अनुपस्थिति सूचना (हिन्दी)",
                bodyTemplate = "आदरणीय अभिभावक, सूचित किया जाता है कि आपका बच्चा {student_name} (रोल: {roll_no}) आज दिनांक {date} को {class} में अनुपस्थित (ABSENT) रहा। कृपया अनुपस्थिति का कारण बताएं।\n\n- {teacher_name}, {school_name}"
            ),
            MessageTemplateEntity(
                id = "tpl_result_hi",
                templateType = "RESULT",
                languageCode = "hi",
                title = "परीक्षा परिणाम (हिन्दी)",
                bodyTemplate = "आदरणीय अभिभावक, {exam} ({class}) में {student_name} का परिणाम:\nअंक: {marks}/{max_marks} ({percentage}%)\nग्रेड: {grade} | रैंक: {rank}\n\n- {teacher_name}, {school_name}"
            ),

            // Punjabi Templates (ਪੰਜਾਬੀ)
            MessageTemplateEntity(
                id = "tpl_absent_pa",
                templateType = "ABSENCE",
                languageCode = "pa",
                title = "ਗੈਰ-ਹਾਜ਼ਰੀ ਸੂਚਨਾ (ਪੰਜਾਬੀ)",
                bodyTemplate = "ਸਤਿਕਾਰਯੋਗ ਮਾਪੇ, ਸੂਚਿਤ ਕੀਤਾ ਜਾਂਦਾ ਹੈ ਕਿ ਤੁਹਾਡਾ ਬੱਚਾ {student_name} (ਰੋਲ: {roll_no}) ਅੱਜ {date} ਨੂੰ {class} ਵਿੱਚ ਗੈਰ-ਹਾਜ਼ਰ (ABSENT) ਰਿਹਾ। ਕਿਰਪਾ ਕਰਕੇ ਕਾਰਨ ਦੱਸੋ।\n\n- {teacher_name}, {school_name}"
            ),
            MessageTemplateEntity(
                id = "tpl_result_pa",
                templateType = "RESULT",
                languageCode = "pa",
                title = "ਇਮਤਿਹਾਨ ਨਤੀਜਾ (ਪੰਜਾਬੀ)",
                bodyTemplate = "ਸਤਿਕਾਰਯੋਗ ਮਾਪੇ, {exam} ({class}) ਵਿੱਚ {student_name} ਦਾ ਨਤੀਜਾ:\nਨੰਬਰ: {marks}/{max_marks} ({percentage}%)\nਗ੍ਰੇਡ: {grade} | ਰੈਂਕ: {rank}\n\n- {teacher_name}, {school_name}"
            )
        )
    }
}
