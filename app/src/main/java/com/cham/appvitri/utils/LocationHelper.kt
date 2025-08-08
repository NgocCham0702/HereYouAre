package com.cham.appvitri.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationHelper (private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Hàm này sẽ lấy vị trí một lần duy nhất
    @SuppressLint("MissingPermission") // Đảm bảo bạn đã xin quyền trước khi gọi hàm này
    suspend fun fetchCurrentLocation(): LatLng? {
        return try {
            // Sử dụng CancellationToken để đảm bảo yêu cầu kết thúc
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            LatLng(location.latitude, location.longitude)
        } catch (e: Exception) {
            // Xử lý các lỗi có thể xảy ra (không có quyền, GPS tắt,...)
            null
        }
    }

    // HÀM MỚI: Cập nhật vị trí liên tục sử dụng Flow
    @SuppressLint("MissingPermission")
    fun trackLocation(): Flow<LatLng> = callbackFlow {
        // 1. Cấu hình yêu cầu cập nhật
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Cập nhật mỗi 10 giây
            fastestInterval = 5000 // Nhanh nhất là 5 giây
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // 2. Tạo một callback để nhận kết quả
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Gửi vị trí mới vào Flow
                    trySend(LatLng(location.latitude, location.longitude))
                }
            }
        }

        // 3. Bắt đầu lắng nghe cập nhật
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper() // Cần Looper
        )

        // 4. Khi Flow bị hủy (VD: màn hình bị đóng), dừng lắng nghe để tiết kiệm pin
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}