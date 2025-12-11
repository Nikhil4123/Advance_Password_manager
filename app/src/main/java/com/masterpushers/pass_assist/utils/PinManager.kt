package com.masterpushers.pass_assist.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Manages PIN creation, storage, and validation with encryption and attempt limiting
 */
class PinManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_IS_PIN_SET = "is_pin_set"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
        
        @Volatile
        private var instance: PinManager? = null
        
        fun getInstance(context: Context): PinManager {
            return instance ?: synchronized(this) {
                instance ?: PinManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Check if a PIN has been set up
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PIN_SET, false)
    }
    
    /**
     * Set a new PIN (hashed before storage)
     */
    fun setPin(pin: String): Boolean {
        if (pin.length != 4) {
            return false
        }
        
        if (!pin.all { it.isDigit() }) {
            return false
        }
        
        val pinHash = hashPin(pin)
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, pinHash)
            .putBoolean(KEY_IS_PIN_SET, true)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply()
        
        return true
    }
    
    /**
     * Validate the entered PIN
     */
    fun validatePin(pin: String): PinValidationResult {
        // Check if locked out
        if (isLockedOut()) {
            val remainingTime = getRemainingLockoutTime()
            return PinValidationResult.LockedOut(remainingTime)
        }
        
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null)
            ?: return PinValidationResult.NotSet
        
        val inputHash = hashPin(pin)
        
        return if (storedHash == inputHash) {
            // Reset failed attempts on success
            sharedPreferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply()
            PinValidationResult.Success
        } else {
            val failedAttempts = incrementFailedAttempts()
            val remainingAttempts = MAX_ATTEMPTS - failedAttempts
            
            if (remainingAttempts <= 0) {
                lockout()
                PinValidationResult.LockedOut(LOCKOUT_DURATION_MS)
            } else {
                PinValidationResult.Incorrect(remainingAttempts)
            }
        }
    }
    
    /**
     * Change the existing PIN (requires old PIN for verification)
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        val validationResult = validatePin(oldPin)
        if (validationResult !is PinValidationResult.Success) {
            return false
        }
        
        return setPin(newPin)
    }
    
    /**
     * Clear PIN (for testing or reset)
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_IS_PIN_SET, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_TIMESTAMP)
            .apply()
    }
    
    /**
     * Check if currently locked out
     */
    private fun isLockedOut(): Boolean {
        val lockoutTimestamp = sharedPreferences.getLong(KEY_LOCKOUT_TIMESTAMP, 0)
        if (lockoutTimestamp == 0L) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val isLocked = currentTime < lockoutTimestamp
        
        // Clear lockout if time has passed
        if (!isLocked) {
            sharedPreferences.edit()
                .remove(KEY_LOCKOUT_TIMESTAMP)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply()
        }
        
        return isLocked
    }
    
    /**
     * Get remaining lockout time in milliseconds
     */
    private fun getRemainingLockoutTime(): Long {
        val lockoutTimestamp = sharedPreferences.getLong(KEY_LOCKOUT_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return maxOf(0, lockoutTimestamp - currentTime)
    }
    
    /**
     * Increment failed attempts counter
     */
    private fun incrementFailedAttempts(): Int {
        val currentAttempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0)
        val newAttempts = currentAttempts + 1
        sharedPreferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, newAttempts)
            .apply()
        return newAttempts
    }
    
    /**
     * Lock out the user for a specified duration
     */
    private fun lockout() {
        val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
        sharedPreferences.edit()
            .putLong(KEY_LOCKOUT_TIMESTAMP, lockoutUntil)
            .apply()
    }
    
    /**
     * Hash the PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Result of PIN validation
 */
sealed class PinValidationResult {
    object Success : PinValidationResult()
    object NotSet : PinValidationResult()
    data class Incorrect(val remainingAttempts: Int) : PinValidationResult()
    data class LockedOut(val remainingTimeMs: Long) : PinValidationResult()
}

