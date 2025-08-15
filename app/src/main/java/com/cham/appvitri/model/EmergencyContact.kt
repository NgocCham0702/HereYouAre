package com.cham.appvitri.model // Hoặc package tương ứng của bạn

import androidx.annotation.DrawableRes

/**
 * Data class này chỉ dùng cho UI, để biểu diễn một liên hệ
 * sẽ được hiển thị trên màn hình SOS.
 */
data class EmergencyContact(
    val name: String,
    @DrawableRes val avatarResId: Int
)