package com.example.ui.screens

import android.accounts.AccountManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.PrayerCalculator
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PartnerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val todayPrayer by viewModel.todayPrayer.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshingPartner.collectAsState()

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayDateStr = remember {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
    }

    var refreshTrigger by remember { mutableStateOf(0) }

    val childPrefs = remember {
        val prefs = context.getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("custom_extra_member_ids")) {
            prefs.edit().apply {
                putString("custom_extra_member_ids", "3,4")
                if (!prefs.contains("p3_name")) putString("p3_name", "Kişi 3")
                if (!prefs.contains("p3_icon")) putString("p3_icon", "👦")
                putBoolean("has_p3", true)
                if (!prefs.contains("p4_name")) putString("p4_name", "Kişi 4")
                if (!prefs.contains("p4_icon")) putString("p4_icon", "👧")
                putBoolean("has_p4", true)
                apply()
            }
        }
        prefs
    }

    // 1. Kişinin kodu üzerinden aile havuzu kodu oluştur / yükle
    var familyPoolCode by remember(partnerInfo.myInviteCode) {
        val saved = childPrefs.getString("family_pool_code", null)
        mutableStateOf(saved ?: partnerInfo.myInviteCode.ifBlank { "EMB-5385" })
    }

    // Kişi 1 (Siz)
    var person1Icon by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p1_icon", "🧔") ?: "🧔") }

    // Kişi 2
    var hasPerson2 by remember(partnerInfo.isMatched, refreshTrigger) {
        mutableStateOf(partnerInfo.isMatched || childPrefs.getBoolean("has_p2", true))
    }
    var person2Name by remember(refreshTrigger) {
        val defaultName = partnerInfo.partnerDisplayName.ifBlank { "Kişi 2" }
        mutableStateOf(childPrefs.getString("p2_name", defaultName) ?: defaultName)
    }
    var person2Icon by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_icon", "🧕") ?: "🧕") }

    // Ek Bireyler (Kişi 3, Kişi 4, Kişi 5, ...) - Sınırsız ve Dinamik Eklenebilir
    var extraMemberIds by remember(refreshTrigger) {
        val saved = childPrefs.getString("custom_extra_member_ids", "3,4") ?: "3,4"
        val list = saved.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it >= 3 }
        mutableStateOf(if (list.isEmpty()) listOf(3, 4) else list)
    }

    // Giriş yapılmamışsa doğrudan serbest deneme modudur
    val isGuestMode = !settings.isLoggedIn

    // Giriş yapmadan deneyen kullanıcılar için arayüz durumları (RAM içi geçici)
    var simP1Prayers by remember { mutableStateOf(listOf("PRAYED", "PRAYED", "NONE", "NONE", "NONE")) }
    var simP2Prayers by remember { mutableStateOf(listOf("NONE", "PRAYED", "PRAYED", "NONE", "NONE")) }

    // Uygulama Modu: "FAMILY" veya "PARTNER"
    var appMode by remember {
        mutableStateOf(childPrefs.getString("app_mode", "FAMILY") ?: "FAMILY")
    }

    // Kaza Takip Bilgileri (Herkesin Detaysız Kaza Namaz Durumu için)
    val kazaPrayers by viewModel.kazaPrayers.collectAsState()
    val myTotalOwed = remember(kazaPrayers) { kazaPrayers.sumOf { it.owedCount } }
    val myTotalCompleted = remember(kazaPrayers) { kazaPrayers.sumOf { it.completedCount } }

    // Tarih Seçimi: Aile Tablosu dünün veya bugünün durumunu gösterebilir (0: Bugün, -1: Dün)
    var viewingFamilyDateOffset by remember { mutableStateOf(0) }
    val isViewingTodayInFamily = (viewingFamilyDateOffset == 0)

    // Dinamik Birey Yardımcı Fonksiyonları
    fun getMemberName(id: Int): String {
        return childPrefs.getString("p${id}_name", "Kişi $id") ?: "Kişi $id"
    }

    fun getMemberIcon(id: Int): String {
        return childPrefs.getString("p${id}_icon", if (id % 2 == 1) "👦" else "👧") ?: if (id % 2 == 1) "👦" else "👧"
    }

    fun getMemberPrayers(memberId: Int, isToday: Boolean): List<String> {
        val dateKey = if (isToday) todayDateStr else yesterdayDateStr
        return listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { pKey ->
            childPrefs.getString("p${memberId}_${pKey}_$dateKey", "NONE") ?: "NONE"
        }
    }

    fun setMemberPrayer(memberId: Int, prayerIdx: Int, newStatus: String, isToday: Boolean) {
        val dateKey = if (isToday) todayDateStr else yesterdayDateStr
        val pKey = when (prayerIdx) { 0 -> "fajr"; 1 -> "dhuhr"; 2 -> "asr"; 3 -> "maghrib"; else -> "isha" }
        childPrefs.edit().putString("p${memberId}_${pKey}_$dateKey", newStatus).apply()
        refreshTrigger++
    }

    // Kişi 2 Namaz Durumları (Partner eşleşmesinden veya yerel kayıttan)
    var p2LocalFajr by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_local_fajr_$todayDateStr", partnerInfo.partnerFajr) ?: partnerInfo.partnerFajr) }
    var p2LocalDhuhr by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_local_dhuhr_$todayDateStr", partnerInfo.partnerDhuhr) ?: partnerInfo.partnerDhuhr) }
    var p2LocalAsr by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_local_asr_$todayDateStr", partnerInfo.partnerAsr) ?: partnerInfo.partnerAsr) }
    var p2LocalMaghrib by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_local_maghrib_$todayDateStr", partnerInfo.partnerMaghrib) ?: partnerInfo.partnerMaghrib) }
    var p2LocalIsha by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p2_local_isha_$todayDateStr", partnerInfo.partnerIsha) ?: partnerInfo.partnerIsha) }
    val p2TodayPrayers = if (partnerInfo.isMatched) {
        listOf(partnerInfo.partnerFajr, partnerInfo.partnerDhuhr, partnerInfo.partnerAsr, partnerInfo.partnerMaghrib, partnerInfo.partnerIsha)
    } else {
        listOf(p2LocalFajr, p2LocalDhuhr, p2LocalAsr, p2LocalMaghrib, p2LocalIsha)
    }

    val p2YesterdayPrayers = listOf(
        partnerInfo.partnerYesterdayFajr,
        partnerInfo.partnerYesterdayDhuhr,
        partnerInfo.partnerYesterdayAsr,
        partnerInfo.partnerYesterdayMaghrib,
        partnerInfo.partnerYesterdayIsha
    )

    // Tab Seçimi (0: Aile Tablosu, -1: Kişi 1, 1: Kişi 2, 3..N: Ek Bireyler)
    var selectedFamilyTab by remember { mutableStateOf(0) }

    LaunchedEffect(hasPerson2, extraMemberIds, partnerInfo.isMatched) {
        if (selectedFamilyTab == 1 && !(hasPerson2 || partnerInfo.isMatched)) {
            selectedFamilyTab = 0
        } else if (selectedFamilyTab >= 3 && !extraMemberIds.contains(selectedFamilyTab)) {
            selectedFamilyTab = 0
        }
    }

    // Dialog Durumları
    var showEditPersonDialog by remember { mutableStateOf(false) }
    var editingPersonIndex by remember { mutableStateOf(2) } // 1, 2, 3, 4
    var editPersonNameInput by remember { mutableStateOf("") }
    var editPersonIconInput by remember { mutableStateOf("🧔") }
    var editPersonActiveInput by remember { mutableStateOf(true) }

    var showEmailInviteDialog by remember { mutableStateOf(false) }
    var inviteEmailInput by remember { mutableStateOf("") }
    var inviteRoleInput by remember { mutableStateOf("Kişi 2") }

    var showJoinPoolDialog by remember { mutableStateOf(false) }
    var joinPoolCodeInput by remember { mutableStateOf("") }
    var joinPoolRoleInput by remember { mutableStateOf("Kişi 2") }

    var showResetHandshakeDialog by remember { mutableStateOf(false) }
    var showArchitectureInfoDialog by remember { mutableStateOf(false) }

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
        }
    }

    // Vaktin gelip gelmediğini kontrol eden yardımcı fonksiyon
    fun isPrayerLockedForToday(prayerIdx: Int): Boolean {
        if (!isViewingTodayInFamily) return false // Geçmiş günler kilitli değil
        val prayerTypeStr = when (prayerIdx) {
            0 -> "FAJR"
            1 -> "DHUHR"
            2 -> "ASR"
            3 -> "MAGHRIB"
            4 -> "ISHA"
            else -> "FAJR"
        }
        return !PrayerCalculator.isPrayerTimeArrived(prayerTypeStr, prayerTimes, true)
    }

    fun getPrayerTimeEntryStr(prayerIdx: Int): String {
        return when (prayerIdx) {
            0 -> "İmsak: ${prayerTimes.imsak}"
            1 -> "Öğle: ${prayerTimes.ogle}"
            2 -> "İkindi: ${prayerTimes.ikindi}"
            3 -> "Akşam: ${prayerTimes.aksam}"
            4 -> "Yatsı: ${prayerTimes.yatsi}"
            else -> ""
        }
    }

    fun getPrayerNameStr(prayerIdx: Int): String {
        return when (prayerIdx) {
            0 -> "Sabah"
            1 -> "Öğle"
            2 -> "İkindi"
            3 -> "Akşam"
            4 -> "Yatsı"
            else -> ""
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Durum Bildirim Çubuğu
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
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
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

        // =================================================================
        // 1. KULLANICI PROFİL / GİRİŞ KARTI
        // =================================================================
        item {
            if (settings.isLoggedIn) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = person1Icon,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.userName.ifBlank { "Kişi 1 (Siz)" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        editingPersonIndex = 1
                                        editPersonNameInput = settings.userName
                                        editPersonIconInput = person1Icon
                                        editPersonActiveInput = true
                                        showEditPersonDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "İsmi Düzenle", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                }
                            }

                            if (settings.userEmail.isNotBlank()) {
                                Text(
                                    text = settings.userEmail,
                                    fontSize = 12.sp,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Google Aktif ✓ • Havuz Lideri",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.logoutUser() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Çıkış Yap", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                // Giriş Yapılmamışsa: Belirgin Google Giriş Butonu
                Card(
                    onClick = {
                        try {
                            val chooseAccountIntent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                            googleAccountPickerLauncher.launch(chooseAccountIntent)
                        } catch (_: Exception) {
                            viewModel.loginWithGoogleBrowser()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("google_login_button")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("G", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google ile Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Aile havuzunu görmek ve verileri eşitlemek için gereklidir", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // =================================================================
        // 2. GİRİŞ YAPILMAMIŞSA: CANLI ÖNİZLEME & İNCELEME BİLGİ KARTI
        // =================================================================
        if (!settings.isLoggedIn) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("guest_preview_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "İnceleme Modu: Kişi 1 & Kişi 2 👁️",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldPrimary
                                ) {
                                    Text(
                                        text = "CANLI ÖNİZLEME",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Giriş yapmadan arkadaş havuzunun (Kişi 1 & Kişi 2 canlı namaz matrisi, buton tepkileri ve sekmeler) nasıl çalıştığını aşağıdan deneyebilirsiniz. Başka cihazla anlık eşitlenmek için dilediğiniz an Google ile giriş yapabilirsiniz.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // AİLE DURUM PANELİ (Canlı Bağlantı, Eşleşen Kişiler, Aktif Kod, Detaysız Kaza)
        // -----------------------------------------------------------------
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("family_status_panel")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 1. ÜST BAŞLIK VE CANLI ROZETİ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Diversity3, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Aile Havuzu • İbadet Halkası", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (partnerInfo.isMatched) EmeraldPrimary else if (isGuestMode) Color(0xFF3B82F6) else Color(0xFFD97706)
                                    ) {
                                        Text(
                                            text = if (partnerInfo.isMatched) "CANLI 🟢" else if (isGuestMode) "DENEME MODU 📱" else "BEKLEMEDE ⏳",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Ailece 5 Vakit Namaz & Kaza İbadet Halkası",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        IconButton(onClick = { showArchitectureInfoDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = "Veritabanı Bilgisi", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. ÇOK KİŞİLİK YOLDAŞ HAVUZU BİLGİSİ
                    Surface(
                        color = SoftInfoCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SoftInfoCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sınırsız Kişi & Çoklu Arkadaş Havuzu",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Aileniz, dostlarınız ve sevdikleriniz tek bir arkadaşlık kodu ile aynı havuza bağlanabilir.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. MEVCUT AİLE BAĞLANTI DURUMU (GERÇEK ZAMANLI TEYİT KUTUSU)
                    Surface(
                        color = if (partnerInfo.isMatched) EmeraldContainer.copy(alpha = 0.6f) else if (isGuestMode) Color(0xFFEFF6FF) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (partnerInfo.isMatched) SoftInfoCardBorder else if (isGuestMode) Color(0xFFBFDBFE) else Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (partnerInfo.isMatched) Icons.Filled.CheckCircle else if (isGuestMode) Icons.Filled.PhoneAndroid else Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = if (partnerInfo.isMatched) EmeraldPrimary else if (isGuestMode) Color(0xFF2563EB) else Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (partnerInfo.isMatched) "Çift Taraflı Bağlantı Aktif 🟢" 
                                               else if (isGuestMode) "Serbest Deneme Modu 📱" 
                                               else "Bağlantı Bekleniyor ⏳",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (partnerInfo.isMatched) EmeraldPrimary else if (isGuestMode) Color(0xFF1D4ED8) else Color(0xFF92400E)
                                    )
                                }

                                // CANLI TEYİT BUTONU
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                                } else {
                                    Surface(
                                        onClick = {
                                            if (isGuestMode && !partnerInfo.isMatched) {
                                                viewModel.showStatusMessage("Deneme Modu: Tüm ekranları ve namaz butonlarını özgürce deneyebilirsiniz 🟢")
                                            } else {
                                                viewModel.refreshPartnerData()
                                                viewModel.showStatusMessage("Havuz bağlantı durumu sorgulanıyor... Canlı teyit sağlandı! 🟢")
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.8f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Bağlantıyı Teyit Et 🔄", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (partnerInfo.isMatched) {
                                    "$person2Name ile çift taraflı bağlantı sağlandı. Kılınan namazlar ve kazalar anlık senkronize ediliyor."
                                } else if (isGuestMode) {
                                    "Giriş yapmadan tüm ekranları, sekmeleri ve butonları özgürce deneyebilirsiniz. Arkadaşınızla gerçek zamanlı bağlantı ve kalıcı kayıt için yukarıdan Google ile giriş yapabilirsiniz."
                                } else {
                                    "2. Kişi henüz havuz kodunu onaylamadı. Bağlantının sağlanması için aşağıdaki davet kodunuzu arkadaşınızla paylaşın."
                                },
                                fontSize = 10.sp,
                                color = if (partnerInfo.isMatched || isGuestMode) TextSecondary else Color(0xFF78350F),
                                lineHeight = 14.sp
                            )

                            if (partnerInfo.isMatched) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bağlantıyı Yeniden Kur / Sıfırla (Temizle)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MissedRed,
                                    modifier = Modifier.clickable { showResetHandshakeDialog = true }
                                )
                            }
                        }
                    }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. AKTİF DAVET KODU BÖLÜMÜ (1. Kişinin Kodu Merkezli)
                        Surface(
                            color = WarmCreamBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Aktif Aile Davet Kodu", fontSize = 11.sp, color = TextSecondary)
                                        Text(text = familyPoolCode, fontSize = 18.sp, fontWeight = FontWeight.Black, color = EmeraldPrimary)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Aile Davet Kodu", familyPoolCode)
                                                clipboard.setPrimaryClip(clip)
                                                viewModel.showStatusMessage("Aile Davet Kodu kopyalandı: $familyPoolCode 📋")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Kodu Kopyala", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Selamün Aleyküm! Ailece Secde uygulamamızda ortak aile havuzumuz açıldı. Aile Havuz Kodumuz: $familyPoolCode . Uygulamadan 'Kodu Onayla & Havuza Katıl' butonuna bu kodu girerek ibadet halkamıza dahil olabilirsiniz! 🤲"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Aile Havuz Kodunu Paylaş"))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = "Kodu Paylaş", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showEmailInviteDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Email, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mail ile Davet", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showJoinPoolDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kodu Onayla & Katıl", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. EŞLEŞEN KİŞİLER & CİHAZ DURUMLARI LİSTESİ
                        Text(
                            text = "Havuzdaki Bireyler ve Durumları:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Kişi 1 (Siz)
                            Surface(
                                color = SoftInfoCardBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SoftInfoCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = person1Icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = settings.userName.ifBlank { "Kişi 1" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "(Siz • Havuz Lideri)", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(text = "Google ile Bağlı ✓ • Çevrimiçi", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = {
                                            editingPersonIndex = 1
                                            editPersonNameInput = settings.userName
                                            editPersonIconInput = person1Icon
                                            editPersonActiveInput = true
                                            showEditPersonDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Kişi 2
                            if (partnerInfo.isMatched || hasPerson2) {
                                Surface(
                                    color = SoftInfoCardBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = person2Icon, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = person2Name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(
                                                text = if (partnerInfo.isMatched) "Çift Taraflı Bağlandı 🟢 • Aktif" else "Davet Bekleniyor ⏳ • Kod Gönderildi",
                                                fontSize = 10.sp,
                                                color = if (partnerInfo.isMatched) EmeraldPrimary else Color(0xFFD97706),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                editingPersonIndex = 2
                                                editPersonNameInput = person2Name
                                                editPersonIconInput = person2Icon
                                                editPersonActiveInput = hasPerson2
                                                showEditPersonDialog = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showJoinPoolDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Kişi 2 ile Kod Girerek Bağlan", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Ek Bireyler (Kişi 3, Kişi 4, Kişi 5, ... Dinamik Liste)
                            extraMemberIds.forEach { id ->
                                val mName = getMemberName(id)
                                val mIcon = getMemberIcon(id)
                                Surface(
                                    color = SoftInfoCardBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = mIcon, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = mName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = "Aile Havuzunda Aktif 🟢", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                        IconButton(
                                            onClick = {
                                                editingPersonIndex = id
                                                editPersonNameInput = mName
                                                editPersonIconInput = mIcon
                                                editPersonActiveInput = true
                                                showEditPersonDialog = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            // + Yeni Kişi Ekle Butonu
                            OutlinedButton(
                                onClick = {
                                    val nextId = if (extraMemberIds.isEmpty()) 3 else (extraMemberIds.maxOrNull() ?: 2) + 1
                                    editingPersonIndex = nextId
                                    editPersonNameInput = "Kişi $nextId"
                                    editPersonIconInput = if (nextId % 2 == 1) "👦" else "👧"
                                    editPersonActiveInput = true
                                    showEditPersonDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Yeni Kişi Ekle (Kişi ${if (extraMemberIds.isEmpty()) 3 else (extraMemberIds.maxOrNull() ?: 2) + 1})",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. HERKESİN DETAYSIZ KAZA NAMAZ BİLGİLERİ KARTI (User request)
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Detaysız Aile Kaza Takibi",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Mahremiyet Korumalı 👁️",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "İbadet mahremiyeti için vakit dökümü yapılmaz; yalnızca genel toplam kaza borcu ve eda edilen adetler özet gösterilir.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bireylerin Detaysız Kaza Satırları
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Kişi 1 (Siz)
                                    FamilyMemberKazaStatusPill(
                                        icon = person1Icon,
                                        name = settings.userName.ifBlank { "Kişi 1" }.substringBefore(" ") + " (Siz)",
                                        owed = myTotalOwed,
                                        completed = myTotalCompleted
                                    )

                                    // Kişi 2 - Eşleşmiş veya aktif ise gösterilir
                                    if (hasPerson2 || partnerInfo.isMatched) {
                                        FamilyMemberKazaStatusPill(
                                            icon = person2Icon,
                                            name = person2Name,
                                            owed = if (partnerInfo.isMatched) partnerInfo.partnerKazaOwed else childPrefs.getInt("p2_kaza_owed", 0),
                                            completed = if (partnerInfo.isMatched) partnerInfo.partnerKazaCompleted else childPrefs.getInt("p2_kaza_completed", 0),
                                            onAdd = if (!partnerInfo.isMatched) {
                                                {
                                                    val c = childPrefs.getInt("p2_kaza_completed", 0) + 1
                                                    childPrefs.edit().putInt("p2_kaza_completed", c).apply()
                                                    refreshTrigger++
                                                    viewModel.showStatusMessage("$person2Name için +1 kaza eda edildi! 🤲")
                                                }
                                            } else null
                                        )
                                    }

                                    // Ek Bireyler (Kişi 3, Kişi 4, Kişi 5... Dinamik Kaza Takibi)
                                    extraMemberIds.forEach { id ->
                                        val mName = getMemberName(id)
                                        val mIcon = getMemberIcon(id)
                                        val mOwed = childPrefs.getInt("p${id}_kaza_owed", 0)
                                        val mCompleted = childPrefs.getInt("p${id}_kaza_completed", 0)
                                        FamilyMemberKazaStatusPill(
                                            icon = mIcon,
                                            name = mName,
                                            owed = mOwed,
                                            completed = mCompleted,
                                            onAdd = {
                                                val c = mCompleted + 1
                                                childPrefs.edit().putInt("p${id}_kaza_completed", c).apply()
                                                refreshTrigger++
                                                viewModel.showStatusMessage("$mName için +1 kaza eda edildi! 🤲")
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                val extraCompletedSum = extraMemberIds.sumOf { childPrefs.getInt("p${it}_kaza_completed", 0) }
                                val p2Completed = if (hasPerson2 || partnerInfo.isMatched) {
                                    if (partnerInfo.isMatched) partnerInfo.partnerKazaCompleted else childPrefs.getInt("p2_kaza_completed", 0)
                                } else 0
                                val totalFamilyCompleted = myTotalCompleted + p2Completed + extraCompletedSum
                                Text(
                                    text = "🌟 Ailece Toplam Kılınan Kaza: $totalFamilyCompleted vakit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFEF3C7)
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // YATAY KAYDIRILABİLİR KİŞİ SEKMELERİ (TABS)
            // -----------------------------------------------------------------
            item {
                val tabsScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabsScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tab 0: Aile Tablosu (Genel Bakış)
                    Surface(
                        onClick = { selectedFamilyTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedFamilyTab == 0) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, if (selectedFamilyTab == 0) EmeraldPrimary else SoftInfoCardBorder)
                    ) {
                        Text(
                            text = "👨‍👩‍👧‍👦 Aile İbadet Halkası",
                            fontSize = 12.sp,
                            fontWeight = if (selectedFamilyTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedFamilyTab == 0) Color.White else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Tab -1: Kişi 1 (Kullanıcı / Kendiniz İnceleme Sekmesi)
                    Surface(
                        onClick = { selectedFamilyTab = -1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedFamilyTab == -1) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, if (selectedFamilyTab == -1) EmeraldPrimary else SoftInfoCardBorder)
                    ) {
                        Text(
                            text = "$person1Icon ${settings.userName.ifBlank { "Kişi 1" }.substringBefore(" ")}",
                            fontSize = 12.sp,
                            fontWeight = if (selectedFamilyTab == -1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedFamilyTab == -1) Color.White else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Tab 1: Kişi 2 (Eşleşmiş, aktif veya deneme modunda)
                    if (hasPerson2 || isGuestMode) {
                        Surface(
                            onClick = { selectedFamilyTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedFamilyTab == 1) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, if (selectedFamilyTab == 1) EmeraldPrimary else SoftInfoCardBorder)
                        ) {
                            Text(
                                text = "$person2Icon $person2Name",
                                fontSize = 12.sp,
                                fontWeight = if (selectedFamilyTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFamilyTab == 1) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Ek Bireyler Sekmeleri (Kişi 3, Kişi 4, Kişi 5...)
                    extraMemberIds.forEach { id ->
                        val mName = getMemberName(id)
                        val mIcon = getMemberIcon(id)
                        Surface(
                            onClick = { selectedFamilyTab = id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedFamilyTab == id) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, if (selectedFamilyTab == id) EmeraldPrimary else SoftInfoCardBorder)
                        ) {
                            Text(
                                text = "$mIcon $mName",
                                fontSize = 12.sp,
                                fontWeight = if (selectedFamilyTab == id) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFamilyTab == id) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Sekme: + Yeni Kişi Ekle
                    Surface(
                        onClick = {
                            val nextId = if (extraMemberIds.isEmpty()) 3 else (extraMemberIds.maxOrNull() ?: 2) + 1
                            editingPersonIndex = nextId
                            editPersonNameInput = "Kişi $nextId"
                            editPersonIconInput = if (nextId % 2 == 1) "👦" else "👧"
                            editPersonActiveInput = true
                            showEditPersonDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, SoftInfoCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Kişi Ekle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // =================================================================
            // GÖRÜNÜM 0: AİLE TABLOSU (ORTAK MATRİS - DÜN & BUGÜN DESTEKLİ)
            // =================================================================
            if (selectedFamilyTab == 0) {
                item {
                    val p1Statuses = if (isGuestMode) {
                        simP1Prayers
                    } else if (isViewingTodayInFamily) {
                        listOf(todayPrayer.fajrStatus, todayPrayer.dhuhrStatus, todayPrayer.asrStatus, todayPrayer.maghribStatus, todayPrayer.ishaStatus)
                    } else {
                        // Dünkü durum (varsayılan dolu veya Entity'den)
                        listOf("PRAYED", "PRAYED", "PRAYED", "PRAYED", "PRAYED")
                    }

                    val p2Statuses = if (isGuestMode) {
                        simP2Prayers
                    } else if (isViewingTodayInFamily) {
                        p2TodayPrayers
                    } else {
                        p2YesterdayPrayers
                    }

                    val extraPrayedCount = extraMemberIds.sumOf { id ->
                        getMemberPrayers(id, isViewingTodayInFamily).count { it == "PRAYED" }
                    }

                    val activeMembersCount = 1 + (if (hasPerson2 || isGuestMode) 1 else 0) + extraMemberIds.size
                    val totalCapacity = activeMembersCount * 5

                    val totalPrayed = p1Statuses.count { it == "PRAYED" } +
                            (if (hasPerson2 || isGuestMode) p2Statuses.count { it == "PRAYED" } else 0) +
                            extraPrayedCount

                    val familyPercent = if (totalCapacity > 0) ((totalPrayed / totalCapacity.toFloat()) * 100).toInt() else 0

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Başlık & Gün Seçici (Dün / Bugün)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "👨‍👩‍👧", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (settings.userName.isNotBlank()) "${settings.userName.substringBefore(" ")} Ailesi" else "Aile İbadet Halkası",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isViewingTodayInFamily) "📅 Bugünün Tablosu" else "📅 DÜNKÜ AİLE TABLOSU",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isViewingTodayInFamily) Color.White.copy(alpha = 0.85f) else Color(0xFFFEF3C7)
                                        )
                                    }
                                }

                                // DÜN / BUGÜN GEÇİŞ BUTONU (User request 6)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        viewingFamilyDateOffset = if (viewingFamilyDateOffset == 0) -1 else 0
                                    }
                                ) {
                                    Text(
                                        text = if (isViewingTodayInFamily) "◀ Dünü Gör" else "Bugüne Dön ▶",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Aile Başarı Rozeti
                            Surface(
                                color = if (totalPrayed >= (totalCapacity * 0.8f).toInt()) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (totalPrayed >= (totalCapacity * 0.8f).toInt()) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌟 ${if (isViewingTodayInFamily) "Bugün Aile Başarısı:" else "Dünkü Aile Skoru:"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalPrayed >= (totalCapacity * 0.8f).toInt()) Color(0xFF78350F) else Color.White
                                    )
                                    Text(
                                        text = "$totalPrayed / $totalCapacity Vakit (%$familyPercent)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (totalPrayed >= (totalCapacity * 0.8f).toInt()) Color(0xFF78350F) else Color(0xFF86EFAC)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ORTAK MATRİS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "👉 Sağa Kaydırılabilir ($activeMembersCount Kişi)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // YATAY KAYDIRILABİLİR MATRİS TABLOSU
                            val matrixScroll = rememberScrollState()
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // SABİT SOL SÜTUN: VAKİTLER
                                Column(modifier = Modifier.width(62.dp)) {
                                    Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.CenterStart) {
                                        Text(text = "VAKİT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    val times = listOf(
                                        Pair("Sabah", "🌅"),
                                        Pair("Öğle", "☀️"),
                                        Pair("İkindi", "⛅"),
                                        Pair("Akşam", "🌇"),
                                        Pair("Yatsı", "🌌")
                                    )
                                    times.forEach { (name, icon) ->
                                        Box(modifier = Modifier.height(38.dp), contentAlignment = Alignment.CenterStart) {
                                            Text(
                                                text = "$icon $name",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // SAĞ SÜTUNLAR (Kişi 1, Kişi 2, ve Tüm Ek Kişiler Dinamik Olarak Gelir)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(matrixScroll),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // SÜTUN 1: Kişi 1 (Siz)
                                    Column(
                                        modifier = Modifier.width(82.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$person1Icon ${settings.userName.ifBlank { "Kişi 1" }.substringBefore(" ")}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                        p1Statuses.forEachIndexed { idx, status ->
                                            Box(
                                                modifier = Modifier
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (isGuestMode) {
                                                            val next = when (status) {
                                                                "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                            }
                                                            simP1Prayers = simP1Prayers.toMutableList().also { it[idx] = next }
                                                            viewModel.showStatusMessage("Kişi 1: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                                                        } else if (isViewingTodayInFamily) {
                                                            if (isPrayerLockedForToday(idx)) {
                                                                viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                                                            } else {
                                                                val prayerType = when (idx) {
                                                                    0 -> "FAJR"; 1 -> "DHUHR"; 2 -> "ASR"; 3 -> "MAGHRIB"; else -> "ISHA"
                                                                }
                                                                val next = when (status) {
                                                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                                }
                                                                viewModel.setPrayerStatus(prayerType, next)
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                FamilyMatrixStatusCell(status)
                                            }
                                        }
                                    }

                                    // SÜTUN 2: Kişi 2
                                    if (hasPerson2 || isGuestMode) {
                                        Column(
                                            modifier = Modifier.width(82.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$person2Icon $person2Name",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                            p2Statuses.forEachIndexed { idx, status ->
                                                Box(
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (isGuestMode) {
                                                            val next = when (status) {
                                                                "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                            }
                                                            simP2Prayers = simP2Prayers.toMutableList().also { it[idx] = next }
                                                            viewModel.showStatusMessage("Kişi 2: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                                                        } else if (isViewingTodayInFamily) {
                                                            if (isPrayerLockedForToday(idx)) {
                                                                viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                                                            } else {
                                                                val next = when (status) {
                                                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                                }
                                                                when (idx) {
                                                                    0 -> { p2LocalFajr = next; childPrefs.edit().putString("p2_local_fajr_$todayDateStr", next).apply() }
                                                                    1 -> { p2LocalDhuhr = next; childPrefs.edit().putString("p2_local_dhuhr_$todayDateStr", next).apply() }
                                                                    2 -> { p2LocalAsr = next; childPrefs.edit().putString("p2_local_asr_$todayDateStr", next).apply() }
                                                                    3 -> { p2LocalMaghrib = next; childPrefs.edit().putString("p2_local_maghrib_$todayDateStr", next).apply() }
                                                                    4 -> { p2LocalIsha = next; childPrefs.edit().putString("p2_local_isha_$todayDateStr", next).apply() }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    FamilyMatrixStatusCell(status)
                                                }
                                            }
                                        }
                                    }

                                    // DİNAMİK SÜTUNLAR: Kişi 3, Kişi 4, Kişi 5, Kişi 6... Sınırsız Birey
                                    extraMemberIds.forEach { extraId ->
                                        val mName = getMemberName(extraId)
                                        val mIcon = getMemberIcon(extraId)
                                        val mStatuses = getMemberPrayers(extraId, isViewingTodayInFamily)

                                        Column(
                                            modifier = Modifier.width(82.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$mIcon $mName",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                            mStatuses.forEachIndexed { idx, status ->
                                                Box(
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            if (isViewingTodayInFamily) {
                                                                if (isPrayerLockedForToday(idx)) {
                                                                    viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                                                                } else {
                                                                    val next = when (status) {
                                                                        "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                                    }
                                                                    setMemberPrayer(extraId, idx, next, isViewingTodayInFamily)
                                                                    viewModel.showStatusMessage("$mName: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                                                                }
                                                            } else {
                                                                val next = when (status) {
                                                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                                }
                                                                setMemberPrayer(extraId, idx, next, isViewingTodayInFamily)
                                                                viewModel.showStatusMessage("$mName: Dünkü ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    FamilyMatrixStatusCell(status)
                                                }
                                            }
                                        }
                                    }

                                    // SON SÜTUN: ➕ Yeni Kişi Ekle (Sınırsız)
                                    Column(
                                        modifier = Modifier.width(76.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                                            Text(text = "+ Kişi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .height(190.dp)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.14f))
                                                .clickable {
                                                    val nextId = if (extraMemberIds.isEmpty()) 3 else (extraMemberIds.maxOrNull() ?: 2) + 1
                                                    editingPersonIndex = nextId
                                                    editPersonNameInput = "Kişi $nextId"
                                                    editPersonIconInput = if (nextId % 2 == 1) "👦" else "👧"
                                                    editPersonActiveInput = true
                                                    showEditPersonDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Filled.PersonAdd, contentDescription = "Kişi Ekle", tint = Color.White, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "+ Yeni\nKişi Ekle",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manevi Dua Butonu (Vakti Hatırlat butonu kaldırıldı, sadeleştirildi)
                            Button(
                                onClick = {
                                    viewModel.showStatusMessage("Tüm ailenize manevi dua gönderildi: Allah ibadetlerimizi kabul etsin! 🤲")
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aileye Dua Gönder 🤲", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // GÖRÜNÜM -1: KİŞİ 1 (KENDİNİZ / KULLANICI DETAYI & İNCELEME)
            // =================================================================
            if (selectedFamilyTab == -1) {
                item {
                    val p1CurrentPrayers = if (isGuestMode) {
                        simP1Prayers
                    } else {
                        listOf(todayPrayer.fajrStatus, todayPrayer.dhuhrStatus, todayPrayer.asrStatus, todayPrayer.maghribStatus, todayPrayer.ishaStatus)
                    }

                    IndividualPersonCard(
                        personName = "${settings.userName.ifBlank { "Kişi 1" }} (Siz)",
                        personIcon = person1Icon,
                        isToday = isViewingTodayInFamily,
                        prayers = p1CurrentPrayers,
                        yesterdayPrayers = listOf("PRAYED", "PRAYED", "PRAYED", "PRAYED", "PRAYED"),
                        onEdit = {
                            editingPersonIndex = 1
                            editPersonNameInput = settings.userName
                            editPersonIconInput = person1Icon
                            editPersonActiveInput = true
                            showEditPersonDialog = true
                        },
                        onToggle = { idx ->
                            if (isGuestMode) {
                                val current = simP1Prayers.getOrElse(idx) { "NONE" }
                                val next = when (current) {
                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                }
                                simP1Prayers = simP1Prayers.toMutableList().also { it[idx] = next }
                                viewModel.showStatusMessage("Kişi 1: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                            } else if (isViewingTodayInFamily) {
                                if (isPrayerLockedForToday(idx)) {
                                    viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                                } else {
                                    val prayerType = when (idx) {
                                        0 -> "FAJR"; 1 -> "DHUHR"; 2 -> "ASR"; 3 -> "MAGHRIB"; else -> "ISHA"
                                    }
                                    val current = p1CurrentPrayers.getOrElse(idx) { "NONE" }
                                    val next = when (current) {
                                        "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                    }
                                    viewModel.setPrayerStatus(prayerType, next)
                                }
                            }
                        },
                        onSendDua = { viewModel.showStatusMessage("Kişi 1 için tebrik ve dua kaydedildi! 🤲") }
                    )
                }
            }

            // =================================================================
            // GÖRÜNÜM 1: KİŞİ 2 DETAYI
            // =================================================================
            if (selectedFamilyTab == 1 && (hasPerson2 || isGuestMode)) {
                item {
                    val p2CurrentPrayers = if (isGuestMode) simP2Prayers else p2TodayPrayers

                    IndividualPersonCard(
                        personName = person2Name,
                        personIcon = person2Icon,
                        isToday = isViewingTodayInFamily,
                        prayers = p2CurrentPrayers,
                        yesterdayPrayers = p2YesterdayPrayers,
                        onEdit = {
                            editingPersonIndex = 2
                            editPersonNameInput = person2Name
                            editPersonIconInput = person2Icon
                            editPersonActiveInput = hasPerson2
                            showEditPersonDialog = true
                        },
                        onToggle = { idx ->
                            if (isGuestMode) {
                                val current = simP2Prayers.getOrElse(idx) { "NONE" }
                                val next = when (current) {
                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                }
                                simP2Prayers = simP2Prayers.toMutableList().also { it[idx] = next }
                                viewModel.showStatusMessage("Kişi 2: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                            } else if (isPrayerLockedForToday(idx)) {
                                viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                            } else {
                                val current = p2TodayPrayers.getOrElse(idx) { "NONE" }
                                val next = when (current) {
                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                }
                                when (idx) {
                                    0 -> { p2LocalFajr = next; childPrefs.edit().putString("p2_local_fajr_$todayDateStr", next).apply() }
                                    1 -> { p2LocalDhuhr = next; childPrefs.edit().putString("p2_local_dhuhr_$todayDateStr", next).apply() }
                                    2 -> { p2LocalAsr = next; childPrefs.edit().putString("p2_local_asr_$todayDateStr", next).apply() }
                                    3 -> { p2LocalMaghrib = next; childPrefs.edit().putString("p2_local_maghrib_$todayDateStr", next).apply() }
                                    4 -> { p2LocalIsha = next; childPrefs.edit().putString("p2_local_isha_$todayDateStr", next).apply() }
                                }
                            }
                        },
                        onSendDua = { viewModel.showStatusMessage("$person2Name için dua ve tebrik gönderildi! 👏") }
                    )
                }
            }

            // =================================================================
            // DİNAMİK BİREY DETAY GÖRÜNÜMÜ: KİŞİ 3, 4, 5, 6... (Sınırsız)
            // =================================================================
            if (extraMemberIds.contains(selectedFamilyTab)) {
                val currentMemberId = selectedFamilyTab
                val mName = getMemberName(currentMemberId)
                val mIcon = getMemberIcon(currentMemberId)
                val todayP = getMemberPrayers(currentMemberId, true)
                val yesterdayP = getMemberPrayers(currentMemberId, false)
                val currentP = if (isViewingTodayInFamily) todayP else yesterdayP

                item {
                    IndividualPersonCard(
                        personName = mName,
                        personIcon = mIcon,
                        isToday = isViewingTodayInFamily,
                        prayers = currentP,
                        yesterdayPrayers = yesterdayP,
                        onEdit = {
                            editingPersonIndex = currentMemberId
                            editPersonNameInput = mName
                            editPersonIconInput = mIcon
                            editPersonActiveInput = true
                            showEditPersonDialog = true
                        },
                        onToggle = { idx ->
                            if (isViewingTodayInFamily && isPrayerLockedForToday(idx)) {
                                viewModel.showStatusMessage("Bu vakit henüz girmedi (${getPrayerTimeEntryStr(idx)}) ⏳")
                            } else {
                                val current = currentP.getOrElse(idx) { "NONE" }
                                val next = when (current) {
                                    "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                }
                                setMemberPrayer(currentMemberId, idx, next, isViewingTodayInFamily)
                                viewModel.showStatusMessage("$mName: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                            }
                        },
                        onSendDua = { viewModel.showStatusMessage("$mName için dua ve tebrik kaydedildi! 🤲") }
                    )
                }
            }
        }

    // =================================================================
    // DİALOGLAR (KİŞİ YÖNETİMİ, CİNSİYET İKONLARI, HAVUZ ONAY)
    // =================================================================

    // KİŞİ DÜZENLE / EKLE DİALOGU (Kadın, Erkek, Çocuk İkonları & Serbest İsim)
    if (showEditPersonDialog) {
        val womenIcons = listOf("🧕", "👩", "👵", "👧", "👱‍♀️")
        val menIcons = listOf("🧔", "👨", "🧓", "👦", "👱‍♂️")
        val childIcons = listOf("👦", "👧", "👶", "🧒", "🐣")
        val generalIcons = listOf("👤", "🧑", "🧓")

        AlertDialog(
            onDismissRequest = { showEditPersonDialog = false },
            title = {
                Text(
                    text = "Kişi $editingPersonIndex Bilgilerini Düzenle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    // Kadın, Anne & Eş İkonları
                    Text("Kadın / Anne / Eş İkonları:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        womenIcons.forEach { icon ->
                            val isSel = (editPersonIconInput == icon)
                            Surface(
                                onClick = { editPersonIconInput = icon },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else SoftInfoCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(text = icon, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Erkek, Baba & Eş İkonları
                    Text("Erkek / Baba / Eş İkonları:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        menIcons.forEach { icon ->
                            val isSel = (editPersonIconInput == icon)
                            Surface(
                                onClick = { editPersonIconInput = icon },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else SoftInfoCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(text = icon, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Çocuk & Bebek İkonları
                    Text("Çocuk & Bebek İkonları:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        childIcons.forEach { icon ->
                            val isSel = (editPersonIconInput == icon)
                            Surface(
                                onClick = { editPersonIconInput = icon },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else SoftInfoCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(text = icon, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Profil / Diğer
                    Text("Profil & Diğer:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        generalIcons.forEach { icon ->
                            val isSel = (editPersonIconInput == icon)
                            Surface(
                                onClick = { editPersonIconInput = icon },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else SoftInfoCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(text = icon, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hızlı Aile Şablonları (Eş, Çocuk, Bebek, Anne, Baba, Kişi)
                    Text("Hızlı Aile Rolü Şablonu:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Eş", "Çocuk", "Bebek", "Anne", "Baba").forEach { role ->
                            val isSel = (editPersonNameInput == role)
                            Surface(
                                onClick = {
                                    editPersonNameInput = role
                                    when (role) {
                                        "Eş" -> editPersonIconInput = if (editingPersonIndex % 2 == 0) "🧕" else "🧔"
                                        "Çocuk" -> editPersonIconInput = if (editingPersonIndex % 2 == 0) "👧" else "👦"
                                        "Bebek" -> editPersonIconInput = "👶"
                                        "Anne" -> editPersonIconInput = "🧕"
                                        "Baba" -> editPersonIconInput = "🧔"
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { editPersonNameInput = "Kişi $editingPersonIndex" },
                            shape = RoundedCornerShape(6.dp),
                            color = if (editPersonNameInput == "Kişi $editingPersonIndex") EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Kişi $editingPersonIndex",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (editPersonNameInput == "Kişi $editingPersonIndex") Color.White else TextPrimary,
                                modifier = Modifier.padding(vertical = 5.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            onClick = { editPersonNameInput = "" },
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Temizle (Kendin Yaz)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = 5.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editPersonNameInput,
                        onValueChange = { editPersonNameInput = it },
                        label = { Text("İsim / Takma Ad (Kişi $editingPersonIndex)") },
                        placeholder = { Text("İsmi buraya yazınız...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (editingPersonIndex >= 2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = editPersonActiveInput,
                                onCheckedChange = { editPersonActiveInput = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bu Kişi Aile Tablosunda Aktif Olsun", fontSize = 12.sp, color = TextPrimary)
                        }

                        if (editingPersonIndex >= 3 && editPersonActiveInput) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { editPersonActiveInput = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MissedRed),
                                border = BorderStroke(1.dp, MissedRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PersonRemove, contentDescription = null, tint = MissedRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bu Kişiyi Havuzdan Çıkar", fontSize = 11.sp, color = MissedRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editPersonNameInput.trim().ifBlank { "Kişi $editingPersonIndex" }
                        when (editingPersonIndex) {
                            1 -> {
                                person1Icon = editPersonIconInput
                                childPrefs.edit().putString("p1_icon", editPersonIconInput).apply()
                                if (name.isNotBlank()) viewModel.updateMyProfileName(name)
                            }
                            2 -> {
                                person2Name = name
                                person2Icon = editPersonIconInput
                                hasPerson2 = editPersonActiveInput
                                if (!editPersonActiveInput && selectedFamilyTab == 1) selectedFamilyTab = 0
                                childPrefs.edit()
                                    .putString("p2_name", name)
                                    .putString("p2_icon", editPersonIconInput)
                                    .putBoolean("has_p2", editPersonActiveInput)
                                    .putBoolean("has_p2_customized", true)
                                    .apply()
                            }
                            else -> {
                                val targetId = editingPersonIndex
                                if (editPersonActiveInput) {
                                    if (!extraMemberIds.contains(targetId)) {
                                        val updated = (extraMemberIds + targetId).sorted()
                                        extraMemberIds = updated
                                        childPrefs.edit().putString("custom_extra_member_ids", updated.joinToString(",")).apply()
                                    }
                                    childPrefs.edit()
                                        .putString("p${targetId}_name", name)
                                        .putString("p${targetId}_icon", editPersonIconInput)
                                        .putBoolean("has_p${targetId}", true)
                                        .apply()
                                } else {
                                    val updated = extraMemberIds.filter { it != targetId }
                                    extraMemberIds = updated
                                    childPrefs.edit()
                                        .putString("custom_extra_member_ids", updated.joinToString(","))
                                        .putBoolean("has_p${targetId}", false)
                                        .apply()
                                    if (selectedFamilyTab == targetId) selectedFamilyTab = 0
                                }
                            }
                        }
                        refreshTrigger++
                        showEditPersonDialog = false
                        viewModel.showStatusMessage(
                            if (editPersonActiveInput) "$name bilgileri başarıyla kaydedildi! 🌟"
                            else "$name havuzdan çıkarıldı."
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPersonDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // AİLE HAVUZUNA KATIL VE ONAYLA DİALOGU
    if (showJoinPoolDialog) {
        AlertDialog(
            onDismissRequest = { showJoinPoolDialog = false },
            title = { Text("Aile Havuzuna Katıl ve Onayla", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("1. Kişiden (Aile liderinden) aldığınız Aile Kodunu girerek ortak havuza dahil olun:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinPoolCodeInput,
                        onValueChange = { joinPoolCodeInput = it },
                        label = { Text("Aile Kodu (örn: EMB-5385)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu ailedeki rolünüz:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Kişi 2", "Kişi 3", "Kişi 4").forEach { r ->
                            val isSel = (joinPoolRoleInput == r)
                            Surface(
                                onClick = { joinPoolRoleInput = r },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = joinPoolCodeInput.trim().uppercase()
                        if (code.isNotBlank()) {
                            familyPoolCode = code
                            childPrefs.edit().putString("family_pool_code", code).apply()
                            viewModel.matchPartnerWithCode(code)
                            viewModel.showStatusMessage("$code kodlu Aile Ortak Havuzuna katılımınız ONAYLANDI! 🟢")
                        }
                        showJoinPoolDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Onayla & Havuza Katıl")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinPoolDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // E-POSTA İLE DAVET DİALOGU
    if (showEmailInviteDialog) {
        AlertDialog(
            onDismissRequest = { showEmailInviteDialog = false },
            title = { Text("E-Posta ile Aileye Davet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Aile havuzuna davet edilecek kişinin e-posta adresi:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = { inviteEmailInput = it },
                        label = { Text("E-Posta Adresi") },
                        placeholder = { Text("ornek@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Davet Rolü:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Kişi 2", "Kişi 3", "Kişi 4").forEach { r ->
                            val isSel = (inviteRoleInput == r)
                            Surface(
                                onClick = { inviteRoleInput = r },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = inviteEmailInput.trim()
                        if (email.isNotBlank()) {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                                putExtra(Intent.EXTRA_SUBJECT, "Ailece Secde - Aile İbadet Havuzu Daveti ve Onay Kodu")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Selamün Aleyküm!\n\nAilece Secde uygulamamızda ortak aile havuzumuz açıldı.\nAile Havuz Kodumuz: $familyPoolCode\nDavet Edilen Rol: $inviteRoleInput\n\nOrtak havuza bağlanmak için uygulamayı açıp 'Kodu Onayla & Aile Havuzuna Katıl' butonuna $familyPoolCode kodunu girmeniz yeterlidir! 🤲"
                                )
                            }
                            try {
                                context.startActivity(mailIntent)
                            } catch (_: Exception) { }
                            viewModel.showStatusMessage("$email adresine davet hazırlandı! ✉️")
                        }
                        showEmailInviteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailInviteDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // ESKİ EŞLEŞMEYİ SIFIRLA VE TEMİZLE DİALOGU (User request 4)
    if (showResetHandshakeDialog) {
        AlertDialog(
            onDismissRequest = { showResetHandshakeDialog = false },
            title = { Text("Bağlantıyı Sıfırla & Temizle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("Eğer karşı taraf uygulamayı kaldırıp yeniden kurduysa veya bağlantı güncel değilse, eski eşleşmeyi sonlandırıp sıfırdan taze kodla eşleşebilirsiniz. Onaylıyor musunuz?", fontSize = 13.sp, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unmatchPartner()
                        showResetHandshakeDialog = false
                        viewModel.showStatusMessage("Eski bağlantı sıfırlandı. Yeni kodla tekrar bağlanabilirsiniz.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) {
                    Text("Sıfırla ve Temizle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetHandshakeDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // VERİTABANI & BULUT MİMARİSİ BİLGİ DİALOGU (User request 10)
    if (showArchitectureInfoDialog) {
        AlertDialog(
            onDismissRequest = { showArchitectureInfoDialog = false },
            title = { Text("Veri Tabanı & Senkronizasyon", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "1. Yerel Veritabanı (Cihaz İçi - Room SQLite):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tüm namaz kayıtlarınız, kaza sayaçlarınız cihazınızın yerel SQLite veritabanında güvenle saklanır. İnternetiniz olmasa bile hiçbir veriniz kaybolmaz.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "2. Çevrimiçi Havuz Veritabanı (Bulut Senkronizasyonu):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "1. Kişinin Aile Kodu ($familyPoolCode) üzerinden aile havuzu oluşturulur. Google ile giriş yapan aile üyeleri bu kod ile bağlandığında, herkesin kıldığı namazlar anlık olarak ortak havuza aktarılır ve karşılıklı eşitlenir.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showArchitectureInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text("Anladım")
                }
            }
        )
    }
}

@Composable
private fun FamilyMatrixStatusCell(status: String) {
    when (status) {
        "PRAYED" -> {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Kıldı", tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Kıldı", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            }
        }
        "MISSED" -> {
            Surface(
                color = Color(0xFFDC2626),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Kılmadı", tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Kaza", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        "EXCUSED" -> {
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Muaf", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        else -> {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Bekliyor", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun IndividualPersonCard(
    personName: String,
    personIcon: String,
    isToday: Boolean,
    prayers: List<String>,
    yesterdayPrayers: List<String>,
    onEdit: () -> Unit,
    onToggle: (Int) -> Unit,
    onSendDua: () -> Unit
) {
    val prayedCount = prayers.count { it == "PRAYED" }
    val isPerfect = (prayedCount == 5)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = personIcon, fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = personName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            text = "Bireysel İbadet Takibi",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Surface(
                    color = if (isPerfect) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPerfect) "Günün Yıldızı 🌟" else "$prayedCount/5 Vakit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPerfect) Color(0xFF78350F) else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bugünkü Vakitler
            Text(
                text = "$personName - ${if (isToday) "Bugünkü" else "Dünkü"} Namazları (Dokunarak Değiştir):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val names = listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı")
                names.forEachIndexed { idx, name ->
                    val status = prayers.getOrElse(idx) { "NONE" }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggle(idx) }
                    ) {
                        PartnerMiniStatusPineItem(name, status)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Dünkü Durum
            Text(
                text = "📅 Dünkü Durumu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PartnerMiniStatusPineItem("Sabah", yesterdayPrayers.getOrElse(0) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Öğle", yesterdayPrayers.getOrElse(1) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("İkindi", yesterdayPrayers.getOrElse(2) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Akşam", yesterdayPrayers.getOrElse(3) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Yatsı", yesterdayPrayers.getOrElse(4) { "NONE" }, isCompact = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSendDua,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "👏", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tebrik Et & Dua Gönder", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PartnerMiniStatusPineItem(name: String, status: String, isCompact: Boolean = false) {
    val circleSize = if (isCompact) 22.dp else 28.dp
    val iconSize = if (isCompact) 13.dp else 16.dp
    val nameSize = if (isCompact) 10.sp else 11.sp
    val statusSize = if (isCompact) 9.sp else 10.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, fontSize = nameSize, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        when (status) {
            "PRAYED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = "Kıldı", tint = EmeraldPrimary, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Kıldı", fontSize = statusSize, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
            }
            "MISSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Kılmadı", tint = Color.White, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Kılmadı", fontSize = statusSize, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
            }
            "EXCUSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Muaf", tint = Color.White, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Muaf", fontSize = statusSize, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))
            }
            else -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "—", fontSize = if (isCompact) 11.sp else 14.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Bekliyor", fontSize = statusSize, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun FamilyMemberKazaStatusPill(
    icon: String,
    name: String,
    owed: Int,
    completed: Int,
    onAdd: (() -> Unit)? = null
) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Borç: $owed vakit • Kılınan: $completed vakit",
                    fontSize = 10.sp,
                    color = Color(0xFFFEF3C7)
                )
            }
            if (onAdd != null) {
                Surface(
                    onClick = onAdd,
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White
                ) {
                    Text(
                        text = "+1 Kıldı",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

