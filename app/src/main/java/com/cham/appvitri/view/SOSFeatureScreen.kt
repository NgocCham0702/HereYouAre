package com.cham.appvitri.view

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.R // Thay đổi package cho phù hợp
import com.cham.appvitri.viewModel.SOSViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import android.app.Application
import com.cham.appvitri.model.EmergencyContact
import com.cham.appvitri.viewModel.SOSViewModelFactory

// --- Composable chính cho toàn bộ màn hình SOS ---
@Composable
fun SOSScreen(onNavigateBack: () -> Unit) {
    val viewModel: SOSViewModel = viewModel(
        factory = SOSViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
    // Trạng thái để quản lý thời gian đếm ngược
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isCancelled) {
        if (uiState.isCancelled) {
            delay(2000)
            onNavigateBack()
        }
    }
    // Nền gradient tỏa tròn từ vàng sang hồng
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(Color(0xFFFBE9A7), Color(0xFFF9C5D1)),
        center = Offset(400f, 800f),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Hiển thị trạng thái dựa trên tin nhắn từ ViewModel
            StatusText(statusMessage = uiState.statusMessage, isCancelled = uiState.isCancelled)

            RadarAndCountdown(
                timeLeft = uiState.timeLeft,
                contacts = uiState.emergencyContacts,
                isCancelled = uiState.isCancelled
            )

            SafetyButton(
                onClick = { viewModel.cancelSos() },
                enabled = !uiState.isCancelled // Lấy từ state

            )
        }
    }
}
@Composable
private fun StatusText(statusMessage: String?, isCancelled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Thay đổi văn bản dựa trên trạng thái
        val title = when {
            statusMessage != null -> "Thông báo"
            isCancelled -> "Đã hủy"
            else -> "Đang gọi khẩn cấp..."
        }
        val description = statusMessage ?: if (isCancelled) {
            "Tín hiệu SOS đã được hủy. Bạn an toàn."
        } else {
            "Hệ thống sẽ gửi tín hiệu cầu cứu và vị trí của bạn sau khi đếm ngược kết thúc."
        }

        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun RadarAndCountdown(timeLeft: Int, contacts: List<EmergencyContact>, isCancelled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Chỉ hiển thị hiệu ứng radar khi chưa bị hủy
        if (!isCancelled) {
            RadarPulseAnimation()
            ContactsOnRadar(contacts = contacts)
        }

        // Hiển thị đồng hồ hoặc biểu tượng "an toàn"
        CountdownDisplay(timeLeft = timeLeft, isCancelled = isCancelled)
    }
}

@Composable
private fun CountdownDisplay(timeLeft: Int, isCancelled: Boolean) {
    val backgroundColor = if (isCancelled) Color(0xFF198754) else Color(0xFFFD8A8A)

    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.5f))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(backgroundColor), // Màu nền thay đổi
            contentAlignment = Alignment.Center
        ) {
            if (isCancelled) {
                // Hiển thị icon an toàn khi đã hủy
                Icon(
                    painter = painterResource(id = R.drawable.img_16), // Thay bằng icon check/khiên của bạn
                    contentDescription = "An toàn",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            } else {
                // Hiển thị đồng hồ đếm ngược
                Text(
                    text = String.format("%02d", timeLeft),
                    color = Color.White,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SafetyButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled, // <<< Điều khiển trạng thái nút
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF198754),
            disabledContainerColor = Color.Gray
        )
    ) {
        Text(
            text = "TÔI AN TOÀN",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RadarPulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")

    // Tạo 3 sóng lan tỏa với độ trễ khác nhau
    val pulseAnim1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse1"
    )
    val pulseAnim2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse2"
    )
    val pulseAnim3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveColor = Color(0xFFFDBFBF)
        drawCircle(
            color = waveColor,
            radius = size.minDimension / 2 * pulseAnim1,
            alpha = 1f - pulseAnim1,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = waveColor,
            radius = size.minDimension / 2 * pulseAnim2,
            alpha = 1f - pulseAnim2,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = waveColor,
            radius = size.minDimension / 2 * pulseAnim3,
            alpha = 1f - pulseAnim3,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
fun BoxScope.ContactsOnRadar(contacts: List<EmergencyContact>) {
    val infiniteTransition = rememberInfiniteTransition(label = "contact_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "angle"
    )

    val radius = 120.dp
    val angleStep = 360f / contacts.size

    contacts.forEachIndexed { index, contact ->
        val currentAngleRad = Math.toRadians((angle + index * angleStep).toDouble()).toFloat()
        val x = (radius.value * cos(currentAngleRad)).dp
        val y = (radius.value * sin(currentAngleRad)).dp

        Image(
            painter = painterResource(id = contact.avatarResId),
            contentDescription = contact.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = x, y = y)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(2.dp)
                .clip(CircleShape)
        )
    }
}

// --- Composable để xem trước trong Android Studio ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SOSScreenPreview() {
    SOSScreen(onNavigateBack = {})
}