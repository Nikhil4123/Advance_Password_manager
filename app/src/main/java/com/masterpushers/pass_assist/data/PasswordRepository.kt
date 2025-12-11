package com.masterpushers.pass_assist.data

import android.content.Context
import com.masterpushers.pass_assist.utils.EncryptionUtils
import com.masterpushers.pass_assist.utils.SecureKeyStore
import kotlinx.coroutines.flow.Flow
import javax.crypto.SecretKey

class PasswordRepository private constructor(context: Context) {
    private val passwordDao = AppDatabase.getDatabase(context).passwordDao()
    
    // Use Android Keystore for secure key management
    private val encryptionKey: SecretKey = SecureKeyStore.getOrCreateMasterKey(context)
    
    // Use a singleton pattern to ensure we use the same repository instance
    companion object {
        @Volatile
        private var INSTANCE: PasswordRepository? = null
        
        fun getInstance(context: Context): PasswordRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PasswordRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    fun getAllPasswords(): Flow<List<PasswordEntity>> {
        return passwordDao.getAllPasswords()
    }
    
    suspend fun getPasswordById(id: Long): PasswordEntity? {
        return passwordDao.getPasswordById(id)
    }
    
    suspend fun insertPassword(accountType: String, username: String, password: String): Long {
        val encryptedPassword = EncryptionUtils.encrypt(password, encryptionKey)
        val passwordEntity = PasswordEntity(
            accountType = accountType,
            username = username,
            password = encryptedPassword
        )
        return passwordDao.insertPassword(passwordEntity)
    }
    
    suspend fun updatePassword(id: Long, accountType: String, username: String, password: String) {
        val encryptedPassword = EncryptionUtils.encrypt(password, encryptionKey)
        val passwordEntity = PasswordEntity(
            id = id,
            accountType = accountType,
            username = username,
            password = encryptedPassword,
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.updatePassword(passwordEntity)
    }
    
    suspend fun deletePassword(id: Long) {
        passwordDao.deletePasswordById(id)
    }
    
    fun decryptPassword(encryptedPassword: String): String {
        return try {
            EncryptionUtils.decrypt(encryptedPassword, encryptionKey)
        } catch (e: Exception) {
            // Handle decryption errors gracefully
            "Decryption failed"
        }
    }
}