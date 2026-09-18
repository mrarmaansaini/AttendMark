package com.example.core.util

import java.util.Locale

object GradeCalculator {
    data class GradeResult(
        val percentage: Double,
        val grade: String,
        val isPassed: Boolean
    )

    fun calculateGrade(marksObtained: Double, maxMarks: Double): GradeResult {
        if (maxMarks <= 0.0) return GradeResult(0.0, "N/A", false)
        val pct = ((marksObtained / maxMarks) * 100.0).coerceIn(0.0, 100.0)
        val roundedPct = String.format(Locale.US, "%.1f", pct).toDouble()

        val grade = calculateGrade(roundedPct)

        return GradeResult(
            percentage = roundedPct,
            grade = grade,
            isPassed = roundedPct >= 33.0
        )
    }

    fun calculateGrade(percentage: Double): String {
        return when {
            percentage >= 90.0 -> "A+"
            percentage >= 80.0 -> "A"
            percentage >= 70.0 -> "B+"
            percentage >= 60.0 -> "B"
            percentage >= 50.0 -> "C"
            percentage >= 40.0 -> "D"
            percentage >= 33.0 -> "E"
            else -> "F"
        }
    }

    data class StudentRank(
        val studentId: String,
        val rank: Int,
        val marks: Double,
        val percentage: Double,
        val grade: String
    )

    fun computeRanks(marksMap: Map<String, Double>, maxMarks: Double): Map<String, StudentRank> {
        val sortedList = marksMap.entries
            .sortedByDescending { it.value }

        val resultMap = mutableMapOf<String, StudentRank>()
        var currentRank = 1
        var previousMarks: Double? = null
        var rankIncrement = 0

        for ((index, entry) in sortedList.withIndex()) {
            val studentId = entry.key
            val marks = entry.value
            if (previousMarks != null && marks < previousMarks) {
                currentRank += rankIncrement
                rankIncrement = 1
            } else if (previousMarks != null && marks == previousMarks) {
                rankIncrement++
            } else {
                rankIncrement = 1
            }
            previousMarks = marks

            val gradeRes = calculateGrade(marks, maxMarks)
            resultMap[studentId] = StudentRank(
                studentId = studentId,
                rank = currentRank,
                marks = marks,
                percentage = gradeRes.percentage,
                grade = gradeRes.grade
            )
        }

        return resultMap
    }
}
