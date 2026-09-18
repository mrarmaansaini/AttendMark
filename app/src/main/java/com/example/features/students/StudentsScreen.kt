package com.example.features.students

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.entity.ClassRoomEntity
import com.example.core.database.entity.StudentEntity
import com.example.core.model.CountryCodes
import com.example.core.repository.AttendMarkRepository
import com.example.core.util.CsvImporter
import com.example.core.util.ExportHelper
import com.example.core.util.PhoneValidator
import com.example.features.classes.ClassSwitcherHeader
import com.example.features.classes.CreateClassDialog
import com.example.features.classes.ManageClassesDialog
import com.example.ui.components.CountryCodePickerDialog
import com.example.ui.components.CountryCodeSelector
import kotlinx.coroutines.launch

@Composable
fun StudentsScreen(
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

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showManageClassesDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.rollNo.contains(searchQuery, ignoreCase = true) ||
            it.guardianPhoneE164.contains(searchQuery)
        }
    }

    // CSV File Picker launcher
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && activeClass != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val parsed = CsvImporter.parseCsv(stream)
                        val entities = parsed.map { p ->
                            val validPhone = PhoneValidator.validate(p.guardianPhone)
                            StudentEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                classId = activeClass.id,
                                name = p.name,
                                rollNo = p.rollNo,
                                guardianName = p.guardianName,
                                guardianPhoneE164 = if (validPhone.isValid) validPhone.e164Number else p.guardianPhone,
                                whatsappConsent = true
                            )
                        }
                        repository.addStudentsBatch(entities)
                        snackbarHostState.showSnackbar("Successfully imported ${entities.size} students!")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to import CSV: ${e.localizedMessage}")
                }
            }
        }
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

    if (showImportCsvDialog) {
        AlertDialog(
            onDismissRequest = { showImportCsvDialog = false },
            icon = {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Import Students via CSV", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Upload a CSV spreadsheet to batch import students. Download our ready-to-use template to match the required columns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Required Fields / Headers:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• name *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Student Full Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• roll_no *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Roll / Seat Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Optional Fields:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• guardian_name", fontSize = 13.sp)
                                Text("Parent / Contact", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• guardian_phone", fontSize = 13.sp)
                                Text("WhatsApp Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• notes", fontSize = 13.sp)
                                Text("Special Remarks", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val templateFile = ExportHelper.createStudentTemplateCsv(context)
                                ExportHelper.shareFile(context, templateFile, "text/csv", "Save / Share Student Template")
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Failed to download template: ${e.localizedMessage}") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Sample Template (.csv)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showImportCsvDialog = false
                            csvPickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select & Import CSV File")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportCsvDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog && activeClass != null) {
        AddStudentDialog(
            classId = activeClass.id,
            onDismissRequest = { showAddDialog = false },
            onSave = { student ->
                scope.launch {
                    repository.addStudent(
                        classId = student.classId,
                        name = student.name,
                        rollNo = student.rollNo,
                        guardianName = student.guardianName,
                        guardianPhoneE164 = student.guardianPhoneE164,
                        whatsappConsent = student.whatsappConsent,
                        notes = student.notes
                    )
                    showAddDialog = false
                }
            }
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

            // Action Row: Search + Import CSV
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, roll no...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showImportCsvDialog = true },
                    enabled = activeClass != null
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Students Counter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredStudents.size} Students Enrolled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (activeClass == null) "Select or create a class first" else "No students enrolled yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + or Import CSV to add students",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        StudentCard(
                            student = student,
                            onDelete = {
                                scope.launch {
                                    repository.deleteStudent(student)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (activeClass != null) showAddDialog = true
                else showCreateClassDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Student", tint = MaterialTheme.colorScheme.onPrimary)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun StudentCard(
    student: StudentEntity,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Roll No Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.rollNo.ifEmpty { "#" },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (student.guardianPhoneE164.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${student.guardianName.ifEmpty { "Guardian" }}: ${student.guardianPhoneE164}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (student.whatsappConsent) {
                    Text(
                        text = "• WhatsApp Consent: Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete student",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddStudentDialog(
    classId: String,
    onDismissRequest: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var rawPhone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCodes.default) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var whatsappConsent by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val phoneValidation = remember(rawPhone, selectedCountry) {
        if (rawPhone.isBlank()) null
        else {
            val full = if (rawPhone.startsWith("+")) rawPhone else "${selectedCountry.dialCode}$rawPhone"
            PhoneValidator.validate(full, selectedCountry.code)
        }
    }

    if (showCountryDialog) {
        CountryCodePickerDialog(
            selectedCountry = selectedCountry,
            onCountrySelected = { selectedCountry = it },
            onDismissRequest = { showCountryDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Student") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("Roll Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("Guardian / Parent Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountryCodeSelector(
                        selectedCountry = selectedCountry,
                        onClick = { showCountryDialog = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = rawPhone,
                        onValueChange = { rawPhone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                        label = { Text("Guardian Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = whatsappConsent,
                        onCheckedChange = { whatsappConsent = it }
                    )
                    Text("Consent to receive WhatsApp alerts", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val e164 = phoneValidation?.e164Number ?: rawPhone
                        onSave(
                            StudentEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                classId = classId,
                                name = name.trim(),
                                rollNo = rollNo.trim(),
                                guardianName = guardianName.trim(),
                                guardianPhoneE164 = e164,
                                whatsappConsent = whatsappConsent,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
