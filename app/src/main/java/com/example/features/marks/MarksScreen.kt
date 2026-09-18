package com.example.features.marks

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.ExamEntity
import com.example.core.database.entity.MarkEntryEntity
import com.example.core.database.entity.SendQueueItemEntity
import com.example.core.database.entity.StudentEntity
import com.example.core.model.ExamType
import com.example.core.repository.AttendMarkRepository
import com.example.core.util.ExportHelper
import com.example.core.util.GradeCalculator
import com.example.core.util.PdfReportGenerator
import com.example.features.classes.ClassSwitcherHeader
import com.example.features.classes.CreateClassDialog
import com.example.features.classes.ManageClassesDialog
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MarksScreen(
    repository: AttendMarkRepository,
    activeClass: ClassRoomEntity?,
    allClasses: List<ClassRoomEntity>,
    onSelectClass: (ClassRoomEntity) -> Unit,
    onNavigateToSendQueue: (batchId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exams by (activeClass?.let { repository.getExamsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val students by (activeClass?.let { repository.getStudentsForClass(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    var selectedExam by remember(exams) { mutableStateOf(exams.firstOrNull()) }
    var showCreateExamDialog by remember { mutableStateOf(false) }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showManageClassesDialog by remember { mutableStateOf(false) }
    var showExportMarksDialog by remember { mutableStateOf(false) }
    var showConsolidatedPicker by remember { mutableStateOf(false) }

    val markEntries by (selectedExam?.let { repository.getMarksForExam(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val marksMap = remember(markEntries) {
        markEntries.associateBy { it.studentId }
    }

    // Auto calculate ranks and statistics
    val studentMarksDoubleMap = remember(marksMap) {
        marksMap.mapValues { it.value.marksObtained }
    }
    val ranksMap = remember(studentMarksDoubleMap, selectedExam) {
        val maxMarks = selectedExam?.maxMarks ?: 100.0
        GradeCalculator.computeRanks(studentMarksDoubleMap, maxMarks)
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

    if (showExportMarksDialog && activeClass != null) {
        AlertDialog(
            onDismissRequest = { showExportMarksDialog = false },
            icon = {
                Icon(
                    Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Export & Report Cards", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose an export format or generate multi-subject consolidated student report cards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Current Exam CSV
                    OutlinedCard(
                        onClick = {
                            showExportMarksDialog = false
                            if (selectedExam != null) {
                                try {
                                    val file = ExportHelper.exportExamMarksCsv(
                                        context = context,
                                        className = activeClass.name,
                                        exam = selectedExam!!,
                                        students = students,
                                        marks = markEntries
                                    )
                                    ExportHelper.shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "text/csv",
                                        chooserTitle = "Export ${selectedExam!!.name} Marks CSV"
                                    )
                                } catch (e: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}") }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Export Current Exam (CSV)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Roll No, Name, Marks, %, Grade for '${selectedExam?.name ?: "Exam"}'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: All Students Subject-Wise Master Sheet (CSV) - Requirement 6
                    OutlinedCard(
                        onClick = {
                            showExportMarksDialog = false
                            scope.launch {
                                try {
                                    val allMarks = repository.getAllMarksForClassSync(activeClass.id)
                                    val file = ExportHelper.exportMasterSubjectWiseCsv(
                                        context = context,
                                        className = activeClass.name,
                                        students = students,
                                        exams = exams,
                                        allMarks = allMarks
                                    )
                                    ExportHelper.shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "text/csv",
                                        chooserTitle = "Export Subject-Wise Marks Sheet"
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Export Subject-Wise Sheet (CSV)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("All students matrix with every subject, total, % & grade", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Multi-Subject Consolidated Report Card (PDF) - Requirement 5
                    OutlinedCard(
                        onClick = {
                            showExportMarksDialog = false
                            showConsolidatedPicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Consolidated Report Card (PDF)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Single sheet with all marks & subjects of each student", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportMarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showConsolidatedPicker && activeClass != null) {
        AlertDialog(
            onDismissRequest = { showConsolidatedPicker = false },
            title = { Text("Select Student for Report Card") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(students) { student ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showConsolidatedPicker = false
                                    scope.launch {
                                        try {
                                            val profile = repository.profileFlow.first()
                                            val allMarks = repository.getAllMarksForClassSync(activeClass.id)
                                            val subjectScores = exams.map { ex ->
                                                val entry = allMarks.find { it.studentId == student.id && it.examId == ex.id }
                                                PdfReportGenerator.SubjectScore(
                                                    subjectOrExamName = ex.name,
                                                    maxMarks = ex.maxMarks,
                                                    marksObtained = entry?.marksObtained ?: 0.0
                                                )
                                            }
                                            val file = PdfReportGenerator.generateConsolidatedReportCard(
                                                context = context,
                                                studentName = student.name,
                                                rollNo = student.rollNo,
                                                className = activeClass.name,
                                                academicYear = activeClass.academicYear.ifEmpty { "2024-2025" },
                                                scores = subjectScores,
                                                teacherName = profile.fullName,
                                                schoolName = profile.schoolName
                                            )
                                            if (file != null) {
                                                PdfReportGenerator.sharePdfFile(context, file)
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to generate PDF")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(student.name, fontWeight = FontWeight.SemiBold)
                                    Text("Roll: ${student.rollNo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConsolidatedPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateExamDialog && activeClass != null) {
        CreateExamDialog(
            classId = activeClass.id,
            onDismissRequest = { showCreateExamDialog = false },
            onCreated = { examId ->
                scope.launch {
                    showCreateExamDialog = false
                }
            },
            repository = repository
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Class Switcher
            ClassSwitcherHeader(
                activeClass = activeClass,
                allClasses = allClasses,
                onSelectClass = onSelectClass,
                onManageClasses = { showManageClassesDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Exam Selector Row + New Exam Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExamDropdownSelector(
                    exams = exams,
                    selectedExam = selectedExam,
                    onSelectExam = { selectedExam = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (activeClass != null) showCreateExamDialog = true
                        else showCreateClassDialog = true
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Exam")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedExam != null) {
                // Exam Header Summary Card
                val exam = selectedExam!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${exam.name} (${exam.examType})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Max Marks: ${exam.maxMarks} • Date: ${exam.dateIso}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Row: Export Options & WhatsApp Results
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showExportMarksDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Options", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val profile = repository.profileFlow.first()
                                val batchId = UUID.randomUUID().toString()

                                val queueItems = students.mapIndexed { index, student ->
                                    val markEntry = marksMap[student.id]
                                    val score = markEntry?.marksObtained ?: 0.0
                                    val rankInfo = ranksMap[student.id]
                                    val pct = rankInfo?.percentage ?: 0.0
                                    val grade = rankInfo?.grade ?: "N/A"
                                    val rank = rankInfo?.rank ?: (index + 1)

                                    val msg = "Dear Guardian, result for ${student.name} in ${exam.name} (${activeClass?.name}):\nMarks: $score/${exam.maxMarks} ($pct%)\nGrade: $grade | Rank: #$rank\n\n- ${profile.fullName}, ${profile.schoolName}"

                                    val isValid = student.guardianPhoneE164.isNotBlank() && student.whatsappConsent
                                    SendQueueItemEntity(
                                        id = UUID.randomUUID().toString(),
                                        batchId = batchId,
                                        studentId = student.id,
                                        studentName = student.name,
                                        recipientName = student.guardianName.ifEmpty { "Guardian" },
                                        recipientPhoneE164 = student.guardianPhoneE164,
                                        messageText = msg,
                                        queueStatus = if (isValid) "PENDING" else "SKIPPED",
                                        skipReason = if (!isValid) "No valid phone or consent" else null,
                                        sequenceIndex = index
                                    )
                                }
                                repository.createSendQueue(batchId, queueItems)
                                onNavigateToSendQueue(batchId)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp Results", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spreadsheet Student Score Entry Grid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val markEntry = marksMap[student.id]
                        val rankInfo = ranksMap[student.id]

                        StudentMarkRow(
                            student = student,
                            exam = exam,
                            currentMarks = markEntry?.marksObtained ?: 0.0,
                            rankInfo = rankInfo,
                            onSaveMarks = { newMarks ->
                                scope.launch {
                                    repository.recordMark(
                                        examId = exam.id,
                                        studentId = student.id,
                                        marksObtained = newMarks
                                    )
                                }
                            },
                            onGeneratePdf = {
                                scope.launch {
                                    val profile = repository.profileFlow.first()
                                    val score = markEntry?.marksObtained ?: 0.0
                                    val file = PdfReportGenerator.generateReportCard(
                                        context = context,
                                        studentName = student.name,
                                        rollNo = student.rollNo,
                                        className = activeClass?.name ?: "",
                                        examName = exam.name,
                                        marksObtained = score,
                                        maxMarks = exam.maxMarks,
                                        percentage = rankInfo?.percentage ?: 0.0,
                                        grade = rankInfo?.grade ?: "N/A",
                                        rank = rankInfo?.rank ?: 1,
                                        teacherName = profile.fullName,
                                        schoolName = profile.schoolName
                                    )
                                    if (file != null) {
                                        PdfReportGenerator.sharePdfFile(context, file)
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to generate PDF")
                                    }
                                }
                            },
                            onGenerateConsolidatedPdf = {
                                scope.launch {
                                    try {
                                        val profile = repository.profileFlow.first()
                                        val allMarks = repository.getAllMarksForClassSync(activeClass?.id ?: "")
                                        val subjectScores = exams.map { ex ->
                                            val entry = allMarks.find { it.studentId == student.id && it.examId == ex.id }
                                            PdfReportGenerator.SubjectScore(
                                                subjectOrExamName = ex.name,
                                                maxMarks = ex.maxMarks,
                                                marksObtained = entry?.marksObtained ?: 0.0
                                            )
                                        }
                                        val file = PdfReportGenerator.generateConsolidatedReportCard(
                                            context = context,
                                            studentName = student.name,
                                            rollNo = student.rollNo,
                                            className = activeClass?.name ?: "",
                                            academicYear = activeClass?.academicYear?.ifEmpty { "2024-2025" } ?: "2024-2025",
                                            scores = subjectScores,
                                            teacherName = profile.fullName,
                                            schoolName = profile.schoolName
                                        )
                                        if (file != null) {
                                            PdfReportGenerator.sharePdfFile(context, file)
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to generate PDF")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No assessments found for this class.\nTap '+ New Exam' to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
fun ExamDropdownSelector(
    exams: List<ExamEntity>,
    selectedExam: ExamEntity?,
    onSelectExam: (ExamEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = true },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedExam?.name ?: "Select Exam",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                exams.forEach { exam ->
                    DropdownMenuItem(
                        text = { Text("${exam.name} (${exam.maxMarks} marks)") },
                        onClick = {
                            onSelectExam(exam)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentMarkRow(
    student: StudentEntity,
    exam: ExamEntity,
    currentMarks: Double,
    rankInfo: GradeCalculator.StudentRank?,
    onSaveMarks: (Double) -> Unit,
    onGeneratePdf: () -> Unit,
    onGenerateConsolidatedPdf: () -> Unit
) {
    var textValue by remember(currentMarks) {
        mutableStateOf(if (currentMarks > 0.0) currentMarks.toString().removeSuffix(".0") else "")
    }
    var pdfMenuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Roll No
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.rollNo.ifEmpty { "#" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (rankInfo != null) {
                    Text(
                        text = "Grade: ${rankInfo.grade} • Rank: #${rankInfo.rank} (${rankInfo.percentage}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Numeric Score Input Field (Rapid Next typing)
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    textValue = filtered
                    val doubleVal = filtered.toDoubleOrNull()
                    if (doubleVal != null && doubleVal <= exam.maxMarks) {
                        onSaveMarks(doubleVal)
                    }
                },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.width(72.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // PDF Report Card Share Action with Dropdown
            Box {
                IconButton(onClick = { pdfMenuExpanded = true }) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Share PDF report card",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = pdfMenuExpanded,
                    onDismissRequest = { pdfMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Exam Report (${exam.name})") },
                        onClick = {
                            pdfMenuExpanded = false
                            onGeneratePdf()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Consolidated (All Subjects)") },
                        onClick = {
                            pdfMenuExpanded = false
                            onGenerateConsolidatedPdf()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateExamDialog(
    classId: String,
    onDismissRequest: () -> Unit,
    onCreated: (String) -> Unit,
    repository: AttendMarkRepository
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var maxMarksStr by remember { mutableStateOf("50") }
    var selectedType by remember { mutableStateOf(ExamType.UNIT_TEST.displayName) }
    var dateIso by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        mutableStateOf(sdf.format(Date()))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Assessment") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exam Name *") },
                    placeholder = { Text("e.g. Unit Test 1 or Mid-Term") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxMarksStr,
                    onValueChange = { maxMarksStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Maximum Marks *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Exam Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxMarks = maxMarksStr.toDoubleOrNull() ?: 100.0
                    if (name.isNotBlank()) {
                        scope.launch {
                            val id = repository.createExam(
                                classId = classId,
                                name = name.trim(),
                                examType = selectedType,
                                maxMarks = maxMarks,
                                dateIso = dateIso.trim()
                            )
                            onCreated(id)
                        }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
