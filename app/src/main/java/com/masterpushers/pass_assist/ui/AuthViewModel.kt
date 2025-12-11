package com.masterpushers.pass_assist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.masterpushers.pass_assist.utils.BiometricAuthManager
import com.masterpushers.pass_assist.utils.BiometricAvailability
import com.masterpushers.pass_assist.utils.PinManager
import com.masterpushers.pass_assist.utils.PinValidationResult

/**
 * ViewModel for managing authentication state
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val pinManager = PinManager.getInstance(application)
    private val biometricAuthManager = BiometricAuthManager(application)
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState
    
    private val _authMode = MutableLiveData<AuthMode>()
    val authMode: LiveData<AuthMode> = _authMode
    
    private val _pinSetupRequired = MutableLiveData<Boolean>()
    val pinSetupRequired: LiveData<Boolean> = _pinSetupRequired
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        initializeAuthMode()
    }
    
    /**
     * Initialize authentication mode based on device capabilities and PIN setup
     */
    private fun initializeAuthMode() {
        val biometricAvailability = biometricAuthManager.isBiometricAvailable()
        val isPinSet = pinManager.isPinSet()
        
        when {
            // First launch - need to set up PIN
            !isPinSet -> {
                _pinSetupRequired.value = true
                _authMode.value = AuthMode.PinSetup
            }
            // Biometric available - use it
            biometricAvailability.isAvailable() -> {
                _authMode.value = AuthMode.Biometric
                _pinSetupRequired.value = false
            }
            // Fallback to PIN only
            else -> {
                _authMode.value = AuthMode.PinOnly
                _pinSetupRequired.value = false
            }
        }
    }
    
    /**
     * Handle successful biometric authentication
     */
    fun onBiometricAuthSuccess() {
        _authState.value = AuthState.Authenticated
        _errorMessage.value = null
    }
    
    /**
     * Handle biometric authentication error
     */
    fun onBiometricAuthError(error: String) {
        _errorMessage.value = error
        // Switch to PIN mode if biometric fails
        _authMode.value = AuthMode.PinOnly
    }
    
    /**
     * Handle biometric authentication failure (not recognized)
     */
    fun onBiometricAuthFailed() {
        _errorMessage.value = "Biometric not recognized. Try again."
    }
    
    /**
     * Validate PIN entry
     */
    fun validatePin(pin: String) {
        when (val result = pinManager.validatePin(pin)) {
            is PinValidationResult.Success -> {
                _authState.value = AuthState.Authenticated
                _errorMessage.value = null
            }
            is PinValidationResult.Incorrect -> {
                _errorMessage.value = "Incorrect PIN. ${result.remainingAttempts} attempts remaining."
            }
            is PinValidationResult.LockedOut -> {
                val seconds = (result.remainingTimeMs / 1000).toInt()
                _errorMessage.value = "Too many attempts. Try again in $seconds seconds."
            }
            is PinValidationResult.NotSet -> {
                _errorMessage.value = "PIN not set. Please set up a PIN first."
                _authMode.value = AuthMode.PinSetup
            }
        }
    }
    
    /**
     * Set up a new PIN
     */
    fun setupPin(pin: String, confirmPin: String): Boolean {
        if (pin != confirmPin) {
            _errorMessage.value = "PINs do not match"
            return false
        }
        
        if (pin.length != 4) {
            _errorMessage.value = "PIN must be 4 digits"
            return false
        }
        
        if (!pin.all { it.isDigit() }) {
            _errorMessage.value = "PIN must contain only digits"
            return false
        }
        
        val success = pinManager.setPin(pin)
        if (success) {
            _errorMessage.value = null
            _pinSetupRequired.value = false
            
            // After PIN setup, check if we can use biometric
            val biometricAvailability = biometricAuthManager.isBiometricAvailable()
            _authMode.value = if (biometricAvailability.isAvailable()) {
                AuthMode.Biometric
            } else {
                AuthMode.PinOnly
            }
            
            // Auto authenticate after setup
            _authState.value = AuthState.Authenticated
        } else {
            _errorMessage.value = "Failed to set PIN"
        }
        
        return success
    }
    
    /**
     * Switch from biometric to PIN mode
     */
    fun switchToPinMode() {
        _authMode.value = AuthMode.PinOnly
        _errorMessage.value = null
    }
    
    /**
     * Retry biometric authentication
     */
    fun retryBiometric() {
        _authMode.value = AuthMode.Biometric
        _errorMessage.value = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get biometric manager instance
     */
    fun getBiometricAuthManager(): BiometricAuthManager {
        return biometricAuthManager
    }
    
    /**
     * Check if PIN is set
     */
    fun isPinSet(): Boolean {
        return pinManager.isPinSet()
    }
    
    /**
     * Reset authentication state (for testing or logout)
     */
    fun logout() {
        _authState.value = AuthState.Unauthenticated
        initializeAuthMode()
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
}

/**
 * Authentication mode
 */
sealed class AuthMode {
    object Biometric : AuthMode()
    object PinOnly : AuthMode()
    object PinSetup : AuthMode()
}

