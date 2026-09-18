package com.example.features.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.model.AttendanceStatus
import com.example.core.repository.AttendMarkRepository
import com.example.features.classes.ClassSwitcherHeader
import com.example.features.classes.CreateClassDialog
import com.example.features.classes.ManageClassesDialog
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TertiaryLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: AttendMarkRepository,
    activeClass: ClassRoomEntity?,
    allClasses: List<ClassRoomEntity>,
    onSelectClass: (ClassRoomEntity) -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToMarks: () -> Unit,
    onNavigateToStudents: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val todayIso = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    }

    val students by (activeClass?.let { repository.getStudentsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val todayAttendance by (activeClass?.let { repository.getAttendanceForDate(it.id, todayIso) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val exams by (activeClass?.let { repository.getExamsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val profile by repository.profileFlow.collectAsState(initial = null)

    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showManageClassesDialog by remember { mutableStateOf(false) }

    val presentToday = todayAttendance.count { it.status == AttendanceStatus.PRESENT.name }
    val absentToday = todayAttendance.count { it.status == AttendanceStatus.ABSENT.name }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello, ${profile?.fullName?.ifEmpty { "Teacher" }} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile?.schoolName?.ifEmpty { "AttendMark Dashboard" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Class Switcher Header
        ClassSwitcherHeader(
            activeClass = activeClass,
            allClasses = allClasses,
            onSelectClass = onSelectClass,
            onManageClasses = { showManageClassesDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick KPI Metric Cards
        Text(
            text = "Today's Overview ($todayIso)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Total Enrolled",
                value = students.size.toString(),
                icon = Icons.Default.Group,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Present Today",
                value = "$presentToday/${students.size}",
                icon = Icons.Default.CheckCircle,
                accentColor = PresentGreen,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Absent Today",
                value = absentToday.toString(),
                icon = Icons.Default.Warning,
                accentColor = AbsentRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Action Tiles
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            onClick = onNavigateToAttendance,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark Attendance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("One-tap rapid marking & absence alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            onClick = onNavigateToMarks,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = TertiaryLight)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Record Marks & Grades", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Spreadsheet scoring & WhatsApp results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Assessments List
        Text(
            text = "Assessments (${exams.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (exams.isEmpty()) {
            Text(
                text = "No assessments created yet for this class.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            exams.take(3).forEach { exam ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exam.name, fontWeight = FontWeight.SemiBold)
                            Text("Max Marks: ${exam.maxMarks} • ${exam.dateIso}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = exam.examType,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
