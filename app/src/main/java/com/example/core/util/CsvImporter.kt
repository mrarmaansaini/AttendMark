package com.example.core.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object CsvImporter {
    data class ParsedStudent(
        val name: String,
        val rollNo: String,
        val guardianName: String,
        val guardianPhone: String
    )

    fun parseCsv(inputStream: InputStream): List<ParsedStudent> {
        val result = mutableListOf<ParsedStudent>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = reader.readLine()

        // Detect header row if present
        var isFirstRow = true
        var nameCol = 0
        var rollCol = 1
        var guardianCol = 2
        var phoneCol = 3

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val tokens = trimmed.split(",").map { it.trim().removeSurrounding("\"") }
                if (isFirstRow) {
                    isFirstRow = false
                    val lower = tokens.map { it.lowercase() }
                    if (lower.any { it.contains("name") || it.contains("roll") || it.contains("phone") }) {
                        // Found header row, map columns
                        lower.forEachIndexed { idx, header ->
                            when {
                                header.contains("roll") -> rollCol = idx
                                header.contains("guardian") || header.contains("parent") -> guardianCol = idx
                                header.contains("phone") || header.contains("mobile") || header.contains("whatsapp") -> phoneCol = idx
                                header.contains("name") && !header.contains("guardian") -> nameCol = idx
                            }
                        }
                        line = reader.readLine()
                        continue
                    }
                }

                if (tokens.size > nameCol && tokens[nameCol].isNotEmpty()) {
                    val name = tokens[nameCol]
                    val roll = if (tokens.size > rollCol) tokens[rollCol] else (result.size + 1).toString()
                    val guardian = if (tokens.size > guardianCol) tokens[guardianCol] else ""
                    val phone = if (tokens.size > phoneCol) tokens[phoneCol] else ""

                    result.add(
                        ParsedStudent(
                            name = name,
                            rollNo = roll,
                            guardianName = guardian,
                            guardianPhone = phone
                        )
                    )
                }
            }
            line = reader.readLine()
        }
        return result
    }
}
