package com.example.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.StudentEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExportHelper {

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /**
     * Generates a sample CSV template for importing students with all required and optional columns.
     */
    fun createStudentTemplateCsv(context: Context): File {
        val exportDir = File(context.cacheDir, "templates")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "attendmark_student_import_template.csv")
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("name,roll_no,guardian_name,guardian_phone,notes\n")
                writer.write("Aarav Sharma,1,Ramesh Sharma,+919876543210,Prefers WhatsApp in evening\n")
                writer.write("Diya Patel,2,Suresh Patel,+919876543211,Maths batch leader\n")
                writer.write("Rohan Gupta,3,Vikas Gupta,+919876543212,Special physics focus\n")
                writer.write("Ananya Singh,4,Rajesh Singh,+919876543213,\n")
            }
        }
        return file
    }

    /**
     * Exports attendance for a given date as CSV.
     */
    fun exportAttendanceCsv(
        context: Context,
        className: String,
        dateIso: String,
        students: List<StudentEntity>,
        attendance: List<AttendanceRecordEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val sanitizeClass = className.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(exportDir, "Attendance_${sanitizeClass}_$dateIso.csv")

        val attendanceMap = attendance.associateBy { it.studentId }

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("Roll No,Student Name,Date,Status,Guardian Name,Guardian Phone,Remarks\n")
                students.forEach { s ->
                    val rec = attendanceMap[s.id]
                    val status = rec?.status ?: "NOT_MARKED"
                    val remarks = rec?.remarks?.replace(",", ";") ?: ""
                    writer.write("\"${s.rollNo}\",\"${s.name}\",\"$dateIso\",\"$status\",\"${s.guardianName}\",\"${s.guardianPhoneE164}\",\"$remarks\"\n")
                }
            }
        }
        return file
    }

    /**
     * Exports exam marks for a single exam as CSV.
     */
    fun exportExamMarksCsv(
        context: Context,
        className: String,
        exam: ExamEntity,
        students: List<StudentEntity>,
        marks: List<MarkEntryEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val sanitizeExam = exam.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(exportDir, "Marks_${sanitizeExam}_${exam.dateIso}.csv")

        val marksMap = marks.associateBy { it.studentId }

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("Roll No,Student Name,Exam Name,Max Marks,Marks Obtained,Percentage,Grade,Status\n")
                students.forEach { s ->
                    val m = marksMap[s.id]
                    val status = when {
                        m == null -> "PENDING"
                        m.isAbsent -> "ABSENT"
                        m.isExempt -> "EXEMPT"
                        else -> "EVALUATED"
                    }
                    val obtained = if (m != null && !m.isAbsent && !m.isExempt) m.marksObtained.toString() else "0.0"
                    val pct = if (m != null && !m.isAbsent && !m.isExempt && exam.maxMarks > 0) {
                        String.format("%.1f", (m.marksObtained / exam.maxMarks) * 100)
                    } else "0.0"
                    val grade = if (m != null && !m.isAbsent && !m.isExempt && exam.maxMarks > 0) {
                        GradeCalculator.calculateGrade((m.marksObtained / exam.maxMarks) * 100)
                    } else "N/A"

                    writer.write("\"${s.rollNo}\",\"${s.name}\",\"${exam.name}\",${exam.maxMarks},$obtained,$pct%,\"$grade\",\"$status\"\n")
                }
            }
        }
        return file
    }

    /**
     * Exports a Master Subject-wise / Exam-wise Sheet containing all students and all exams in one table.
     */
    fun exportMasterSubjectWiseCsv(
        context: Context,
        className: String,
        students: List<StudentEntity>,
        exams: List<ExamEntity>,
        allMarks: List<MarkEntryEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val sanitizeClass = className.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(exportDir, "Master_Sheet_${sanitizeClass}.csv")

        // Map: (studentId, examId) -> MarkEntryEntity
        val marksLookup = allMarks.associateBy { "${it.studentId}_${it.examId}" }

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header: Roll No, Student Name, [Exam 1 (Max)], [Exam 2 (Max)], ..., Total Obtained, Total Max, Overall %, Grade
                val examHeaders = exams.joinToString(",") { "\"${it.name} (Max ${it.maxMarks})\"" }
                val header = if (exams.isNotEmpty()) {
                    "Roll No,Student Name,$examHeaders,Total Obtained,Total Max,Overall %,Overall Grade\n"
                } else {
                    "Roll No,Student Name,Total Obtained,Total Max,Overall %,Overall Grade\n"
                }
                writer.write(header)

                val totalPossibleMarks = exams.sumOf { it.maxMarks }

                students.forEach { student ->
                    var studentTotalObtained = 0.0
                    val examScores = exams.map { exam ->
                        val entry = marksLookup["${student.id}_${exam.id}"]
                        when {
                            entry == null -> "N/A"
                            entry.isAbsent -> "AB"
                            entry.isExempt -> "EX"
                            else -> {
                                studentTotalObtained += entry.marksObtained
                                entry.marksObtained.toString()
                            }
                        }
                    }

                    val overallPct = if (totalPossibleMarks > 0) {
                        (studentTotalObtained / totalPossibleMarks) * 100
                    } else 0.0

                    val grade = if (totalPossibleMarks > 0) {
                        GradeCalculator.calculateGrade(overallPct)
                    } else "N/A"

                    val scoresJoined = if (examScores.isNotEmpty()) examScores.joinToString(",") + "," else ""
                    writer.write("\"${student.rollNo}\",\"${student.name}\",$scoresJoined$studentTotalObtained,$totalPossibleMarks,${String.format("%.1f", overallPct)}%,\"$grade\"\n")
                }
            }
        }
        return file
    }
}
