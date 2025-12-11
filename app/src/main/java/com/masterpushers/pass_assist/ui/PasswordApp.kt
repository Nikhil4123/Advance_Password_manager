package com.masterpushers.pass_assist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PasswordApp(
    requiresAuth: Boolean = true,
    onAuthenticationRequired: () -> Unit = {}
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    
    // Reset authentication when requiresAuth changes to true
    LaunchedEffect(requiresAuth) {
        if (requiresAuth) {
            isAuthenticated = false
        }
    }
    
    if (!isAuthenticated) {
        AuthScreen(
            onAuthSuccess = {
                isAuthenticated = true
            }
        )
    } else {
        HomeScreen()
    }
}