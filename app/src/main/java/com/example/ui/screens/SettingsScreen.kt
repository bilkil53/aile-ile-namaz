package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OnboardingLoginDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusMessage by viewModel.statusMessage.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }
    val isLoggedIn = appSettings.isLoggedIn || appSettings.userEmail.isNotBlank()

    LaunchedEffect(Unit) {
        viewModel.checkForAppUpdate(silent = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status banner if present
        if (statusMessage != null) {
            item {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage ?: "",
                            color = OnEmeraldContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Ana Kart: Uygulama Bilgileri (Çam Yeşili #0E5A4A Arka Plan ve Beyaz Yazılar)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Kart Başlığı
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Uygulama Bilgileri",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Aile ile Namaz",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Uygulama Adı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Uygulama", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(text = "Aile ile Namaz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    // Sürüm ve Kesintisiz Güncelleme Sorgulama
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bilkil53/aile-ile-namaz/releases/latest")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sürüm", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = com.example.BuildConfig.VERSION_NAME,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        if (availableUpdate != null && availableUpdate?.hasUpdate == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Yeni Sürüm Yayında: ${availableUpdate?.latestVersionTag}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                        Text("İndirip güncellemek için dokunun.", fontSize = 10.sp, color = Color(0xFFB45309))
                                    }
                                    Button(
                                        onClick = { viewModel.checkForAppUpdate(silent = false, forceShow = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Güncelle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.checkForAppUpdate(silent = false, forceShow = true) },
                                enabled = !isCheckingForUpdate,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingForUpdate) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sorgulanıyor...", fontSize = 11.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Güncellemeleri Kontrol Et", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    // Veritabanı ve Senkronizasyon Durumu
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Veritabanı Durumu", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                            Text(
                                text = if (isLoggedIn) "Bulut Senkronize ☁️" else "Yerel Cihaz Hafızası 📱",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLoggedIn) Color(0xFFC8E6C9) else Color(0xFFFFE082)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Detaylı Bilgilendirme ve E-Posta Yedekleme Alanı
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isLoggedIn) Icons.Filled.CloudDone else Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isLoggedIn) Color(0xFFC8E6C9) else Color(0xFFFFE082),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isLoggedIn) {
                                            "Hesaba Bağlı Bulut Yedekleme Aktif"
                                        } else {
                                            "Yalnızca Bu Cihazda Kayıtlı (Yerel)"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = if (isLoggedIn) {
                                        "Kıldı/kılmadı işaretlemeleriniz, kaza namazlarınız ve tüm ibadet geçmişiniz ${appSettings.userEmail} Google hesabınıza anlık olarak yedeklenmektedir. Uygulamayı silseniz veya telefon değiştirseniz bile bu e-posta ile giriş yaptığınızda bütün kayıtlarınız eksiksiz geri yüklenir."
                                    } else {
                                        "Kıldı/kılmadı ve kaza kayıtlarınız şu an sadece bu telefonda tutulmaktadır. Telefonu değiştirdiğinizde veya sildiğinizde verilerinizin kaybolmaması için Google hesabınızla giriş yaparak e-postanıza yedekleyebilirsiniz."
                                    },
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.90f),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isLoggedIn) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.backupNowToCloud() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.22f),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Şimdi Yedekle ☁️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.logoutUser() },
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Çıkış Yap", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { showLoginDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Google ile Giriş Yap & Buluta Yedekle 🔐", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    // Geliştirici
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Geliştirici", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(
                            text = "bilkil53",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    // Açık Kaynak Proje & GitHub Butonu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Proje & Kaynak Kod", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        }

                        Surface(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bilkil53/aile-ile-namaz/releases/latest")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    try {
                                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bilkil53/aile-ile-namaz/releases")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(fallbackIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Tarayıcı açılamadı.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Detaylar & Son Sürüm Notları",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.OpenInNew,
                                    contentDescription = "GitHub Son Sürüm",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    // Öneri ve Destek Bölümü
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Öneri & Destek", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        }

                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:e.bilkil5391@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Öneri - Aile ile Namaz")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Selamün aleyküm,\n\nAile ile Namaz uygulaması için önerim / geri bildirimim:\n\n"
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Öneri Gönder"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "E-posta uygulaması bulunamadı.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Öneri Yaz ✉️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showLoginDialog) {
        OnboardingLoginDialog(
            viewModel = viewModel,
            onDismiss = { showLoginDialog = false }
        )
    }
}
