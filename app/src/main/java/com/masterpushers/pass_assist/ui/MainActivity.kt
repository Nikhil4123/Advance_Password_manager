package com.masterpushers.pass_assist.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.masterpushers.pass_assist.ui.theme.PassAssistTheme

class MainActivity : AppCompatActivity() {
    
    private var requiresAuth by mutableStateOf(true)
    private var wasInBackground = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TEMPORARILY DISABLED: Prevent screenshots and hide content in Recent Apps
        // Uncomment the lines below after taking screenshots to re-enable security
        // window.setFlags(
        //     WindowManager.LayoutParams.FLAG_SECURE,
        //     WindowManager.LayoutParams.FLAG_SECURE
        // )
        setContent {
            PassAssistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PasswordApp(
                        requiresAuth = requiresAuth,
                        onAuthenticationRequired = { requiresAuth = true }
                    )
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Mark that app is going to background
        wasInBackground = true
    }
    
    override fun onResume() {
        super.onResume()
        // Require authentication when app comes back from background
        if (wasInBackground) {
            requiresAuth = true
            wasInBackground = false
        }
    }
}