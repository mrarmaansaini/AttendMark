package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attendmark_prefs")

data class TeacherProfile(
    val fullName: String = "",
    val phoneE164: String = "",
    val email: String = "",
    val photoUri: String = "",
    val schoolName: String = "",
    val designation: String = "",
    val preferredWhatsAppApp: String = "com.whatsapp",
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val lowAttendanceThreshold: Int = 75,
    val isOnboarded: Boolean = false
)

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_FULL_NAME = stringPreferencesKey("full_name")
        val KEY_PHONE = stringPreferencesKey("phone_e164")
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_PHOTO_URI = stringPreferencesKey("photo_uri")
        val KEY_SCHOOL_NAME = stringPreferencesKey("school_name")
        val KEY_DESIGNATION = stringPreferencesKey("designation")
        val KEY_PREFERRED_WHATSAPP = stringPreferencesKey("preferred_whatsapp")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val KEY_LOW_ATTENDANCE_THRESHOLD = intPreferencesKey("low_attendance_threshold")
        val KEY_IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val KEY_ACTIVE_CLASS_ID = stringPreferencesKey("active_class_id")
        val KEY_FIREBASE_PROJECT_ID = stringPreferencesKey("firebase_project_id")
        val KEY_FIREBASE_API_KEY = stringPreferencesKey("firebase_api_key")
        val KEY_LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val firebaseProjectIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIREBASE_PROJECT_ID] ?: ""
    }

    val firebaseApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIREBASE_API_KEY] ?: ""
    }

    val lastSyncTimeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME] ?: ""
    }

    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
    }

    suspend fun saveFirebaseConfig(projectId: String, apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIREBASE_PROJECT_ID] = projectId.trim()
            prefs[KEY_FIREBASE_API_KEY] = apiKey.trim()
        }
    }

    suspend fun saveLastSyncTime(timeStr: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = timeStr
        }
    }

    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    val profileFlow: Flow<TeacherProfile> = context.dataStore.data.map { prefs ->
        TeacherProfile(
            fullName = prefs[KEY_FULL_NAME] ?: "",
            phoneE164 = prefs[KEY_PHONE] ?: "",
            email = prefs[KEY_EMAIL] ?: "",
            photoUri = prefs[KEY_PHOTO_URI] ?: "",
            schoolName = prefs[KEY_SCHOOL_NAME] ?: "",
            designation = prefs[KEY_DESIGNATION] ?: "",
            preferredWhatsAppApp = prefs[KEY_PREFERRED_WHATSAPP] ?: "com.whatsapp",
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockPin = prefs[KEY_APP_LOCK_PIN] ?: "",
            lowAttendanceThreshold = prefs[KEY_LOW_ATTENDANCE_THRESHOLD] ?: 75,
            isOnboarded = prefs[KEY_IS_ONBOARDED] ?: false
        )
    }

    val activeClassIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_CLASS_ID]
    }

    suspend fun saveProfile(profile: TeacherProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FULL_NAME] = profile.fullName
            prefs[KEY_PHONE] = profile.phoneE164
            prefs[KEY_EMAIL] = profile.email
            prefs[KEY_PHOTO_URI] = profile.photoUri
            prefs[KEY_SCHOOL_NAME] = profile.schoolName
            prefs[KEY_DESIGNATION] = profile.designation
            prefs[KEY_PREFERRED_WHATSAPP] = profile.preferredWhatsAppApp
            prefs[KEY_APP_LOCK_ENABLED] = profile.appLockEnabled
            prefs[KEY_APP_LOCK_PIN] = profile.appLockPin
            prefs[KEY_LOW_ATTENDANCE_THRESHOLD] = profile.lowAttendanceThreshold
            prefs[KEY_IS_ONBOARDED] = true
        }
    }

    suspend fun setActiveClassId(classId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_CLASS_ID] = classId
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
