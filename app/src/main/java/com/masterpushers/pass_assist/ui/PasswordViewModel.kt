package com.masterpushers.pass_assist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.masterpushers.pass_assist.data.PasswordRepository
import kotlinx.coroutines.launch

class PasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PasswordRepository.getInstance(application)
    
    val allPasswords = repository.getAllPasswords().asLiveData()
    
    private val _decryptedPasswords = MutableLiveData<Map<Long, String>>()
    val decryptedPasswords: LiveData<Map<Long, String>> = _decryptedPasswords
    
    fun insert(accountType: String, username: String, password: String) = viewModelScope.launch {
        repository.insertPassword(accountType, username, password)
    }
    
    fun update(id: Long, accountType: String, username: String, password: String) = viewModelScope.launch {
        repository.updatePassword(id, accountType, username, password)
    }
    
    fun delete(id: Long) = viewModelScope.launch {
        repository.deletePassword(id)
    }
    
    fun decryptPassword(encryptedPassword: String): String {
        return repository.decryptPassword(encryptedPassword)
    }
    
    fun updateDecryptedPassword(id: Long, decryptedPassword: String) {
        val currentMap = _decryptedPasswords.value?.toMutableMap() ?: mutableMapOf()
        currentMap[id] = decryptedPassword
        _decryptedPasswords.value = currentMap
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PasswordViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PasswordViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}