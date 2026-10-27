package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MissedRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppUpdateInfo
import com.example.util.AppUpdateManager

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadProgress: Float?,
    errorMessage: String?,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDownloading = downloadProgress != null
    val isDark = isSystemInDarkTheme()

    // Highly readable, contrast-enforced color palette guaranteed to be crisp in both Dark and Light modes
    val surfaceColor = if (isDark) Color(0xFF1B2724) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF324A43) else Color(0xFFCBD5E1)
    val textPrimaryColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val notesSurfaceColor = if (isDark) Color(0xFF111A18) else Color(0xFFF1F5F9)
    val notesBorderColor = if (isDark) Color(0xFF273C36) else Color(0xFFCBD5E1)
    val notesTitleColor = if (isDark) Color(0xFF34D399) else EmeraldPrimary
    val notesBodyColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)
    val dismissBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    val dismissTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with App Icon Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = if (isDark) 0.35f else 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yeni Sürüm Hazır!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = updateInfo.latestVersionTag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${updateInfo.apkSizeFormatted}",
                                fontSize = 11.sp,
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF2A3D38) else Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Release Title & Notes Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yenilikler:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = notesTitleColor
                        )
                    }

                    Surface(
                        onClick = {
                            AppUpdateManager.openInBrowser(context, "https://github.com/bilkil53/aile-ile-namaz/releases/latest")
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isDark) Color(0xFF34D399) else EmeraldPrimary).copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "GitHub (latest)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF34D399) else EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Filled.OpenInNew,
                                contentDescription = "Son Açıklama",
                                tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-Contrast, Clean Readable Release Notes Box (Always clearly visible in dark theme)
                val formattedNotes = remember(updateInfo.releaseNotes) {
                    val raw = if (updateInfo.releaseNotes.isBlank() || updateInfo.releaseNotes.length < 25) {
                        com.example.util.AppUpdateManager.DEFAULT_FULL_RELEASE_NOTES
                    } else {
                        updateInfo.releaseNotes
                    }
                    raw.lines()
                        .map { it.trim().removePrefix("##").removePrefix("#").replace(Regex("(?i)\\s*\\(?build\\s*\\d+\\)?"), "").trim() }
                        .filter { it.isNotBlank() }
                }

                Surface(
                    color = notesSurfaceColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, notesBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formattedNotes.forEach { line ->
                            val cleanLine = line
                                .replace("**", "")
                                .removePrefix("*")
                                .removePrefix("-")
                                .trim()

                            val isTitle = line.startsWith("🕌") || line.startsWith("Sürüm") || line.contains("Yenilikleri")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (!isTitle) {
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                                Text(
                                    text = cleanLine,
                                    fontSize = if (isTitle) 13.sp else 12.5.sp,
                                    fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTitle) notesTitleColor else notesBodyColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Direct Clickable Link to GitHub latest release page
                Surface(
                    onClick = {
                        AppUpdateManager.openInBrowser(context, "https://github.com/bilkil53/aile-ile-namaz/releases/latest")
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Son yüklenen resmi açıklama için dokunun (GitHub latest)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF34D399) else EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Download Progress or Error State
                if (isDownloading) {
                    val progress = downloadProgress ?: 0f
                    val percent = (progress * 100).toInt()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Güncelleme indiriliyor...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimaryColor
                            )
                            Text(
                                text = "%$percent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF34D399) else EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                            trackColor = EmeraldPrimary.copy(alpha = if (isDark) 0.3f else 0.2f),
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "İndirme tamamlandığında doğrudan kurulum ekranı açılacaktır.",
                            fontSize = 10.sp,
                            color = textSecondaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (errorMessage != null) {
                    Surface(
                        color = MissedRed.copy(alpha = if (isDark) 0.2f else 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MissedRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = if (isDark) Color(0xFFFCA5A5) else MissedRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, dismissBorderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Daha Sonra", fontSize = 13.sp, color = textSecondaryColor)
                        }

                        Button(
                            onClick = onStartDownload,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Şimdi Güncelle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        // While downloading: show browser fallback option
                        OutlinedButton(
                            onClick = {
                                AppUpdateManager.openInBrowser(context, updateInfo.apkDownloadUrl)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tarayıcıda İndir", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
