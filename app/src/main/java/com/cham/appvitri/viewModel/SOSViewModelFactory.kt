package com.cham.appvitri.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SOSViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SOSViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SOSViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}