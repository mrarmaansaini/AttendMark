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
import org.json.JSONObject
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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(SyncPayload::class.java)

    // Standard Firestore REST API endpoint format:
    // https://firestore.googleapis.com/v1/projects/{projectId}/databases/(default)/documents/{collection}/{documentId}
    private fun getFirestoreUrl(projectId: String, documentPath: String, apiKey: String?): String {
        val base = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$documentPath"
        return if (!apiKey.isNullOrBlank()) "$base?key=$apiKey" else base
    }

    suspend fun exportPayload(): SyncPayload = withContext(Dispatchers.IO) {
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
            syncCode = "firebase",
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

    suspend fun pushToFirebase(projectId: String, apiKey: String): SyncResult = withContext(Dispatchers.IO) {
        val cleanProject = projectId.trim()
        if (cleanProject.isBlank()) {
            return@withContext SyncResult.Error("Please enter your Firebase Project ID first")
        }

        try {
            val payload = exportPayload()
            val rawJson = adapter.toJson(payload)

            // Wrap into Firestore document fields structure
            val firestoreBody = JSONObject().apply {
                val fields = JSONObject().apply {
                    put("payloadJson", JSONObject().apply { put("stringValue", rawJson) })
                    put("lastUpdated", JSONObject().apply { put("integerValue", System.currentTimeMillis().toString()) })
                    put("classCount", JSONObject().apply { put("integerValue", payload.classes.size.toString()) })
                    put("studentCount", JSONObject().apply { put("integerValue", payload.students.size.toString()) })
                }
                put("fields", fields)
            }

            val body = firestoreBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = getFirestoreUrl(cleanProject, "attendmark_sync/main_data", apiKey.trim())
            
            // PATCH to update or create document
            val request = Request.Builder()
                .url(url)
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val timeStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date())
                    preferencesManager.saveLastSyncTime(timeStr)
                    SyncResult.Success("Synced with Firebase Firestore!", payload.classes.size, payload.students.size)
                } else {
                    val errBody = response.body?.string() ?: ""
                    Log.e("SyncManager", "Firebase push error: ${response.code} $errBody")
                    SyncResult.Error("Firebase upload failed (${response.code}). Check Project ID / Rules in Firebase.")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Push error", e)
            SyncResult.Error("Connection error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun pullFromFirebase(projectId: String, apiKey: String): SyncResult = withContext(Dispatchers.IO) {
        val cleanProject = projectId.trim()
        if (cleanProject.isBlank()) {
            return@withContext SyncResult.Error("Please enter your Firebase Project ID first")
        }

        try {
            val url = getFirestoreUrl(cleanProject, "attendmark_sync/main_data", apiKey.trim())
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (response.code == 404) {
                        SyncResult.Error("No data found in Firebase for '$cleanProject'. Tap 'Upload to Firebase' on Web or Phone first.")
                    } else {
                        val err = response.body?.string() ?: ""
                        Log.e("SyncManager", "Firebase pull error: ${response.code} $err")
                        SyncResult.Error("Firebase pull failed (${response.code}). Check Project ID / Permissions.")
                    }
                }

                val bodyStr = response.body?.string() ?: ""
                val jsonDoc = JSONObject(bodyStr)
                val fields = jsonDoc.optJSONObject("fields")
                val payloadJson = fields?.optJSONObject("payloadJson")?.optString("stringValue")

                if (payloadJson.isNullOrBlank()) {
                    return@withContext SyncResult.Error("Firebase data is empty or invalid format")
                }

                val payload = adapter.fromJson(payloadJson)
                    ?: return@withContext SyncResult.Error("Could not parse payload from Firebase")

                importPayload(payload)
                val timeStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date())
                preferencesManager.saveLastSyncTime(timeStr)
                SyncResult.Success("Synced ${payload.classes.size} classes, ${payload.students.size} students from Firebase", payload.classes.size, payload.students.size)
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Pull error", e)
            SyncResult.Error("Connection error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
