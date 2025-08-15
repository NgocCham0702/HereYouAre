package com.cham.appvitri.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cham.appvitri.model.UserModel

@Composable
private fun FriendSelectItem(
    friend: UserModel,
    isSelected: Boolean,
    onSelectionChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectionChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = AvatarHelper.getDrawableId(friend.profilePictureUrl)),
            contentDescription = friend.displayName,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Text(
            text = friend.displayName ?: "Người dùng",
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(checked = isSelected, onCheckedChange = { onSelectionChange() })
    }
}