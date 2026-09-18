package com.example.core.sync

import android.content.Context
import android.util.Log
import com.example.core.database.AttendMarkDatabase
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.StudentEntity
import com.example.core.datastore.PreferencesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val message: String, val classesCount: Int, val studentsCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class SyncManager(
    context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val db = AttendMarkDatabase.getDatabase(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(SyncPayload::class.java)

    // Free, zero-setup reliable cloud key-value endpoint for sync
    // Uses kvdb.io (Free public Redis/Key-Value REST bucket with CORS support)
    // Keys are isolated by the teacher's secret 6-char or 8-char sync code
    private val bucketBaseUrl = "https://kvdb.io/4y9hN6Fk2XvH4X9gZ9L1pA" // Fallback bucket namespace

    companion object {
        private const val CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // unambiguous chars
        fun generateSyncCode(): String {
            val random = SecureRandom()
            val sb = StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(CHARSET[random.nextInt(CHARSET.length)])
            }
            return sb.toString()
        }
    }

    suspend fun exportPayload(syncCode: String): SyncPayload = withContext(Dispatchers.IO) {
        val classes = db.classDao().getAllClassesSync().map {
            SyncClass(
                id = it.id,
                name = it.name,
                subject = it.subject,
                section = it.section,
                academicYear = it.academicYear,
                isArchived = it.isArchived,
                createdAt = it.createdAt
            )
        }

        val students = db.studentDao().getAllStudentsSync().map {
            SyncStudent(
                id = it.id,
                classId = it.classId,
                name = it.name,
                rollNo = it.rollNo,
                guardianName = it.guardianName,
                guardianPhoneE164 = it.guardianPhoneE164,
                whatsappConsent = it.whatsappConsent,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }

        val attendance = db.attendanceDao().getAllAttendanceSync().map {
            SyncAttendance(
                id = it.id,
                classId = it.classId,
                studentId = it.studentId,
                dateIso = it.dateIso,
                status = it.status,
                remarks = it.remarks,
                markedAt = it.markedAt,
                updatedAt = it.updatedAt
            )
        }

        val exams = db.examDao().getAllExamsSync().map {
            SyncExam(
                id = it.id,
                classId = it.classId,
                name = it.name,
                examType = it.examType,
                maxMarks = it.maxMarks,
                dateIso = it.dateIso,
                createdAt = it.createdAt
            )
        }

        val marks = db.markDao().getAllMarksSync().map {
            SyncMark(
                id = it.id,
                examId = it.examId,
                studentId = it.studentId,
                marksObtained = it.marksObtained,
                isAbsent = it.isAbsent,
                isExempt = it.isExempt,
                remarks = it.remarks,
                updatedAt = it.updatedAt
            )
        }

        SyncPayload(
            syncCode = syncCode,
            timestamp = System.currentTimeMillis(),
            classes = classes,
            students = students,
            attendance = attendance,
            exams = exams,
            marks = marks
        )
    }

    suspend fun importPayload(payload: SyncPayload) = withContext(Dispatchers.IO) {
        if (payload.classes.isNotEmpty()) {
            val classEntities = payload.classes.map {
                ClassRoomEntity(
                    id = it.id,
                    name = it.name,
                    subject = it.subject,
                    section = it.section,
                    academicYear = it.academicYear,
                    isArchived = it.isArchived,
                    createdAt = it.createdAt
                )
            }
            db.classDao().insertClasses(classEntities)
        }

        if (payload.students.isNotEmpty()) {
            val studentEntities = payload.students.map {
                StudentEntity(
                    id = it.id,
                    classId = it.classId,
                    name = it.name,
                    rollNo = it.rollNo,
                    guardianName = it.guardianName,
                    guardianPhoneE164 = it.guardianPhoneE164,
                    whatsappConsent = it.whatsappConsent,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }
            db.studentDao().insertStudents(studentEntities)
        }

        if (payload.attendance.isNotEmpty()) {
            val attendanceEntities = payload.attendance.map {
                AttendanceRecordEntity(
                    id = it.id,
                    classId = it.classId,
                    studentId = it.studentId,
                    dateIso = it.dateIso,
                    status = it.status,
                    remarks = it.remarks,
                    markedAt = it.markedAt,
                    updatedAt = it.updatedAt
                )
            }
            db.attendanceDao().insertOrUpdateAttendanceBatch(attendanceEntities)
        }

        if (payload.exams.isNotEmpty()) {
            val examEntities = payload.exams.map {
                ExamEntity(
                    id = it.id,
                    classId = it.classId,
                    name = it.name,
                    examType = it.examType,
                    maxMarks = it.maxMarks,
                    dateIso = it.dateIso,
                    createdAt = it.createdAt
                )
            }
            db.examDao().insertExams(examEntities)
        }

        if (payload.marks.isNotEmpty()) {
            val markEntities = payload.marks.map {
                MarkEntryEntity(
                    id = it.id,
                    examId = it.examId,
                    studentId = it.studentId,
                    marksObtained = it.marksObtained,
                    isAbsent = it.isAbsent,
                    isExempt = it.isExempt,
                    remarks = it.remarks,
                    updatedAt = it.updatedAt
                )
            }
            db.markDao().insertOrUpdateMarksBatch(markEntities)
        }
    }

    suspend fun pushToCloud(syncCode: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val payload = exportPayload(syncCode)
            val json = adapter.toJson(payload)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url("$bucketBaseUrl/$syncCode")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val timeStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date())
                    preferencesManager.saveLastSyncTime(timeStr)
                    SyncResult.Success("Cloud sync uploaded successfully", payload.classes.size, payload.students.size)
                } else {
                    SyncResult.Error("Cloud push failed (Code ${response.code}): ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Push error", e)
            SyncResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun pullFromCloud(syncCode: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$bucketBaseUrl/$syncCode")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (response.code == 404) {
                        SyncResult.Error("No data found for Sync Code $syncCode. Push from web first or upload from phone.")
                    } else {
                        SyncResult.Error("Cloud pull failed (${response.code}): ${response.message}")
                    }
                }

                val bodyStr = response.body?.string() ?: ""
                if (bodyStr.isBlank()) {
                    return@withContext SyncResult.Error("Cloud data is empty")
                }

                val payload = adapter.fromJson(bodyStr)
                    ?: return@withContext SyncResult.Error("Could not parse sync payload")

                importPayload(payload)
                val timeStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date())
                preferencesManager.saveLastSyncTime(timeStr)
                SyncResult.Success("Synced ${payload.classes.size} classes, ${payload.students.size} students", payload.classes.size, payload.students.size)
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Pull error", e)
            SyncResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
