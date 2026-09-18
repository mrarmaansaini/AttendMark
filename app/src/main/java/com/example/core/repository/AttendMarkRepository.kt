package com.example.core.repository

import android.content.Context
import com.example.core.database.AttendMarkDatabase
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.MessageLogEntity
import com.example.core.database.entity.MessageTemplateEntity
import com.example.core.database.entity.SendQueueItemEntity
import com.example.core.database.entity.StudentEntity
import com.example.core.datastore.PreferencesManager
import com.example.core.datastore.TeacherProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendMarkRepository(context: Context) {
    private val db = AttendMarkDatabase.getDatabase(context)
    private val classDao = db.classDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val examDao = db.examDao()
    private val markDao = db.markDao()
    private val sendQueueDao = db.sendQueueDao()
    private val messageLogDao = db.messageLogDao()
    private val templateDao = db.messageTemplateDao()
    val preferencesManager = PreferencesManager(context)

    // Profile & Preferences
    val profileFlow: Flow<TeacherProfile> = preferencesManager.profileFlow
    val activeClassIdFlow: Flow<String?> = preferencesManager.activeClassIdFlow

    suspend fun saveProfile(profile: TeacherProfile) = preferencesManager.saveProfile(profile)
    suspend fun setActiveClassId(classId: String) = preferencesManager.setActiveClassId(classId)

    // Classes
    val activeClasses: Flow<List<ClassRoomEntity>> = classDao.getAllActiveClasses()

    suspend fun createClass(name: String, subject: String, section: String, academicYear: String): String {
        val id = UUID.randomUUID().toString()
        val entity = ClassRoomEntity(
            id = id,
            name = name,
            subject = subject,
            section = section,
            academicYear = academicYear
        )
        classDao.insertClass(entity)
        return id
    }

    suspend fun updateClass(classRoom: ClassRoomEntity) = classDao.updateClass(classRoom)
    suspend fun deleteClass(classRoom: ClassRoomEntity) = classDao.deleteClass(classRoom)
    suspend fun deleteClassById(classId: String) {
        classDao.deleteClassById(classId)
        val currentActive = activeClassIdFlow.first()
        if (currentActive == classId) {
            val remaining = activeClasses.first()
            val nextActive = remaining.firstOrNull { it.id != classId }?.id
            if (nextActive != null) {
                setActiveClassId(nextActive)
            } else {
                setActiveClassId("")
            }
        }
    }

    // Students
    fun getStudentsForClass(classId: String): Flow<List<StudentEntity>> = studentDao.getStudentsByClass(classId)

    suspend fun addStudent(
        classId: String,
        name: String,
        rollNo: String,
        guardianName: String,
        guardianPhoneE164: String,
        photoUri: String? = null,
        whatsappConsent: Boolean = true,
        notes: String = ""
    ) {
        val entity = StudentEntity(
            id = UUID.randomUUID().toString(),
            classId = classId,
            name = name,
            rollNo = rollNo,
            guardianName = guardianName,
            guardianPhoneE164 = guardianPhoneE164,
            photoUri = photoUri,
            whatsappConsent = whatsappConsent,
            notes = notes
        )
        studentDao.insertStudent(entity)
    }

    suspend fun addStudentsBatch(students: List<StudentEntity>) = studentDao.insertStudents(students)
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)

    // Attendance
    fun getAttendanceForDate(classId: String, dateIso: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAttendanceForDate(classId, dateIso)

    fun getAllAttendanceForClass(classId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAllAttendanceForClass(classId)

    suspend fun markStudentAttendance(classId: String, studentId: String, dateIso: String, status: String) {
        val id = "$classId-$studentId-$dateIso"
        val record = AttendanceRecordEntity(
            id = id,
            classId = classId,
            studentId = studentId,
            dateIso = dateIso,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.insertOrUpdateAttendance(record)
    }

    suspend fun markAllStudents(classId: String, studentIds: List<String>, dateIso: String, status: String) {
        val records = studentIds.map { studentId ->
            AttendanceRecordEntity(
                id = "$classId-$studentId-$dateIso",
                classId = classId,
                studentId = studentId,
                dateIso = dateIso,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        }
        attendanceDao.insertOrUpdateAttendanceBatch(records)
    }

    // Exams & Marks
    fun getExamsForClass(classId: String): Flow<List<ExamEntity>> = examDao.getExamsForClass(classId)

    suspend fun createExam(classId: String, name: String, examType: String, maxMarks: Double, dateIso: String): String {
        val id = UUID.randomUUID().toString()
        val exam = ExamEntity(
            id = id,
            classId = classId,
            name = name,
            examType = examType,
            maxMarks = maxMarks,
            dateIso = dateIso
        )
        examDao.insertExam(exam)
        return id
    }

    suspend fun deleteExam(exam: ExamEntity) = examDao.deleteExam(exam)

    fun getMarksForExam(examId: String): Flow<List<MarkEntryEntity>> = markDao.getMarksForExam(examId)
    fun getAllMarksForClass(classId: String): Flow<List<MarkEntryEntity>> = markDao.getAllMarksForClass(classId)
    suspend fun getAllMarksForClassSync(classId: String): List<MarkEntryEntity> = markDao.getAllMarksForClassSync(classId)

    suspend fun recordMark(
        examId: String,
        studentId: String,
        marksObtained: Double,
        isAbsent: Boolean = false,
        isExempt: Boolean = false,
        remarks: String = ""
    ) {
        val id = "$examId-$studentId"
        val entry = MarkEntryEntity(
            id = id,
            examId = examId,
            studentId = studentId,
            marksObtained = marksObtained,
            isAbsent = isAbsent,
            isExempt = isExempt,
            remarks = remarks
        )
        markDao.insertOrUpdateMark(entry)
    }

    // WhatsApp Send Queue
    fun getQueueItems(batchId: String): Flow<List<SendQueueItemEntity>> = sendQueueDao.getQueueItemsForBatch(batchId)

    suspend fun createSendQueue(
        batchId: String,
        items: List<SendQueueItemEntity>
    ) {
        sendQueueDao.insertQueueItems(items)
    }

    suspend fun updateQueueItemStatus(itemId: String, status: String) {
        sendQueueDao.updateStatus(itemId, status, System.currentTimeMillis())
    }

    // Message Templates & Logs
    fun getAllTemplates(): Flow<List<MessageTemplateEntity>> = templateDao.getAllTemplates()
    suspend fun updateTemplate(template: MessageTemplateEntity) = templateDao.updateTemplate(template)
    fun getRecentLogs(): Flow<List<MessageLogEntity>> = messageLogDao.getRecentLogs()

    suspend fun logMessage(
        studentId: String,
        studentName: String,
        recipientPhoneE164: String,
        templateType: String,
        messageBody: String,
        status: String
    ) {
        val log = MessageLogEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            studentName = studentName,
            recipientPhoneE164 = recipientPhoneE164,
            templateType = templateType,
            messageBody = messageBody,
            status = status
        )
        messageLogDao.insertLog(log)
    }

    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        preferencesManager.clearAllData()
    }
}
