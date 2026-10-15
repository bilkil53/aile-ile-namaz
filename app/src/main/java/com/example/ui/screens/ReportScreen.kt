package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DailyPrayerEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recentPrayers by viewModel.recentDailyPrayers.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val todayPrayer by viewModel.todayPrayer.collectAsState()
    val kazaPrayers by viewModel.kazaPrayers.collectAsState()
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()

    // Calculate weekly statistics (last 7 days)
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale("tr")) }
    val displayDateFormat = remember { SimpleDateFormat("d MMM", Locale("tr")) }

    val last7DaysData = remember(recentPrayers, todayPrayer) {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<DayReportData>()
        
        for (i in 0 until 7) {
            val dateStr = dayFormat.format(calendar.time)
            val dayName = dayNameFormat.format(calendar.time).replace(".", "").replaceFirstChar { it.uppercase() }
            val displayDate = displayDateFormat.format(calendar.time)

            val prayerEntity = if (dateStr == todayPrayer.date) {
                todayPrayer
            } else {
                recentPrayers.find { it.date == dateStr } ?: DailyPrayerEntity(date = dateStr)
            }

            val fajr = prayerEntity.fajrStatus
            val dhuhr = prayerEntity.dhuhrStatus
            val asr = prayerEntity.asrStatus
            val maghrib = prayerEntity.maghribStatus
            val isha = prayerEntity.ishaStatus
            val statuses = listOf(fajr, dhuhr, asr, maghrib, isha)
            val completedCount = statuses.count { it == "PRAYED" }

            list.add(
                DayReportData(
                    date = dateStr,
                    dayName = dayName,
                    displayDate = displayDate,
                    completedPrayers = completedCount,
                    isToday = i == 0,
                    fajr = fajr,
                    dhuhr = dhuhr,
                    asr = asr,
                    maghrib = maghrib,
                    isha = isha
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reversed() // From 6 days ago to today
    }

    var selectedDayDate by remember(last7DaysData) {
        mutableStateOf(last7DaysData.lastOrNull()?.date)
    }

    val totalWeekPrayed = remember(last7DaysData) {
        last7DaysData.sumOf { it.completedPrayers }
    }
    val totalWeekPossible = 35
    val weeklySuccessRate = remember(totalWeekPrayed) {
        ((totalWeekPrayed.toFloat() / totalWeekPossible) * 100).toInt().coerceIn(0, 100)
    }

    val totalCompletedKaza = remember(kazaPrayers) {
        kazaPrayers.sumOf { it.completedCount }
    }

    val activeDay = remember(selectedDayDate, last7DaysData) {
        last7DaysData.find { it.date == selectedDayDate } ?: last7DaysData.lastOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = if (settings.isLoggedIn && settings.userName.isNotBlank()) "KULLANICI: ${settings.userName.uppercase()}" else "HAFTALIK MANEVİ KARNE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (settings.isLoggedIn && settings.userName.isNotBlank()) "${settings.userName} · Karnesi" else "İbadet Karnesi & Rozetler",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (settings.isLoggedIn && settings.userEmail.isNotBlank()) "${settings.userEmail} · İbadette istikrar, secdede huzur." else "İbadette istikrar, secdede manevi huzur.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Main Weekly Summary Pine Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Haftalık Namaz Başarısı",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "%$weeklySuccessRate Başarı Oranı",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = GoldContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { weeklySuccessRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Metric Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReportMetricItem(
                            label = "Kılınan Vakit",
                            value = "$totalWeekPrayed / $totalWeekPossible"
                        )
                        ReportMetricItem(
                            label = "Eda Edilen Kaza",
                            value = "$totalCompletedKaza Vakit"
                        )
                        ReportMetricItem(
                            label = "Aile Uyumu",
                            value = if (partnerInfo.isMatched) "%85 Ortak" else "Tekil"
                        )
                    }
                }
            }
        }

        // Daily Breakdown Chart
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Son 7 Günün Vakit Dağılımı",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Günde 5 vaktin tamamlanma grafiği:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysData.forEach { day ->
                            DayBarColumn(
                                day = day,
                                isSelected = (day.date == selectedDayDate),
                                onClick = { selectedDayDate = day.date }
                            )
                        }
                    }
                }
            }
        }

        // Selected Day Details Card (Detailed Breakdown by Prayer)
        if (activeDay != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (activeDay.isToday) "Bugünün Vakit Detayı" else "${activeDay.dayName} (${activeDay.displayDate}) Detayı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Grafikte seçilen günün 5 vakit dökümü:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (activeDay.completedPrayers == 5) "5/5 Tamamlandı 🌟" else "${activeDay.completedPrayers}/5 Vakit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeDay.completedPrayers == 5) EmeraldPrimary else if (activeDay.completedPrayers > 0) TextPrimary else MissedRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5 prayer detail items
                        val prayerList = listOf(
                            Triple("Sabah", activeDay.fajr, "Güneş doğmadan önceki vaktin bereketi"),
                            Triple("Öğle", activeDay.dhuhr, "Günün ortasındaki manevi mola"),
                            Triple("İkindi", activeDay.asr, "Günün en kıymetli secde vakti"),
                            Triple("Akşam", activeDay.maghrib, "Günün batışıyla ifa edilen şükür"),
                            Triple("Yatsı", activeDay.isha, "Günün son huzur secdesi")
                        )

                        prayerList.forEachIndexed { index, (name, status, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = when (status) {
                                            "PRAYED" -> PrayedGreenBg
                                            "MISSED" -> MissedRedBg
                                            "EXCUSED" -> ExcusedGrayBg
                                            else -> Color(0xFFF1F5F3)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (status) {
                                                    "PRAYED" -> Icons.Filled.Check
                                                    "MISSED" -> Icons.Filled.Close
                                                    "EXCUSED" -> Icons.Filled.Remove
                                                    else -> Icons.Filled.AccessTime
                                                },
                                                contentDescription = null,
                                                tint = when (status) {
                                                    "PRAYED" -> PrayedGreen
                                                    "MISSED" -> MissedRed
                                                    "EXCUSED" -> ExcusedGray
                                                    else -> TextMuted
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (status) {
                                        "PRAYED" -> PrayedGreenBg
                                        "MISSED" -> MissedRedBg
                                        "EXCUSED" -> ExcusedGrayBg
                                        else -> Color(0xFFF1F5F3)
                                    }
                                ) {
                                    Text(
                                        text = when (status) {
                                            "PRAYED" -> "Kılındı ✓"
                                            "MISSED" -> "Kaza (Kılınmadı)"
                                            "EXCUSED" -> "Muaf"
                                            else -> "—"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            "PRAYED" -> PrayedGreen
                                            "MISSED" -> MissedRed
                                            "EXCUSED" -> ExcusedGray
                                            else -> TextMuted
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (index < 4) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Partner Harmony Card (if matched)
        if (partnerInfo.isMatched) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aile Fertleri ile Birlikte Secde",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${partnerInfo.partnerDisplayName} ile bu hafta ortak namaz kıldınız. Tebrikler!",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.88f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Section: Spiritual Badges
        item {
            Text(
                text = "Manevi Motivasyon Rozetleri",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // Badges Grid / Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiritualBadgeRow(
                    title = "Sabah Namazı Muhafızı",
                    description = "Sabah namazlarını vaktinde kaçırmadan eda etme sadakati.",
                    icon = Icons.Filled.WbSunny,
                    isUnlocked = totalWeekPrayed >= 5,
                    badgeColor = GoldAccent
                )
                SpiritualBadgeRow(
                    title = "5'te 5 Sadakati",
                    description = "Günü eksiksiz olarak 5 vakit namaz ile tamamlama.",
                    icon = Icons.Filled.CheckCircle,
                    isUnlocked = totalWeekPrayed >= 15,
                    badgeColor = EmeraldPrimary
                )
                SpiritualBadgeRow(
                    title = "Kaza Avcısı",
                    description = "Kaza namazı borçlarını düzenli ve gayretle kılma azmi.",
                    icon = Icons.Filled.Bolt,
                    isUnlocked = totalCompletedKaza >= 5,
                    badgeColor = Color(0xFFE65100)
                )
                SpiritualBadgeRow(
                    title = "İbadet Kardeşliği",
                    description = "Aile fertleriyle birlikte ibadet takibi sürdürme.",
                    icon = Icons.Filled.People,
                    isUnlocked = partnerInfo.isMatched,
                    badgeColor = Color(0xFF0284C7)
                )
                SpiritualBadgeRow(
                    title = "İstikrar Yıldızı",
                    description = "Haftalık %80 üzerinde genel namaz başarı oranına ulaşma.",
                    icon = Icons.Filled.Star,
                    isUnlocked = weeklySuccessRate >= 80,
                    badgeColor = Color(0xFF9333EA)
                )
            }
        }

        // Spiritual Quote & App Info footer
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Günün Hadis-i Şerifi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"Allah katında amellerin en sevimlisi, az da olsa devamlı olanıdır.\" (Buhârî, Îmân, 32)",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 17.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Konum: ${settings.cityName} (Diyanet)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Aile ile Namaz ${com.example.BuildConfig.VERSION_NAME}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // App Version & Automatic GitHub Updater Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Uygulama Güncellemeleri",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "GitHub üzerinden otomatik APK güncellemesi",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        if (availableUpdate != null && availableUpdate!!.hasUpdate) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "Yeni!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.checkForAppUpdate(silent = false) },
                        enabled = !isCheckingForUpdate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingForUpdate) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub Kontrol Ediliyor...", fontSize = 13.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        } else if (availableUpdate != null && availableUpdate!!.hasUpdate) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yeni Sürümü İndir (${availableUpdate!!.latestVersionTag})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Güncellemeleri Denetle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }
}

data class DayReportData(
    val date: String,
    val dayName: String,
    val displayDate: String,
    val completedPrayers: Int,
    val isToday: Boolean,
    val fajr: String = "NONE",
    val dhuhr: String = "NONE",
    val asr: String = "NONE",
    val maghrib: String = "NONE",
    val isha: String = "NONE"
)

@Composable
private fun ReportMetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DayBarColumn(
    day: DayReportData,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${day.completedPrayers}/5",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // Bar representation (max height 70dp)
        val barHeight = ((day.completedPrayers / 5f) * 65).coerceAtLeast(10f).dp
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    if (day.completedPrayers == 5) GoldAccent
                    else if (day.completedPrayers > 0) Color.White.copy(alpha = 0.7f)
                    else Color.White.copy(alpha = 0.2f)
                )
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = day.dayName,
            fontSize = 11.sp,
            fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = Color.White
        )
        Text(
            text = day.displayDate,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun SpiritualBadgeRow(
    title: String,
    description: String,
    icon: ImageVector,
    isUnlocked: Boolean,
    badgeColor: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isUnlocked) GoldContainer else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isUnlocked) {
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Kazanıldı ✓",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "(Kilitli)",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
