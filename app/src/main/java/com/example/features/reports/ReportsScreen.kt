package com.example.features.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.model.AttendanceStatus
import com.example.core.repository.AttendMarkRepository
import com.example.features.classes.ClassSwitcherHeader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ReportsScreen(
    repository: AttendMarkRepository,
    activeClass: ClassRoomEntity?,
    allClasses: List<ClassRoomEntity>,
    onSelectClass: (ClassRoomEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val students by (activeClass?.let { repository.getStudentsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val allAttendance by (activeClass?.let { repository.getAllAttendanceForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    // Compute attendance percentage per student
    val studentAttendanceMap = remember(students, allAttendance) {
        val totalSessions = allAttendance.map { it.dateIso }.distinct().size.coerceAtLeast(1)
        students.associateWith { student ->
            val presentSessions = allAttendance.count {
                it.studentId == student.id && (it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name)
            }
            val pct = ((presentSessions.toDouble() / totalSessions.toDouble()) * 100.0).coerceIn(0.0, 100.0)
            Pair(presentSessions, pct)
        }
    }

    fun exportAttendanceCsv() {
        scope.launch {
            try {
                val exportsDir = File(context.filesDir, "exports")
                if (!exportsDir.exists()) exportsDir.mkdirs()

                val file = File(exportsDir, "Attendance_${activeClass?.name ?: "Class"}.csv")
                val fos = FileOutputStream(file)
                val writer = fos.bufferedWriter()

                writer.write("Roll No,Student Name,Guardian Name,Guardian Phone,Attended Days,Attendance Percentage\n")
                studentAttendanceMap.forEach { (student, stats) ->
                    writer.write("\"${student.rollNo}\",\"${student.name}\",\"${student.guardianName}\",\"${student.guardianPhoneE164}\",${stats.first},\"${String.format(java.util.Locale.US, "%.1f", stats.second)}%\"\n")
                }
                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export Attendance CSV"))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to export: ${e.localizedMessage}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ClassSwitcherHeader(
            activeClass = activeClass,
            allClasses = allClasses,
            onSelectClass = onSelectClass,
            onManageClasses = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Export Actions Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attendance Register Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Generate CSV report for official records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Button(
                    onClick = { exportAttendanceCsv() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cumulative Attendance Roster",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                val stats = studentAttendanceMap[student] ?: Pair(0, 0.0)
                val pct = stats.second
                val isLow = pct < 75.0

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(student.name, fontWeight = FontWeight.SemiBold)
                            Text("Roll: ${student.rollNo} • Attended: ${stats.first} sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", pct)}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
