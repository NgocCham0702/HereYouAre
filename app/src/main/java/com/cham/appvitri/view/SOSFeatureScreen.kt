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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cham.appvitri.R // Thay đổi package cho phù hợp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- Data class để biểu diễn một người bạn/liên hệ khẩn cấp ---
data class EmergencyContact(
    val name: String,
    @DrawableRes val avatarResId: Int
)

// --- Composable chính cho toàn bộ màn hình SOS ---
@Composable
fun SOSScreen() {
    // Trạng thái để quản lý thời gian đếm ngược
    var timeLeft by remember { mutableStateOf(10) }
    // Trạng thái để biết người dùng đã nhấn nút "An toàn" chưa
    var isCancelled by remember { mutableStateOf(false) }

    // Dữ liệu giả cho các liên hệ khẩn cấp
    val contacts = remember {
        listOf(
            EmergencyContact("Chị An", R.drawable.img),
            EmergencyContact("Chú Ba", R.drawable.img),
            EmergencyContact("Mẹ", R.drawable.img_10),
            EmergencyContact("Bố", R.drawable.img_9)
        )
    }

    // Coroutine để chạy đồng hồ đếm ngược
    LaunchedEffect(key1 = isCancelled) {
        // Nếu người dùng đã hủy, không chạy nữa
        if (isCancelled) return@LaunchedEffect

        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        // Khi đếm ngược xong, có thể thực hiện hành động tiếp theo ở đây
        // (ví dụ: chuyển sang màn hình "Đang chia sẻ vị trí")
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Phần văn bản trạng thái ở trên cùng
            StatusText()

            // Phần hiệu ứng radar và đồng hồ ở giữa
            RadarAndCountdown(
                timeLeft = timeLeft,
                contacts = contacts
            )

            // Nút "An toàn" ở dưới cùng
            SafetyButton(
                onClick = {
                    isCancelled = true
                }
            )
        }
    }
}

@Composable
private fun StatusText() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Đang gọi khẩn cấp...",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vui lòng đợi, hệ thống đang gửi tín hiệu cầu cứu và vị trí của bạn đến các liên hệ khẩn cấp.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun RadarAndCountdown(timeLeft: Int, contacts: List<EmergencyContact>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Đảm bảo khu vực này là hình vuông
        contentAlignment = Alignment.Center
    ) {
        // Hiệu ứng sóng radar lan tỏa
        RadarPulseAnimation()

        // Hiển thị các avatar trên sóng radar
        ContactsOnRadar(contacts = contacts)

        // Đồng hồ đếm ngược ở trung tâm
        CountdownDisplay(timeLeft = timeLeft)
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


@Composable
private fun CountdownDisplay(timeLeft: Int) {
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
                .background(Color(0xFFFD8A8A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                // Định dạng số để luôn có 2 chữ số, ví dụ: "09", "08"...
                text = String.format("%02d", timeLeft),
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SafetyButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF198754) // Màu xanh lá cây an toàn
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


// --- Composable để xem trước trong Android Studio ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SOSScreenPreview() {
    SOSScreen()
}