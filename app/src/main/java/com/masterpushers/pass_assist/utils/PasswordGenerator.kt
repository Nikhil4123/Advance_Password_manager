package com.masterpushers.pass_assist.utils

import java.security.SecureRandom

/**
 * Generate secure random passwords with configurable options
 */
object PasswordGenerator {
    
    private val secureRandom = SecureRandom()
    
    // Character sets
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    
    // Exclude ambiguous characters that look similar
    private const val AMBIGUOUS_CHARS = "0O1lI"
    
    /**
     * Generate a password with given options
     */
    fun generate(options: PasswordGeneratorOptions = PasswordGeneratorOptions()): String {
        val charPool = buildCharacterPool(options)
        
        if (charPool.isEmpty()) {
            // Fallback to default if no character types selected
            return generate(PasswordGeneratorOptions())
        }
        
        val password = StringBuilder()
        
        // Ensure at least one character from each selected category
        if (options.includeLowercase) {
            val chars = getFilteredChars(LOWERCASE, options.excludeAmbiguous)
            password.append(chars[secureRandom.nextInt(chars.length)])
        }
        if (options.includeUppercase) {
            val chars = getFilteredChars(UPPERCASE, options.excludeAmbiguous)
            password.append(chars[secureRandom.nextInt(chars.length)])
        }
        if (options.includeDigits) {
            val chars = getFilteredChars(DIGITS, options.excludeAmbiguous)
            password.append(chars[secureRandom.nextInt(chars.length)])
        }
        if (options.includeSpecialChars) {
            password.append(SPECIAL_CHARS[secureRandom.nextInt(SPECIAL_CHARS.length)])
        }
        
        // Fill the rest with random characters from the pool
        val remainingLength = options.length - password.length
        repeat(remainingLength) {
            password.append(charPool[secureRandom.nextInt(charPool.length)])
        }
        
        // Shuffle to avoid predictable patterns
        val charList = password.toString().toMutableList()
        for (i in charList.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = charList[i]
            charList[i] = charList[j]
            charList[j] = temp
        }
        return charList.joinToString("")
    }
    
    /**
     * Build character pool based on options
     */
    private fun buildCharacterPool(options: PasswordGeneratorOptions): String {
        val pool = StringBuilder()
        
        if (options.includeLowercase) {
            pool.append(getFilteredChars(LOWERCASE, options.excludeAmbiguous))
        }
        if (options.includeUppercase) {
            pool.append(getFilteredChars(UPPERCASE, options.excludeAmbiguous))
        }
        if (options.includeDigits) {
            pool.append(getFilteredChars(DIGITS, options.excludeAmbiguous))
        }
        if (options.includeSpecialChars) {
            pool.append(SPECIAL_CHARS)
        }
        
        return pool.toString()
    }
    
    /**
     * Filter out ambiguous characters if requested
     */
    private fun getFilteredChars(chars: String, excludeAmbiguous: Boolean): String {
        return if (excludeAmbiguous) {
            chars.filter { it !in AMBIGUOUS_CHARS }
        } else {
            chars
        }
    }
    
    /**
     * Generate multiple password options at once
     */
    fun generateMultiple(
        count: Int = 3,
        options: PasswordGeneratorOptions = PasswordGeneratorOptions()
    ): List<String> {
        return List(count) { generate(options) }
    }
}

/**
 * Configuration options for password generation
 */
data class PasswordGeneratorOptions(
    val length: Int = 16,
    val includeLowercase: Boolean = true,
    val includeUppercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSpecialChars: Boolean = true,
    val excludeAmbiguous: Boolean = true
) {
    init {
        require(length in 8..32) { "Password length must be between 8 and 32" }
    }
}

