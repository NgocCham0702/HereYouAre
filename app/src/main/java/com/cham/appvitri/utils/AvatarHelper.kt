// file: com/cham/appvitri/utils/AvatarHelper.kt
package com.cham.appvitri.utils

import com.cham.appvitri.R

object AvatarHelper {
    // Danh sách các avatar có sẵn. Key là String để lưu vào Firestore,
    // Value là ID của ảnh trong drawable.
    val avatars = mapOf(
        "default" to R.drawable.img, // Thay bằng tên ảnh mặc định của bạn
        "avatar_1" to R.drawable.avatar_1,
        "avatar_2" to R.drawable.avatar_2,
        "avatar_3" to R.drawable.img_15
        // Thêm các avatar khác của bạn vào đây
    )

    /**
     * Lấy ID tài nguyên drawable từ tên định danh (key).
     * @param identifier Tên của avatar, ví dụ "avatar_1".
     * @return ID tài nguyên, ví dụ R.drawable.avatar_1.
     */
    fun getDrawableId(identifier: String?): Int {
        return avatars[identifier] ?: R.drawable.no // Trả về ảnh mặc định nếu không tìm thấy
    }
}