package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.MessageLogEntity
import com.example.core.database.entity.MessageTemplateEntity
import com.example.core.database.entity.SendQueueItemEntity
import com.example.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE is_archived = 0 ORDER BY created_at DESC")
    fun getAllActiveClasses(): Flow<List<ClassRoomEntity>>

    @Query("SELECT * FROM classes ORDER BY created_at DESC")
    suspend fun getAllClassesSync(): List<ClassRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassRoomEntity>)

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: String): ClassRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classRoom: ClassRoomEntity)

    @Update
    suspend fun updateClass(classRoom: ClassRoomEntity)

    @Delete
    suspend fun deleteClass(classRoom: ClassRoomEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY CAST(roll_no AS INTEGER) ASC, name ASC")
    fun getStudentsByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT COUNT(*) FROM students WHERE class_id = :classId")
    fun getStudentCountFlow(classId: String): Flow<Int>

    @Query("SELECT * FROM students ORDER BY CAST(roll_no AS INTEGER) ASC, name ASC")
    suspend fun getAllStudentsSync(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE class_id = :classId AND date_iso = :dateIso")
    fun getAttendanceForDate(classId: String, dateIso: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE class_id = :classId AND date_iso = :dateIso")
    suspend fun getAttendanceForDateSync(classId: String, dateIso: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE student_id = :studentId")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE class_id = :classId")
    fun getAllAttendanceForClass(classId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceSync(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendanceBatch(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM attendance_records WHERE class_id = :classId AND date_iso = :dateIso")
    suspend fun deleteAttendanceForDate(classId: String, dateIso: String)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE class_id = :classId ORDER BY date_iso DESC, created_at DESC")
    fun getExamsForClass(classId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY date_iso DESC, created_at DESC")
    suspend fun getAllExamsSync(): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getExamById(examId: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}

@Dao
interface MarkDao {
    @Query("SELECT * FROM mark_entries WHERE exam_id = :examId")
    fun getMarksForExam(examId: String): Flow<List<MarkEntryEntity>>

    @Query("SELECT * FROM mark_entries WHERE exam_id = :examId")
    suspend fun getMarksForExamSync(examId: String): List<MarkEntryEntity>

    @Query("SELECT * FROM mark_entries WHERE student_id = :studentId")
    fun getMarksForStudent(studentId: String): Flow<List<MarkEntryEntity>>

    @Query("SELECT m.* FROM mark_entries m INNER JOIN exams e ON m.exam_id = e.id WHERE e.class_id = :classId")
    fun getAllMarksForClass(classId: String): Flow<List<MarkEntryEntity>>

    @Query("SELECT m.* FROM mark_entries m INNER JOIN exams e ON m.exam_id = e.id WHERE e.class_id = :classId")
    suspend fun getAllMarksForClassSync(classId: String): List<MarkEntryEntity>

    @Query("SELECT * FROM mark_entries")
    suspend fun getAllMarksSync(): List<MarkEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMark(markEntry: MarkEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMarksBatch(markEntries: List<MarkEntryEntity>)
}

@Dao
interface SendQueueDao {
    @Query("SELECT * FROM send_queue_items WHERE batch_id = :batchId ORDER BY sequence_index ASC")
    fun getQueueItemsForBatch(batchId: String): Flow<List<SendQueueItemEntity>>

    @Query("SELECT * FROM send_queue_items WHERE batch_id = :batchId ORDER BY sequence_index ASC")
    suspend fun getQueueItemsForBatchSync(batchId: String): List<SendQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<SendQueueItemEntity>)

    @Update
    suspend fun updateQueueItem(item: SendQueueItemEntity)

    @Query("UPDATE send_queue_items SET queue_status = :status, opened_at = :openedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, openedAt: Long?)
}

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE student_id = :studentId ORDER BY timestamp DESC")
    fun getLogsForStudent(studentId: String): Flow<List<MessageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MessageLogEntity)
}

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates WHERE template_type = :type AND language_code = :lang LIMIT 1")
    suspend fun getTemplate(type: String, lang: String): MessageTemplateEntity?

    @Query("SELECT * FROM message_templates")
    fun getAllTemplates(): Flow<List<MessageTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MessageTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: MessageTemplateEntity)
}
