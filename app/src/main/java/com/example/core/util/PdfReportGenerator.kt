package com.example.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {
    fun generateReportCard(
        context: Context,
        studentName: String,
        rollNo: String,
        className: String,
        examName: String,
        marksObtained: Double,
        maxMarks: Double,
        percentage: Double,
        grade: String,
        rank: Int,
        teacherName: String,
        schoolName: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in pt
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()

            // Header Banner
            paint.color = Color.rgb(30, 58, 138) // Deep Blue
            canvas.drawRect(0f, 0f, 595f, 120f, paint)

            // School & Title Text
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText(schoolName.ifEmpty { "AttendMark Academy" }, 40f, 55f, paint)

            paint.textSize = 16f
            paint.isFakeBoldText = false
            canvas.drawText("STUDENT PERFORMANCE REPORT CARD", 40f, 85f, paint)

            // Content Section
            paint.color = Color.rgb(15, 23, 42) // Slate Dark
            paint.textSize = 14f

            var y = 160f
            fun drawRow(label: String, value: String) {
                paint.isFakeBoldText = true
                canvas.drawText(label, 40f, y, paint)
                paint.isFakeBoldText = false
                canvas.drawText(value, 200f, y, paint)
                y += 28f
            }

            drawRow("Student Name:", studentName)
            drawRow("Roll Number:", rollNo)
            drawRow("Class / Batch:", className)
            drawRow("Examination:", examName)

            // Divider
            y += 10f
            paint.color = Color.rgb(203, 213, 225)
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 30f

            // Score Card Box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(40f, y, 555f, y + 140f, 12f, 12f, paint)

            paint.color = Color.rgb(30, 58, 138)
            paint.isFakeBoldText = true
            paint.textSize = 18f
            canvas.drawText("Assessment Results", 60f, y + 35f, paint)

            paint.textSize = 14f
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText("Score Obtained: $marksObtained / $maxMarks", 60f, y + 70f, paint)
            canvas.drawText("Percentage: $percentage%", 60f, y + 100f, paint)

            paint.textSize = 16f
            paint.color = Color.rgb(13, 148, 136) // Emerald
            canvas.drawText("Grade: $grade", 340f, y + 70f, paint)
            paint.color = Color.rgb(217, 119, 6) // Amber
            canvas.drawText("Class Rank: #$rank", 340f, y + 100f, paint)

            y += 180f

            // Signature Footer
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Evaluated & Certified by: $teacherName", 40f, y, paint)
            canvas.drawText("Generated locally via AttendMark - Zero Cloud Leakage", 40f, y + 20f, paint)

            pdfDocument.finishPage(page)

            // Save to internal cache
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "Report_${studentName.replace(" ", "_")}_$examName.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class SubjectScore(
        val subjectOrExamName: String,
        val maxMarks: Double,
        val marksObtained: Double,
        val percentage: Double = if (maxMarks > 0) (marksObtained / maxMarks) * 100.0 else 0.0,
        val grade: String = if (maxMarks > 0) GradeCalculator.calculateGrade(marksObtained, maxMarks).grade else "N/A",
        val statusText: String = if (maxMarks > 0 && (marksObtained / maxMarks) >= 0.33) "Passed" else "Needs Attention"
    )

    /**
     * Generates a single-sheet comprehensive report card containing all marks and all subjects for a student.
     */
    fun generateConsolidatedReportCard(
        context: Context,
        studentName: String,
        rollNo: String,
        className: String,
        academicYear: String,
        scores: List<SubjectScore>,
        teacherName: String,
        schoolName: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Header Banner
            paint.color = Color.rgb(30, 58, 138) // Deep Blue
            canvas.drawRect(0f, 0f, 595f, 110f, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText(schoolName.ifEmpty { "AttendMark Academy" }, 40f, 48f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("CONSOLIDATED STUDENT REPORT CARD • ALL SUBJECTS", 40f, 74f, paint)
            paint.textSize = 11f
            canvas.drawText("Academic Year: $academicYear", 40f, 94f, paint)

            // Student Info Card
            paint.color = Color.rgb(248, 250, 252) // Slate 50
            canvas.drawRoundRect(40f, 130f, 555f, 190f, 8f, 8f, paint)
            paint.color = Color.rgb(203, 213, 225)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(40f, 130f, 555f, 190f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 12f

            paint.isFakeBoldText = true
            canvas.drawText("Student Name:", 55f, 156f, paint)
            paint.isFakeBoldText = false
            canvas.drawText(studentName, 150f, 156f, paint)

            paint.isFakeBoldText = true
            canvas.drawText("Roll Number:", 340f, 156f, paint)
            paint.isFakeBoldText = false
            canvas.drawText(rollNo, 430f, 156f, paint)

            paint.isFakeBoldText = true
            canvas.drawText("Class / Section:", 55f, 178f, paint)
            paint.isFakeBoldText = false
            canvas.drawText(className, 150f, 178f, paint)

            // Table Header
            val tableTop = 210f
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(40f, tableTop, 555f, tableTop + 28f, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            canvas.drawText("SUBJECT / ASSESSMENT", 50f, tableTop + 18f, paint)
            canvas.drawText("MAX", 270f, tableTop + 18f, paint)
            canvas.drawText("OBTAINED", 330f, tableTop + 18f, paint)
            canvas.drawText("PERCENT", 420f, tableTop + 18f, paint)
            canvas.drawText("GRADE", 495f, tableTop + 18f, paint)

            // Rows
            var rowY = tableTop + 28f
            paint.isFakeBoldText = false

            var totalMax = 0.0
            var totalObtained = 0.0

            scores.forEachIndexed { index, score ->
                totalMax += score.maxMarks
                totalObtained += score.marksObtained

                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(40f, rowY, 555f, rowY + 24f, paint)
                }

                paint.color = Color.rgb(15, 23, 42)
                paint.textSize = 10.5f
                val displayName = if (score.subjectOrExamName.length > 32) score.subjectOrExamName.take(30) + ".." else score.subjectOrExamName
                canvas.drawText(displayName, 50f, rowY + 16f, paint)
                canvas.drawText(score.maxMarks.toString(), 270f, rowY + 16f, paint)
                canvas.drawText(score.marksObtained.toString(), 330f, rowY + 16f, paint)
                canvas.drawText("${String.format("%.1f", score.percentage)}%", 420f, rowY + 16f, paint)

                paint.color = Color.rgb(13, 148, 136) // Teal
                paint.isFakeBoldText = true
                canvas.drawText(score.grade, 495f, rowY + 16f, paint)
                paint.isFakeBoldText = false

                // Row border
                paint.color = Color.rgb(241, 245, 249)
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, rowY + 24f, 555f, rowY + 24f, paint)

                rowY += 24f
            }

            // Summary Footer Box
            val summaryTop = rowY + 20f
            paint.color = Color.rgb(238, 242, 255) // Indigo 50
            canvas.drawRoundRect(40f, summaryTop, 555f, summaryTop + 85f, 10f, 10f, paint)

            val overallPct = if (totalMax > 0) (totalObtained / totalMax) * 100 else 0.0
            val overallGrade = if (totalMax > 0) GradeCalculator.calculateGrade(overallPct) else "N/A"

            paint.color = Color.rgb(49, 46, 129) // Indigo 900
            paint.isFakeBoldText = true
            paint.textSize = 13f
            canvas.drawText("Cumulative Academic Summary", 55f, summaryTop + 24f, paint)

            paint.textSize = 11.5f
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("Aggregate Score: $totalObtained / $totalMax", 55f, summaryTop + 50f, paint)
            canvas.drawText("Overall Percentage: ${String.format("%.1f", overallPct)}%", 55f, summaryTop + 70f, paint)

            paint.textSize = 15f
            paint.color = Color.rgb(13, 148, 136)
            canvas.drawText("Overall Grade: $overallGrade", 340f, summaryTop + 50f, paint)
            paint.textSize = 11.5f
            paint.color = if (overallPct >= 40) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            canvas.drawText("Result: " + (if (overallPct >= 40) "PASSED" else "NEEDS ATTENTION"), 340f, summaryTop + 70f, paint)

            // Signature & Date Footer
            val footerY = 760f
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawLine(40f, footerY - 10f, 555f, footerY - 10f, paint)

            canvas.drawText("Class Teacher / Instructor: $teacherName", 40f, footerY + 15f, paint)
            canvas.drawText("Authorized Signature: _______________________", 340f, footerY + 15f, paint)
            canvas.drawText("AttendMark Offline-First Classroom Suite • Zero Cloud Data Leakage", 40f, footerY + 35f, paint)

            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "Report_Card_${studentName.replace(" ", "_")}_AllSubjects.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Student Report Card"))
    }
}
