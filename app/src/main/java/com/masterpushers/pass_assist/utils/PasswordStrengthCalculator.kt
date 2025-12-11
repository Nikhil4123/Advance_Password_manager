package com.masterpushers.pass_assist.utils

import androidx.compose.ui.graphics.Color

/**
 * Calculate password strength based on multiple criteria
 */
object PasswordStrengthCalculator {
    
    private val commonPasswords = setOf(
        "password", "123456", "12345678", "qwerty", "abc123", "monkey",
        "1234567", "letmein", "trustno1", "dragon", "baseball", "iloveyou",
        "master", "sunshine", "ashley", "bailey", "passw0rd", "shadow",
        "123123", "654321", "superman", "qazwsx", "michael", "football"
    )
    
    /**
     * Calculate password strength and return result
     */
    fun calculateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength.NONE
        }
        
        var score = 0
        val feedback = mutableListOf<String>()
        
        // Length check
        when {
            password.length < 6 -> {
                feedback.add("Too short")
            }
            password.length >= 8 -> {
                score += 20
            }
            password.length >= 12 -> {
                score += 10
            }
            password.length >= 16 -> {
                score += 10
            }
        }
        
        // Character diversity checks
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        if (hasLowercase) score += 10
        if (hasUppercase) score += 15
        if (hasDigit) score += 15
        if (hasSpecialChar) score += 20
        
        // Variety bonus
        val variety = listOf(hasLowercase, hasUppercase, hasDigit, hasSpecialChar).count { it }
        if (variety >= 3) score += 15
        if (variety == 4) score += 10
        
        // Pattern detection (penalty)
        if (hasSequentialChars(password)) {
            score -= 15
            feedback.add("Avoid sequential characters")
        }
        
        if (hasRepeatingChars(password)) {
            score -= 10
            feedback.add("Avoid repeating characters")
        }
        
        // Common password check (severe penalty)
        if (commonPasswords.contains(password.lowercase())) {
            score -= 50
            feedback.add("This is a common password")
        }
        
        // Add suggestions for improvement
        if (!hasUppercase && !hasLowercase) {
            feedback.add("Add letters")
        } else if (!hasUppercase) {
            feedback.add("Add uppercase letters")
        }
        
        if (!hasDigit) {
            feedback.add("Add numbers")
        }
        
        if (!hasSpecialChar) {
            feedback.add("Add special characters")
        }
        
        if (password.length < 12) {
            feedback.add("Make it longer")
        }
        
        // Normalize score to 0-100
        val normalizedScore = score.coerceIn(0, 100)
        
        return when {
            normalizedScore < 30 -> PasswordStrength.WEAK
            normalizedScore < 60 -> PasswordStrength.FAIR
            normalizedScore < 80 -> PasswordStrength.GOOD
            else -> PasswordStrength.STRONG
        }.apply {
            this.score = normalizedScore
            this.feedback = feedback.take(3) // Limit to top 3 suggestions
        }
    }
    
    /**
     * Check for sequential characters (e.g., "abc", "123")
     */
    private fun hasSequentialChars(password: String): Boolean {
        if (password.length < 3) return false
        
        for (i in 0 until password.length - 2) {
            val char1 = password[i].code
            val char2 = password[i + 1].code
            val char3 = password[i + 2].code
            
            if (char2 == char1 + 1 && char3 == char2 + 1) {
                return true
            }
            if (char2 == char1 - 1 && char3 == char2 - 1) {
                return true
            }
        }
        return false
    }
    
    /**
     * Check for repeating characters (e.g., "aaa", "111")
     */
    private fun hasRepeatingChars(password: String): Boolean {
        if (password.length < 3) return false
        
        for (i in 0 until password.length - 2) {
            if (password[i] == password[i + 1] && password[i] == password[i + 2]) {
                return true
            }
        }
        return false
    }
}

/**
 * Password strength levels
 */
enum class PasswordStrength(
    val label: String,
    val color: Color,
    val progress: Float
) {
    NONE("", Color.Transparent, 0f),
    WEAK("Weak", Color(0xFFE53935), 0.25f),
    FAIR("Fair", Color(0xFFFB8C00), 0.5f),
    GOOD("Good", Color(0xFFFDD835), 0.75f),
    STRONG("Strong", Color(0xFF43A047), 1f);
    
    var score: Int = 0
    var feedback: List<String> = emptyList()
}

