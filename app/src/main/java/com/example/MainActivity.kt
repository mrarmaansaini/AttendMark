package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.core.repository.AttendMarkRepository
import com.example.navigation.AppNavHost
import com.example.ui.theme.AttendMarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AttendMarkRepository(applicationContext)

        setContent {
            AttendMarkTheme {
                AppNavHost(repository = repository)
            }
        }
    }
}
