// file: com/cham/appvitri/viewModel/HomeViewModelFactory.kt
package com.cham.appvitri.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.LocationHelper

class HomeViewModelFactory(
    private val locationHelper: LocationHelper,
    private val userRepository: UserRepository // Thêm
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(locationHelper, userRepository) as T // Truyền vào
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}