// file: com/cham/appvitri/utils/AvatarHelper.kt
package com.cham.appvitri.utils

import com.cham.appvitri.R

object AvatarHelper {
    // Ánh xạ từ tên định danh (String) sang ID tài nguyên (Int)
    val avatars = mapOf(
        "avatar_1" to R.drawable.avatar_1,
        "avatar_2" to R.drawable.avatar_2,
        "default" to R.drawable.no
    )

    fun getDrawableId(identifier: String?): Int {
        return avatars[identifier] ?: R.drawable.no // Trả về ảnh mặc định
    }
}