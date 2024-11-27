package com.example.badminton

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.badminton.data.AppDatabase
import com.example.badminton.data.populateDatabase
import com.example.badminton.ui.AppNavigation
import com.example.badminton.ui.theme.BadmintonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BadmintonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }

        // For testing: Ensure the database callback is triggered
        val db = AppDatabase.getDatabase(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                populateDatabase(db) // Populate the database with dummy data
                Log.d("MainActivity", "Dummy data population complete")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error populating database", e)
            }
        }
    }
}
