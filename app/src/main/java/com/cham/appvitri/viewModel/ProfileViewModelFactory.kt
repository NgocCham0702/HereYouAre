package com.cham.appvitri.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository

class ProfileViewModelFactory(
    private val application: Application,
    private val userId: String,
    private val userRepository:UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application, userId, userRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}