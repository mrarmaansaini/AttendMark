package com.example.features.attendance

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.entity.AttendanceRecordEntity
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.SendQueueItemEntity
import com.example.core.database.entity.StudentEntity
import com.example.core.model.AttendanceStatus
import com.example.core.repository.AttendMarkRepository
import com.example.core.util.ExportHelper
import com.example.features.classes.ClassSwitcherHeader
import com.example.features.classes.CreateClassDialog
import com.example.features.classes.ManageClassesDialog
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedContainer
import com.example.ui.theme.HolidayPurple
import com.example.ui.theme.HolidayPurpleContainer
import com.example.ui.theme.LateOrange
import com.example.ui.theme.LateOrangeContainer
import com.example.ui.theme.LeaveBlue
import com.example.ui.theme.LeaveBlueContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AttendanceScreen(
    repository: AttendMarkRepository,
    activeClass: ClassRoomEntity?,
    allClasses: List<ClassRoomEntity>,
    onSelectClass: (ClassRoomEntity) -> Unit,
    onNavigateToSendQueue: (batchId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDateIso by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        mutableStateOf(sdf.format(Date()))
    }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showManageClassesDialog by remember { mutableStateOf(false) }

    val students by (activeClass?.let { repository.getStudentsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val attendanceRecords by (activeClass?.let { repository.getAttendanceForDate(it.id, selectedDateIso) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val attendanceMap = remember(attendanceRecords) {
        attendanceRecords.associateBy { it.studentId }
    }

    // Attendance stats
    val presentCount = attendanceRecords.count { it.status == AttendanceStatus.PRESENT.name }
    val absentCount = attendanceRecords.count { it.status == AttendanceStatus.ABSENT.name }
    val lateCount = attendanceRecords.count { it.status == AttendanceStatus.LATE.name }
    val leaveCount = attendanceRecords.count { it.status == AttendanceStatus.LEAVE.name }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val parts = selectedDateIso.split("-")
        if (parts.size == 3) {
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDateIso = newDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showCreateClassDialog) {
        CreateClassDialog(
            onDismissRequest = { showCreateClassDialog = false },
            onClassCreated = { name, subj, sec, yr ->
                scope.launch {
                    val newId = repository.createClass(name, subj, sec, yr)
                    repository.setActiveClassId(newId)
                    showCreateClassDialog = false
                }
            }
        )
    }

    if (showManageClassesDialog) {
        ManageClassesDialog(
            allClasses = allClasses,
            activeClassId = activeClass?.id,
            onSelectClass = { cls ->
                onSelectClass(cls)
                showManageClassesDialog = false
            },
            onDeleteClass = { cls ->
                scope.launch {
                    repository.deleteClassById(cls.id)
                }
            },
            onCreateNewClass = {
                showManageClassesDialog = false
                showCreateClassDialog = true
            },
            onDismissRequest = { showManageClassesDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Class Switcher Header
            ClassSwitcherHeader(
                activeClass = activeClass,
                allClasses = allClasses,
                onSelectClass = onSelectClass,
                onManageClasses = { showManageClassesDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Bar + Export + Mark All Present Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker() },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedDateIso,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            if (activeClass != null && students.isNotEmpty()) {
                                try {
                                    val file = ExportHelper.exportAttendanceCsv(
                                        context = context,
                                        className = activeClass.name,
                                        dateIso = selectedDateIso,
                                        students = students,
                                        attendance = attendanceRecords
                                    )
                                    ExportHelper.shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "text/csv",
                                        chooserTitle = "Export Attendance CSV"
                                    )
                                } catch (e: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}") }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("No records to export") }
                            }
                        },
                        enabled = activeClass != null && students.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (activeClass != null && students.isNotEmpty()) {
                                triggerHapticFeedback()
                                scope.launch {
                                    repository.markAllStudents(
                                        classId = activeClass.id,
                                        studentIds = students.map { it.id },
                                        dateIso = selectedDateIso,
                                        status = AttendanceStatus.PRESENT.name
                                    )
                                    snackbarHostState.showSnackbar("Marked all ${students.size} students Present")
                                }
                            }
                        },
                        enabled = activeClass != null && students.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PresentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Present", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Stats Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip("Present", presentCount, PresentGreen, PresentGreenContainer, Modifier.weight(1f))
                StatChip("Absent", absentCount, AbsentRed, AbsentRedContainer, Modifier.weight(1f))
                StatChip("Late", lateCount, LateOrange, LateOrangeContainer, Modifier.weight(1f))
                StatChip("Leave", leaveCount, LeaveBlue, LeaveBlueContainer, Modifier.weight(1f))
            }

            // Prominent Send WhatsApp Absence Alerts Call-to-action
            if (absentCount > 0 && activeClass != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val profile = repository.profileFlow.first()
                            val batchId = UUID.randomUUID().toString()
                            val absentStudents = students.filter {
                                attendanceMap[it.id]?.status == AttendanceStatus.ABSENT.name
                            }

                            val queueItems = absentStudents.mapIndexed { index, student ->
                                val isValidPhone = student.guardianPhoneE164.isNotBlank() && student.whatsappConsent
                                val msgText = "Dear Guardian, please be informed that your child ${student.name} (Roll: ${student.rollNo}) was marked ABSENT for ${activeClass.name} on $selectedDateIso. Kindly reply with the reason.\n\n- ${profile.fullName}, ${profile.schoolName}"

                                SendQueueItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    batchId = batchId,
                                    studentId = student.id,
                                    studentName = student.name,
                                    recipientName = student.guardianName.ifEmpty { "Guardian" },
                                    recipientPhoneE164 = student.guardianPhoneE164,
                                    messageText = msgText,
                                    queueStatus = if (isValidPhone) "PENDING" else "SKIPPED",
                                    skipReason = if (!isValidPhone) "No valid phone or consent" else null,
                                    sequenceIndex = index
                                )
                            }

                            repository.createSendQueue(batchId, queueItems)
                            onNavigateToSendQueue(batchId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Send $absentCount Absence Alerts via WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Attendance Marking List
            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeClass == null) "Select a class" else "No students in this class.\nAdd students from the Students tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val currentRecord = attendanceMap[student.id]
                        val currentStatus = currentRecord?.status?.let {
                            try { AttendanceStatus.valueOf(it) } catch (_: Exception) { null }
                        }

                        AttendanceStudentItem(
                            student = student,
                            currentStatus = currentStatus,
                            onStatusSelected = { newStatus ->
                                triggerHapticFeedback()
                                scope.launch {
                                    if (activeClass != null) {
                                        repository.markStudentAttendance(
                                            classId = activeClass.id,
                                            studentId = student.id,
                                            dateIso = selectedDateIso,
                                            status = newStatus.name
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun StatChip(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
            Text(text = label, fontSize = 11.sp, color = textColor.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun AttendanceStudentItem(
    student: StudentEntity,
    currentStatus: AttendanceStatus?,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (currentStatus) {
                AttendanceStatus.PRESENT -> PresentGreenContainer.copy(alpha = 0.4f)
                AttendanceStatus.ABSENT -> AbsentRedContainer.copy(alpha = 0.4f)
                AttendanceStatus.LATE -> LateOrangeContainer.copy(alpha = 0.4f)
                AttendanceStatus.LEAVE -> LeaveBlueContainer.copy(alpha = 0.4f)
                AttendanceStatus.HOLIDAY -> HolidayPurpleContainer.copy(alpha = 0.4f)
                null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Roll No Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.rollNo.ifEmpty { "#" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = student.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Rapid One-Tap Status Selector Pill Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AttendanceButton(
                    label = "P",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    selectedColor = PresentGreen,
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
                )
                AttendanceButton(
                    label = "A",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    selectedColor = AbsentRed,
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
                )
                AttendanceButton(
                    label = "L",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    selectedColor = LateOrange,
                    onClick = { onStatusSelected(AttendanceStatus.LATE) }
                )
                AttendanceButton(
                    label = "LV",
                    isSelected = currentStatus == AttendanceStatus.LEAVE,
                    selectedColor = LeaveBlue,
                    onClick = { onStatusSelected(AttendanceStatus.LEAVE) }
                )
            }
        }
    }
}

@Composable
fun AttendanceButton(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
