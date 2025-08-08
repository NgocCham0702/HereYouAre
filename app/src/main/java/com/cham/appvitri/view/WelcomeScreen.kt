// File: view/onboarding/WelcomeScreen.kt

package com.cham.appvitri.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cham.appvitri.R

@Composable
fun WelcomeScreen(
navController: NavController) {
    // Dùng Box để dễ dàng xếp chồng và căn chỉnh các phần tử
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        // --- Phần nội dung chính (logo và text) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center) // Căn giữa theo chiều dọc và ngang
            .offset(y = (-80).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Logo
            Image(
                painter = painterResource(id = R.drawable.logo), // Cần thêm ảnh logo này
                contentDescription = "App Logo",
                modifier = Modifier.size(150.dp) // Chỉnh kích thước logo
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Chữ "welcome to"
            Text(
                text = "welcome to",
                fontSize = 20.sp,
                color = Color.Gray
            )

            // 3. Tên ứng dụng "HOMEEE"
            Text(
                text = "HEREWEARE",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }

        // --- Phần dưới cùng (nút và điều khoản) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter), // Căn ở dưới cùng
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4. Nút Đăng nhập
            Button(
                onClick = {navController.navigate("login")},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50), // Bo tròn hoàn toàn
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0C419) // Màu vàng giống trong ảnh
                )
            ) {
                Text(
                    text = "BẮT ĐẦU",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Điều khoản và Chính sách
            LegalText(
                onTermsClick = { url -> Log.d("WelcomeScreen", "chuyển hướng : $url") },
                onPrivacyClick = { url -> Log.d("WelcomeScreen", "đìeefu khoản: $url") }
            )
        }
    }
}

@Composable
fun LegalText( onTermsClick: (String) -> Unit,
               onPrivacyClick: (String) -> Unit) {
    //val uriHandler = LocalUriHandler.current // Cách chuẩn để mở URL
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 12.sp)) {
            append("By signing up, you are agree to our\n")
        }
        withStyle(style = SpanStyle(color = Color(0xFF0052D4), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)) {
            pushStringAnnotation(tag = "TOS", annotation = "https://yourcompany.com/terms")
            append("Terms of Service")
            pop()
        }
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 12.sp)) {
            append(" & ")
        }
        withStyle(style = SpanStyle(color = Color(0xFF0052D4), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)) {
            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy_policy_url")
            append("privacy")
            pop()
        }
    }
    ClickableText(
        text = annotatedString,
        style = LocalTextStyle.current.copy(textAlign = TextAlign.Center), // Áp dụng style căn giữa
        onClick = { offset ->
            // Lambda 'onClick' của ClickableText cung cấp sẵn 'offset'
            annotatedString.getStringAnnotations(tag = "TOS", start = offset, end = offset)
                .firstOrNull()?.let {
                    // Mở URL bằng cách chuẩn của Compose
                    // uriHandler.openUri(it.item)

                    // Hoặc gọi callback để xử lý ở tầng cao hơn
                    onTermsClick(it.item)
                }

            annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                .firstOrNull()?.let {
                    // uriHandler.openUri(it.item)
                    onPrivacyClick(it.item)
                }
        }
    )


}


@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(navController = rememberNavController())
}