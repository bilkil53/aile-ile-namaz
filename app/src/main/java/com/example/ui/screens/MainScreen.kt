package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.ui.components.MandatoryNicknameDialog
import com.example.ui.components.OnboardingLoginDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

enum class AppTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    TRACKER("Bugün", Icons.Filled.Home, Icons.Outlined.Home, "tab_today"),
    PARTNER("Aile Havuzu", Icons.Filled.Diversity1, Icons.Outlined.Diversity1, "tab_partner"),
    KAZA("Kaza", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_kaza"),
    REPORT("Karne", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "tab_report")
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.TRACKER) }
    val settings by viewModel.settings.collectAsState()
    var hasDismissedOnboarding by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // If on settings screen and back pressed, return to main
    BackHandler(enabled = showSettingsScreen) {
        showSettingsScreen = false
    }

    // If on another tab and back pressed, return to TRACKER
    BackHandler(enabled = !showSettingsScreen && selectedTab != AppTab.TRACKER) {
        selectedTab = AppTab.TRACKER
    }

    if (showSettingsScreen) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(WarmCreamBackground),
            containerColor = WarmCreamBackground,
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                Surface(
                    color = EmeraldPrimary,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { showSettingsScreen = false },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Geri",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ayarlar",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Uygulama Bilgileri",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground),
        containerColor = WarmCreamBackground,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Surface(
                color = EmeraldPrimary,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Mosque,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Aile ile Namaz",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (selectedTab) {
                                    AppTab.TRACKER -> "Vakitler & Günlük İbadet"
                                    AppTab.PARTNER -> "Aile Havuzu (İbadet Halkası)"
                                    AppTab.KAZA -> "Kaza Namazı Yönetimi"
                                    AppTab.REPORT -> "İbadet Karnesi & İstatistik"
                                },
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Status Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Aktif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings Icon Button
                        Surface(
                            onClick = { showSettingsScreen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.size(34.dp).testTag("topbar_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Ayarlar ve Güncellemeler",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Clean Bottom Navigation Bar Matching Images 1 & 2
            Surface(
                color = PureWhite,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorderColor)
            ) {
                NavigationBar(
                    containerColor = PureWhite,
                    tonalElevation = 0.dp
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = Color.Transparent, // No pill, matches screenshot
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBackground)
                .padding(paddingValues)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_transition") { tab ->
                when (tab) {
                    AppTab.TRACKER -> TrackerScreen(
                        viewModel = viewModel,
                        onNavigateToPartner = { selectedTab = AppTab.PARTNER },
                        onRequestPermissions = onRequestPermissions
                    )
                    AppTab.PARTNER -> PartnerScreen(viewModel = viewModel)
                    AppTab.KAZA -> KazaScreen(viewModel = viewModel)
                    AppTab.REPORT -> ReportScreen(viewModel = viewModel)
                }
            }

            // In-App GitHub APK Update Dialog
            val availableUpdate by viewModel.availableUpdate.collectAsState()
            val downloadProgress by viewModel.downloadProgress.collectAsState()
            val updateDownloadError by viewModel.updateDownloadError.collectAsState()

            if (availableUpdate != null && availableUpdate?.hasUpdate == true) {
                com.example.ui.components.AppUpdateDialog(
                    updateInfo = availableUpdate!!,
                    downloadProgress = downloadProgress,
                    errorMessage = updateDownloadError,
                    onStartDownload = { viewModel.startApkUpdateDownload() },
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }

            // Açılışta Tanıtım & Hızlı Google Login Ekranı
            if (!settings.isLoggedIn && !hasDismissedOnboarding) {
                OnboardingLoginDialog(
                    viewModel = viewModel,
                    onDismiss = { hasDismissedOnboarding = true }
                )
            }

            // Login Olduktan Sonra Zorunlu Takma Ad / İsim Belirleme Ekranı
            val childPrefs = remember {
                context.getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE)
            }
            var refreshNickTrigger by remember { mutableStateOf(0) }
            val currentEmailKey = settings.userEmail.trim().lowercase()
            val hasConfirmedNickname = remember(settings.userName, settings.userEmail, refreshNickTrigger) {
                if (currentEmailKey.isBlank()) {
                    false
                } else {
                    val hasConfirmedForEmail = childPrefs.getBoolean("confirmed_nickname_v3_$currentEmailKey", false)
                    val savedNick = childPrefs.getString("user_nickname_$currentEmailKey", "") ?: ""
                    hasConfirmedForEmail && savedNick.isNotBlank() && !savedNick.contains("@") && savedNick != "Mümin"
                }
            }
            val needsMandatoryNickname = (settings.isLoggedIn || settings.userEmail.isNotBlank()) && !hasConfirmedNickname

            if (needsMandatoryNickname) {
                MandatoryNicknameDialog(
                    viewModel = viewModel,
                    userEmail = settings.userEmail,
                    currentName = settings.userName,
                    onConfirmed = { confirmedName, icon ->
                        refreshNickTrigger++
                    }
                )
            }
        }
    }
}
