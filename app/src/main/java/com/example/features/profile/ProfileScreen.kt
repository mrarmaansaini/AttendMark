package com.example.features.profile

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.core.datastore.TeacherProfile
import com.example.core.repository.AttendMarkRepository
import com.example.core.util.WhatsAppHelper
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: AttendMarkRepository,
    onDataWiped: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by repository.profileFlow.collectAsState(initial = TeacherProfile())
    val storedProjectId by repository.firebaseProjectIdFlow.collectAsState(initial = "")
    val storedApiKey by repository.firebaseApiKeyFlow.collectAsState(initial = "")
    val lastSyncTime by repository.lastSyncTimeFlow.collectAsState(initial = "")

    var firebaseProjectId by remember(storedProjectId) { mutableStateOf(storedProjectId) }
    var firebaseApiKey by remember(storedApiKey) { mutableStateOf(storedApiKey) }
    var isSyncing by remember { mutableStateOf(false) }

    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var phoneE164 by remember(profile) { mutableStateOf(profile.phoneE164) }
    var schoolName by remember(profile) { mutableStateOf(profile.schoolName) }
    var designation by remember(profile) { mutableStateOf(profile.designation) }
    var preferredApp by remember(profile) { mutableStateOf(profile.preferredWhatsAppApp) }
    var appLockEnabled by remember(profile) { mutableStateOf(profile.appLockEnabled) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Data?") },
            text = {
                Text("This will permanently delete all student rosters, attendance records, exam marks, and reset your profile. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.wipeAllData()
                            showDeleteConfirmDialog = false
                            onDataWiped()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Avatar Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = profile.fullName.ifEmpty { "Teacher Profile" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.phoneE164.ifEmpty { "No phone set" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Firebase Cloud Sync Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firebase Cloud Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Two-way sync with Web using your Firebase project",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = firebaseProjectId,
                        onValueChange = { firebaseProjectId = it },
                        label = { Text("Firebase Project ID") },
                        placeholder = { Text("e.g. attendmark-prod") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = firebaseApiKey,
                        onValueChange = { firebaseApiKey = it },
                        label = { Text("Firebase Web API Key (Optional)") },
                        placeholder = { Text("e.g. AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (lastSyncTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last synced: $lastSyncTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (firebaseProjectId.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Enter Firebase Project ID") }
                                    return@Button
                                }
                                if (isSyncing) return@Button
                                isSyncing = true
                                scope.launch {
                                    repository.saveFirebaseConfig(firebaseProjectId, firebaseApiKey)
                                    val result = repository.syncPushFirebase(firebaseProjectId, firebaseApiKey)
                                    isSyncing = false
                                    when (result) {
                                        is com.example.core.sync.SyncResult.Success -> {
                                            snackbarHostState.showSnackbar("Uploaded to Firebase Firestore!")
                                        }
                                        is com.example.core.sync.SyncResult.Error -> {
                                            snackbarHostState.showSnackbar("Firebase error: ${result.message}")
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload")
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (firebaseProjectId.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Enter Firebase Project ID") }
                                    return@OutlinedButton
                                }
                                if (isSyncing) return@OutlinedButton
                                isSyncing = true
                                scope.launch {
                                    repository.saveFirebaseConfig(firebaseProjectId, firebaseApiKey)
                                    val result = repository.syncPullFirebase(firebaseProjectId, firebaseApiKey)
                                    isSyncing = false
                                    when (result) {
                                        is com.example.core.sync.SyncResult.Success -> {
                                            snackbarHostState.showSnackbar("Received: ${result.message}")
                                        }
                                        is com.example.core.sync.SyncResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Profile Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phoneE164,
                onValueChange = { phoneE164 = it },
                label = { Text("WhatsApp Phone (E.164)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School / Institute Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = designation,
                onValueChange = { designation = it },
                label = { Text("Designation / Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preferred WhatsApp Package
            Text("Preferred WhatsApp App", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = preferredApp == WhatsAppHelper.PKG_WHATSAPP,
                    onClick = { preferredApp = WhatsAppHelper.PKG_WHATSAPP }
                )
                Text("WhatsApp")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = preferredApp == WhatsAppHelper.PKG_WHATSAPP_BUSINESS,
                    onClick = { preferredApp = WhatsAppHelper.PKG_WHATSAPP_BUSINESS }
                )
                Text("WhatsApp Business")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Profile Button
            Button(
                onClick = {
                    scope.launch {
                        repository.saveProfile(
                            profile.copy(
                                fullName = fullName.trim(),
                                phoneE164 = phoneE164.trim(),
                                schoolName = schoolName.trim(),
                                designation = designation.trim(),
                                preferredWhatsAppApp = preferredApp,
                                appLockEnabled = appLockEnabled
                            )
                        )
                        snackbarHostState.showSnackbar("Profile updated successfully")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Profile Changes")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Privacy & Data Destruction Section
            Text("Privacy & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDeleteConfirmDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All My Data (Reset App)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
