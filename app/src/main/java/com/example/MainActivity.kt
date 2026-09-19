package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.core.repository.AttendMarkRepository
import com.example.navigation.AppNavHost
import com.example.ui.theme.AttendMarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AttendMarkRepository(applicationContext)

        // Automatic Background Polling Loop for Real-time Two-way Sync
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                delay(8000) // Poll Firestore every 8 seconds
                try {
                    val projectId = repository.preferencesManager.firebaseProjectIdFlow.first()
                    val apiKey = repository.preferencesManager.firebaseApiKeyFlow.first()
                    if (projectId.isNotBlank()) {
                        repository.syncPullFirebase(projectId, apiKey)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Auto-pull failed: ${e.message}")
                }
            }
        }

        setContent {
            AttendMarkTheme {
                AppNavHost(repository = repository)
            }
        }
    }
}
