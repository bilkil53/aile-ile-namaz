package com.example.ui.components

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun OnboardingLoginDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    // Google Account Picker Launcher
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = accountName.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                viewModel.loginWithGoogleBrowser(email = accountName, name = displayName)
            } else {
                viewModel.loginWithGoogleBrowser()
            }
        } else {
            // Doğrudan varsayılan hesapla giriş yap
            viewModel.loginWithGoogleBrowser()
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Mosque,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aile ile Namaz'a Hoş Geldiniz",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Birlikte namaz kılın, kaza borçlarınızı takip edin ve ortak ibadet havuzunda buluşun 🤲",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Sekmeler Tanıtım Kartları
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IntroFeatureItem(
                        icon = Icons.Filled.Home,
                        title = "Bugün Sekmesi (Vakitler & Kıble)",
                        description = "Diyanet uyumlu tam vakitler, 5 vakit namaz kılındı takibi ve canlı manyetik kıble pusulası.",
                        badgeColor = EmeraldPrimary
                    )

                    IntroFeatureItem(
                        icon = Icons.Filled.People,
                        title = "Aile Havuzu Sekmesi (İbadet Halkası)",
                        description = "Aileniz, eşiniz ve çocuklarınızla tek davet kodu ile bağlanın; canlı namaz durumlarını ortaklaşa görüntüleyin.",
                        badgeColor = Color(0xFF0284C7)
                    )

                    IntroFeatureItem(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Kaza Sekmesi (Mahremiyet Korumalı)",
                        description = "Kaza borçlarınızı pratik şekilde tutun; aile fertlerinizin kaza durumunu mahremiyet korumalı özet olarak görün.",
                        badgeColor = Color(0xFFD97706)
                    )

                    IntroFeatureItem(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Karne Sekmesi (İstatistik & Motivasyon)",
                        description = "Haftalık ve aylık namaz kılma oranlarınız, günün ayeti ve manevi gelişim karneniz.",
                        badgeColor = Color(0xFF9333EA)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Büyük Google ile Giriş Yap Butonu
                Card(
                    onClick = {
                        try {
                            val chooseAccountIntent = AccountManager.newChooseAccountIntent(
                                null, null, arrayOf("com.google"), null, null, null, null
                            )
                            googleAccountPickerLauncher.launch(chooseAccountIntent)
                        } catch (_: Exception) {
                            viewModel.loginWithGoogleBrowser()
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_google_login_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google ile Hızlı Giriş Yap",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Aile Havuzu sekmesinde tekrar girişe gerek kalmaz",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Giriş Yapmadan Devam Et Butonu
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Giriş Yapmadan Devam Et",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    badgeColor: Color
) {
    Surface(
        color = WarmCreamBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, SoftInfoCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
