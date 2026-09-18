package com.example.features.onboarding

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.datastore.TeacherProfile
import com.example.core.model.CountryCodes
import com.example.core.repository.AttendMarkRepository
import com.example.core.util.PhoneValidator
import com.example.core.util.WhatsAppHelper
import com.example.ui.components.CountryCodePickerDialog
import com.example.ui.components.CountryCodeSelector
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    repository: AttendMarkRepository,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var rawPhone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCodes.default) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var preferredApp by remember { mutableStateOf(WhatsAppHelper.PKG_WHATSAPP) }

    // Auto-detect installed WhatsApp variants
    val isStandardInstalled = remember { WhatsAppHelper.isAppInstalled(context, WhatsAppHelper.PKG_WHATSAPP) }
    val isBusinessInstalled = remember { WhatsAppHelper.isAppInstalled(context, WhatsAppHelper.PKG_WHATSAPP_BUSINESS) }

    // Validation via libphonenumber with zero-prefix stripping
    val cleanRaw = if (rawPhone.startsWith("+")) rawPhone.trim() else rawPhone.trim().trimStart('0')
    val phoneValidation = remember(cleanRaw, selectedCountry) {
        if (cleanRaw.isBlank()) {
            PhoneValidator.ValidationResult(isValid = false, errorMessage = "Phone number is optional")
        } else {
            val fullNumber = if (cleanRaw.startsWith("+")) cleanRaw else "${selectedCountry.dialCode}$cleanRaw"
            PhoneValidator.validate(fullNumber, selectedCountry.code)
        }
    }

    val isFormValid = fullName.trim().isNotBlank() && (rawPhone.isBlank() || phoneValidation.isValid)

    if (showCountryDialog) {
        CountryCodePickerDialog(
            selectedCountry = selectedCountry,
            onCountrySelected = { selectedCountry = it },
            onDismissRequest = { showCountryDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Hero Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to AttendMark",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Quick setup to personalize your attendance & score messages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Important Note Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Messages are sent directly from the WhatsApp account logged in on this device. Ensure your phone number matches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Full Name (Required)
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            placeholder = { Text("e.g. Prof. Rajesh Sharma") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            isError = fullName.isNotBlank() && fullName.trim().length < 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number + Country Picker (Required)
        Text(
            text = "Your WhatsApp Number *",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryCodeSelector(
                selectedCountry = selectedCountry,
                onClick = { showCountryDialog = true }
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = rawPhone,
                onValueChange = { rawPhone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                placeholder = { Text("WhatsApp mobile number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                isError = rawPhone.isNotBlank() && !phoneValidation.isValid,
                modifier = Modifier.weight(1f)
            )
        }
        if (rawPhone.isNotBlank() && !phoneValidation.isValid) {
            Text(
                text = phoneValidation.errorMessage ?: "Invalid mobile number",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 4.dp)
            )
        } else if (phoneValidation.isValid) {
            Text(
                text = "✓ Valid E.164: ${phoneValidation.e164Number}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional Fields
        OutlinedTextField(
            value = schoolName,
            onValueChange = { schoolName = it },
            label = { Text("School / Institute Name (Optional)") },
            placeholder = { Text("e.g. St. Xavier's Academy") },
            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = designation,
            onValueChange = { designation = it },
            label = { Text("Designation / Subject (Optional)") },
            placeholder = { Text("e.g. Mathematics Lead / Tutor") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Optional)") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // WhatsApp Preference
        Text(
            text = "Preferred WhatsApp Application",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = preferredApp == WhatsAppHelper.PKG_WHATSAPP,
                onClick = { preferredApp = WhatsAppHelper.PKG_WHATSAPP }
            )
            Text(
                text = "WhatsApp" + if (isStandardInstalled) " (Installed)" else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = preferredApp == WhatsAppHelper.PKG_WHATSAPP_BUSINESS,
                onClick = { preferredApp = WhatsAppHelper.PKG_WHATSAPP_BUSINESS }
            )
            Text(
                text = "WhatsApp Business" + if (isBusinessInstalled) " (Installed)" else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (fullName.trim().isEmpty()) {
            Text(
                text = "Enter your full name above to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (rawPhone.isNotBlank() && !phoneValidation.isValid) {
            Text(
                text = "Please enter a valid mobile number or leave blank",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                scope.launch {
                    val phoneToSave = if (phoneValidation.isValid) {
                        phoneValidation.e164Number
                    } else if (rawPhone.isNotBlank()) {
                        if (rawPhone.startsWith("+")) rawPhone.trim() else "${selectedCountry.dialCode}${rawPhone.trim().trimStart('0')}"
                    } else {
                        ""
                    }
                    val profile = TeacherProfile(
                        fullName = fullName.trim(),
                        phoneE164 = phoneToSave,
                        email = email.trim(),
                        schoolName = schoolName.trim(),
                        designation = designation.trim(),
                        preferredWhatsAppApp = preferredApp,
                        isOnboarded = true
                    )
                    repository.saveProfile(profile)
                    // Auto-create a starter class if none exist
                    repository.createClass(
                        name = "Class 10-A",
                        subject = "Mathematics",
                        section = "A",
                        academicYear = "2026-27"
                    )
                    onComplete()
                }
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
