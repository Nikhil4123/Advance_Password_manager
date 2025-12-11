package com.masterpushers.pass_assist.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterpushers.pass_assist.ui.theme.ButtonBlack
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    ),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.observeAsState(AuthState.Unauthenticated)
    val authMode by viewModel.authMode.observeAsState(AuthMode.Biometric)
    val pinSetupRequired by viewModel.pinSetupRequired.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    var hasTriggeredBiometric by remember { mutableStateOf(false) }
    
    // Reset authentication state when screen is shown (when returning from background)
    LaunchedEffect(Unit) {
        viewModel.logout() // Reset to unauthenticated state
        hasTriggeredBiometric = false // Reset trigger flag
    }
    
    // Handle successful authentication
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }
    
    // Trigger biometric prompt when in biometric mode
    LaunchedEffect(authMode, activity) {
        if (authMode is AuthMode.Biometric && activity != null && !hasTriggeredBiometric) {
            // Small delay to ensure activity is fully ready
            delay(300)
            hasTriggeredBiometric = true
            viewModel.getBiometricAuthManager().authenticate(
                activity = activity,
                onSuccess = { 
                    hasTriggeredBiometric = false
                    viewModel.onBiometricAuthSuccess() 
                },
                onError = { error -> 
                    hasTriggeredBiometric = false
                    viewModel.onBiometricAuthError(error) 
                },
                onFailed = { viewModel.onBiometricAuthFailed() }
            )
        }
    }
    
    // Reset trigger when auth mode changes
    LaunchedEffect(authMode) {
        if (authMode !is AuthMode.Biometric) {
            hasTriggeredBiometric = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when {
            pinSetupRequired -> PinSetupScreen(viewModel, errorMessage)
            authMode is AuthMode.Biometric -> BiometricAuthScreen(viewModel, errorMessage)
            authMode is AuthMode.PinOnly -> PinAuthScreen(viewModel, errorMessage)
        }
    }
}

@Composable
fun BiometricAuthScreen(
    viewModel: AuthViewModel,
    errorMessage: String?
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Fingerprint",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Unlock Password Manager",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Use your biometric credential to unlock",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Manual trigger button for biometric
        Button(
            onClick = {
                if (activity != null) {
                    viewModel.getBiometricAuthManager().authenticate(
                        activity = activity,
                        onSuccess = { viewModel.onBiometricAuthSuccess() },
                        onError = { error -> viewModel.onBiometricAuthError(error) },
                        onFailed = { viewModel.onBiometricAuthFailed() }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Authenticate with Biometric",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.switchToPinMode() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonBlack,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Use PIN Instead",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun PinAuthScreen(
    viewModel: AuthViewModel,
    errorMessage: String?
) {
    var pin by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Enter PIN",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter your 4-digit PIN to unlock",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN dots display
        PinDotsDisplay(pinLength = pin.length)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Hidden PIN input field
        OutlinedTextField(
            value = pin,
            onValueChange = { newPin ->
                if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                    pin = newPin
                    if (newPin.length == 4) {
                        viewModel.validatePin(newPin)
                        pin = "" // Clear PIN after validation
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            placeholder = { Text("Enter 4-digit PIN") }
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        
        if (viewModel.getBiometricAuthManager().isBiometricAvailable().isAvailable()) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { viewModel.retryBiometric() }
            ) {
                Text(
                    text = "Use Biometric",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PinSetupScreen(
    viewModel: AuthViewModel,
    errorMessage: String?
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (!isConfirming) "Create PIN" else "Confirm PIN",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (!isConfirming) 
                "Create a 4-digit PIN to secure your passwords" 
            else 
                "Enter your PIN again to confirm",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN dots display
        PinDotsDisplay(pinLength = if (!isConfirming) pin.length else confirmPin.length)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Hidden PIN input field
        if (!isConfirming) {
            OutlinedTextField(
                value = pin,
                onValueChange = { newPin ->
                    if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                        pin = newPin
                        if (newPin.length == 4) {
                            isConfirming = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = { Text("Enter 4-digit PIN") }
            )
        } else {
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { newPin ->
                    if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                        confirmPin = newPin
                        if (newPin.length == 4) {
                            val success = viewModel.setupPin(pin, confirmPin)
                            if (!success) {
                                pin = ""
                                confirmPin = ""
                                isConfirming = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = { Text("Confirm 4-digit PIN") }
            )
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        
        if (isConfirming) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = {
                    pin = ""
                    confirmPin = ""
                    isConfirming = false
                    viewModel.clearError()
                }
            ) {
                Text(
                    text = "Reset PIN",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PinDotsDisplay(pinLength: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pinLength)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (index < pinLength)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            
            if (index < 3) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

