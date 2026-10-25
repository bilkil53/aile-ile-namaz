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
import androidx.compose.foundation.verticalScroll
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
import com.example.util.PartnerSyncManager
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
    val yesterdayPrayer by viewModel.yesterdayPrayer.collectAsState()
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
        // Kullanıcının açıkça eklemediği hiçbir hayalet/örnek kişi (Çocuk, Kişi 2, vb.) havuza ve tabloya gelmesin
        if (!prefs.contains("family_cleanup_no_dummies_v16")) {
            prefs.edit().apply {
                // Eski varsayılan örnek kişileri tamamen temizle
                for (id in 2..25) {
                    remove("has_p$id")
                    remove("p${id}_name")
                    remove("p${id}_icon")
                    for (dayIdx in 0..1) {
                        for (pIdx in 0..4) {
                            remove("m_${id}_d${dayIdx}_p$pIdx")
                        }
                    }
                }
                remove("has_p2_customized")
                remove("p2_fajr")
                remove("p2_dhuhr")
                remove("p2_asr")
                remove("p2_maghrib")
                remove("p2_isha")
                remove("p2_yesterday_fajr")
                remove("p2_yesterday_dhuhr")
                remove("p2_yesterday_asr")
                remove("p2_yesterday_maghrib")
                remove("p2_yesterday_isha")
                putString("custom_extra_member_ids", "NONE")
                putString("custom_extra_member_ids", "NONE")
                putBoolean("family_cleanup_no_dummies_v16", true)
                apply()
            }
            val savedPool = prefs.getString("family_pool_code", null)
            val isJoined = prefs.getBoolean("is_pool_member_joined", false)
            if (savedPool != null && (!savedPool.startsWith("REIS-00") && (!isJoined || savedPool.startsWith("EMB") || savedPool.startsWith("ARK") || savedPool == "REIS-5385"))) {
                prefs.edit().remove("family_pool_code").apply()
            }
        }
        prefs
    }

    // Kodun REIS-00 ile başladığından daima emin ol
    LaunchedEffect(partnerInfo.myInviteCode) {
        if (partnerInfo.myInviteCode.isBlank() || !partnerInfo.myInviteCode.startsWith("REIS-00")) {
            viewModel.ensureUniqueInviteCode()
        }
    }

    var familyPoolCode by remember(partnerInfo.myInviteCode, partnerInfo.partnerInviteCode, refreshTrigger) {
        val saved = childPrefs.getString("family_pool_code", null)
        val validCode = if (!saved.isNullOrBlank()) {
            PartnerSyncManager.formatDisplayCode(saved)
        } else if (partnerInfo.partnerInviteCode.isNotBlank()) {
            partnerInfo.partnerInviteCode
        } else if (partnerInfo.myInviteCode.startsWith("REIS-00")) {
            partnerInfo.myInviteCode
        } else {
            ""
        }
        mutableStateOf(validCode)
    }

    // Aile Havuzu Liderlik ve Dahiliyet Durumları (Aile Reisi ve Aile Fertleri)
    val isPoolMemberJoined = remember(refreshTrigger) {
        childPrefs.getBoolean("is_pool_member_joined", false)
    }
    val userExplicitRole = remember(refreshTrigger) {
        childPrefs.getString("user_role", "") ?: ""
    }
    val isExplicitReis = remember(refreshTrigger) {
        childPrefs.getBoolean("is_explicit_reis", false)
    }

    // Kişi 1 Takma Adı (Asla e-posta adresi görünmez, takma ad garantisi)
    val p1DisplayName = remember(settings.userName, settings.userEmail, refreshTrigger) {
        val emailKey = settings.userEmail.trim().lowercase()
        val savedNick = childPrefs.getString("user_nickname_$emailKey", null)
            ?: childPrefs.getString("p1_name", null)
        val raw = when {
            !savedNick.isNullOrBlank() && !savedNick.contains("@") && savedNick != "Mümin" -> savedNick
            settings.userName.isNotBlank() && !settings.userName.contains("@") && settings.userName != "Mümin" -> settings.userName
            else -> "Kişi 1"
        }
        if (raw.contains("@")) raw.substringBefore("@") else raw
    }

    val nameLower = p1DisplayName.trim().lowercase(java.util.Locale.forLanguageTag("tr"))
    val isNameChildLike = remember(nameLower) {
        nameLower.contains("çocuk") || nameLower.contains("cocuk") ||
            nameLower.contains("evlat") || nameLower.contains("kız") || nameLower.contains("kiz") ||
            nameLower.contains("oğul") || nameLower.contains("ogul") || nameLower.contains("bebek") ||
            nameLower.contains("torun") || nameLower.contains("fert")
    }

    // Başkasının havuzuna dahil olan veya çocuk statüsündeki aile ferdi mi?
    val isJoinedMember = remember(isPoolMemberJoined, userExplicitRole, isExplicitReis, isNameChildLike, partnerInfo.isMatched, partnerInfo.partnerInviteCode, familyPoolCode, partnerInfo.myInviteCode, refreshTrigger) {
        if (isNameChildLike || userExplicitRole == "FERT" || userExplicitRole == "COCUK") {
            true
        } else if (isExplicitReis || userExplicitRole == "REIS") {
            false
        } else {
            isPoolMemberJoined ||
                (familyPoolCode.isNotBlank() && familyPoolCode != partnerInfo.myInviteCode && childPrefs.contains("family_pool_code"))
        }
    }

    // Aile Reisi: Asla çocuk veya dahil olan ferde verilmez! Yalnızca havuz kurucu veya açıkça Reis olan kullanıcıya verilir.
    val isPoolLeader = remember(isJoinedMember, isNameChildLike, userExplicitRole, isExplicitReis, partnerInfo.isMatched, refreshTrigger) {
        !isJoinedMember && !isNameChildLike && userExplicitRole != "FERT" && userExplicitRole != "COCUK" &&
            (isExplicitReis || userExplicitRole == "REIS" || (!isPoolMemberJoined && (partnerInfo.isMatched || childPrefs.getBoolean("family_pool_has_members", false))))
    }

    val isInFamilyPool = remember(settings.isLoggedIn, settings.userEmail, partnerInfo.isMatched, familyPoolCode, isPoolMemberJoined, isJoinedMember, refreshTrigger) {
        (settings.isLoggedIn || settings.userEmail.isNotBlank()) &&
            (childPrefs.getBoolean("family_pool_created", false) || partnerInfo.isMatched || isPoolMemberJoined || isJoinedMember || familyPoolCode.isNotBlank())
    }

    val poolLeaderDisplayName = remember(isPoolLeader, isJoinedMember, settings.userName, partnerInfo.partnerDisplayName, refreshTrigger) {
        val raw = if (isPoolLeader) {
            if (settings.userName.isNotBlank() && !settings.userName.contains("@") && settings.userName != "Mümin") {
                settings.userName
            } else {
                childPrefs.getString("p1_name", "Aile Reisi") ?: "Aile Reisi"
            }
        } else {
            if (partnerInfo.partnerDisplayName.isNotBlank() && !partnerInfo.partnerDisplayName.contains("@") && partnerInfo.partnerDisplayName != "Mümin") {
                partnerInfo.partnerDisplayName
            } else {
                val p2 = childPrefs.getString("p2_name", "Aile Reisi") ?: "Aile Reisi"
                if (p2.isNotBlank() && p2 != "Kişi 2") p2 else "Aile Reisi"
            }
        }
        if (raw.contains("@")) raw.substringBefore("@") else raw
    }

    // Kişi 1 (Siz / Kendiniz)
    var person1Icon by remember(refreshTrigger) { mutableStateOf(childPrefs.getString("p1_icon", "🧔") ?: "🧔") }

    // Kişi 2 (Eşleşmişse, havuza katılmışsa veya açıkça eklenmişse aktiftir; varsayılan kapalı)
    var hasPerson2 by remember(partnerInfo.isMatched, isJoinedMember, refreshTrigger) {
        mutableStateOf(partnerInfo.isMatched || isJoinedMember || childPrefs.getBoolean("has_p2_customized", false))
    }
    var person2Name by remember(partnerInfo.partnerDisplayName, isJoinedMember, poolLeaderDisplayName, refreshTrigger) {
        val raw = if (isJoinedMember) {
            if (partnerInfo.partnerDisplayName.isNotBlank() && !partnerInfo.partnerDisplayName.contains("@")) {
                partnerInfo.partnerDisplayName
            } else {
                childPrefs.getString("p2_name", poolLeaderDisplayName) ?: poolLeaderDisplayName
            }
        } else {
            partnerInfo.partnerDisplayName.ifBlank { childPrefs.getString("p2_name", "Kişi 2") ?: "Kişi 2" }
        }
        val clean = if (raw.contains("@")) raw.substringBefore("@") else raw
        val normalized = com.example.util.FamilyRoleHelper.normalizeRoleTitle(clean)
        mutableStateOf(normalized)
    }
    var person2Icon by remember(partnerInfo.partnerDisplayName, person2Name, refreshTrigger) {
        val saved = childPrefs.getString("p2_icon", null)
        val defaultRoleIcon = com.example.util.FamilyRoleHelper.getDefaultIconForRole(person2Name)
        val icon = if (!saved.isNullOrBlank() && !com.example.util.FamilyRoleHelper.isIconConflictingWithRole(saved, person2Name)) {
            saved
        } else {
            defaultRoleIcon
        }
        mutableStateOf(icon)
    }

    LaunchedEffect(person2Name) {
        val saved = childPrefs.getString("p2_icon", null)
        val roleIcon = com.example.util.FamilyRoleHelper.getDefaultIconForRole(person2Name)
        if (saved.isNullOrBlank() || com.example.util.FamilyRoleHelper.isIconConflictingWithRole(saved, person2Name)) {
            person2Icon = roleIcon
            childPrefs.edit().putString("p2_icon", roleIcon).apply()
        }
    }

    // Ek Bireyler (Yalnızca kullanıcı eklediyse görünür; varsayılan olarak kesinlikle boştur)
    var extraMemberIds by remember(refreshTrigger) {
        val saved = childPrefs.getString("custom_extra_member_ids", "NONE") ?: "NONE"
        val list = if (saved == "NONE" || saved.isBlank()) {
            emptyList<Int>()
        } else {
            saved.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it >= 3 && childPrefs.getBoolean("has_p$it", false) }
                .distinct()
        }
        mutableStateOf(list)
    }

    // Dua Gönderme Dialog Durumları (User request 6)
    var showSendDuaDialog by remember { mutableStateOf(false) }
    var duaTargetName by remember { mutableStateOf("Tüm Ailemiz") }
    var selectedDuaPreset by remember { mutableStateOf("Allah ibadetlerimizi ve namazlarımızı kabul eylesin! 🤲") }
    var customDuaText by remember { mutableStateOf("") }

    // Giriş durumu: Gerçek kullanıcı deneyimi (Deneme modu kaldırıldı)
    val isGuestMode = false
    var simP1Prayers by remember { mutableStateOf(listOf("NONE", "NONE", "NONE", "NONE", "NONE")) }
    var simP2Prayers by remember { mutableStateOf(listOf("NONE", "NONE", "NONE", "NONE", "NONE")) }

    // Uygulama Modu: "FAMILY" veya "PARTNER"
    var appMode by remember {
        mutableStateOf(childPrefs.getString("app_mode", "FAMILY") ?: "FAMILY")
    }

    // Tarih Seçimi: Aile Tablosu dünün veya bugünün durumunu gösterebilir (0: Bugün, -1: Dün)
    var viewingFamilyDateOffset by remember { mutableStateOf(0) }
    val isViewingTodayInFamily = (viewingFamilyDateOffset == 0)

    // Dinamik Birey Yardımcı Fonksiyonları
    fun getMemberName(id: Int): String {
        val raw = childPrefs.getString("p${id}_name", "Kişi $id") ?: "Kişi $id"
        return if (raw.contains("@")) raw.substringBefore("@") else raw
    }

    fun getMemberIcon(id: Int): String {
        val name = getMemberName(id)
        val saved = childPrefs.getString("p${id}_icon", null)
        if (!saved.isNullOrBlank() && !com.example.util.FamilyRoleHelper.isIconConflictingWithRole(saved, name)) {
            return saved
        }
        return com.example.util.FamilyRoleHelper.getDefaultIconForRole(name)
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

    // Kişi 2 Namaz Durumları (Partner eşleşmesinden veya yerel kayıttan hem bugün hem dün)
    fun getP2Prayer(pKey: String, isToday: Boolean): String {
        val dateKey = if (isToday) todayDateStr else yesterdayDateStr
        if (partnerInfo.isMatched) {
            return when (pKey) {
                "fajr" -> if (isToday) partnerInfo.partnerFajr else partnerInfo.partnerYesterdayFajr
                "dhuhr" -> if (isToday) partnerInfo.partnerDhuhr else partnerInfo.partnerYesterdayDhuhr
                "asr" -> if (isToday) partnerInfo.partnerAsr else partnerInfo.partnerYesterdayAsr
                "maghrib" -> if (isToday) partnerInfo.partnerMaghrib else partnerInfo.partnerYesterdayMaghrib
                else -> if (isToday) partnerInfo.partnerIsha else partnerInfo.partnerYesterdayIsha
            }
        }
        val defaultVal = when (pKey) {
            "fajr" -> if (isToday) partnerInfo.partnerFajr else partnerInfo.partnerYesterdayFajr
            "dhuhr" -> if (isToday) partnerInfo.partnerDhuhr else partnerInfo.partnerYesterdayDhuhr
            "asr" -> if (isToday) partnerInfo.partnerAsr else partnerInfo.partnerYesterdayAsr
            "maghrib" -> if (isToday) partnerInfo.partnerMaghrib else partnerInfo.partnerYesterdayMaghrib
            else -> if (isToday) partnerInfo.partnerIsha else partnerInfo.partnerYesterdayIsha
        }
        return childPrefs.getString("p2_local_${pKey}_$dateKey", defaultVal) ?: defaultVal
    }

    fun setP2Prayer(pKey: String, newStatus: String, isToday: Boolean) {
        val dateKey = if (isToday) todayDateStr else yesterdayDateStr
        childPrefs.edit().putString("p2_local_${pKey}_$dateKey", newStatus).apply()
        refreshTrigger++
    }

    val p2TodayPrayers = listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { getP2Prayer(it, true) }
    val p2YesterdayPrayers = listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { getP2Prayer(it, false) }

    val p1TodayPrayers = listOf(todayPrayer.fajrStatus, todayPrayer.dhuhrStatus, todayPrayer.asrStatus, todayPrayer.maghribStatus, todayPrayer.ishaStatus)
    val p1YesterdayPrayers = listOf(yesterdayPrayer.fajrStatus, yesterdayPrayer.dhuhrStatus, yesterdayPrayer.asrStatus, yesterdayPrayer.maghribStatus, yesterdayPrayer.ishaStatus)

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
    var showLeavePoolDialog by remember { mutableStateOf(false) }
    var showStartPoolRoleDialog by remember { mutableStateOf(false) }
    var leaderRoleInput by remember { mutableStateOf("Baba") }
    var leaderIconInput by remember { mutableStateOf("🧔") }
    var showArchitectureInfoDialog by remember { mutableStateOf(false) }

    // Google Account Picker Launcher
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                viewModel.loginWithGoogleBrowser(email = accountName, name = "")
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
                            val myNick = p1DisplayName
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = myNick,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        editingPersonIndex = 1
                                        editPersonNameInput = myNick
                                        editPersonIconInput = person1Icon
                                        editPersonActiveInput = true
                                        showEditPersonDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "İsmi Düzenle", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Google Hesabı Bağlı ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isPoolLeader) Color(0xFF16A34A) else if (isJoinedMember) Color(0xFF0284C7) else Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isPoolLeader) "👑 Aile Reisi" else if (isJoinedMember) "👥 Aile Ferdi" else "👥 Aile Ferdi",
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


        // -----------------------------------------------------------------
        // AİLE DURUM PANELİ (Canlı Bağlantı, Eşleşen Kişiler, Aktif Kod, Detaysız Kaza)
        // -----------------------------------------------------------------
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
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
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Diversity3, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Aile Havuzu • İbadet Halkası", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (partnerInfo.isMatched) Color(0xFF16A34A) else Color.White.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = if (partnerInfo.isMatched) "CANLI 🟢" else "BEKLEMEDE ⏳",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Ailece 5 Vakit Namaz İbadet Halkası",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        IconButton(onClick = { showArchitectureInfoDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = "Bilgi", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AİLE REİSİ BİLGİ KARTI (Sadece havuz kurulduğunda veya dahil olunduğunda)
                    if (isPoolLeader || isJoinedMember) {
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPoolLeader) "👑" else "🕌", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPoolLeader) "Aile Reisi: $poolLeaderDisplayName (Siz)" else "Aile Reisi: $poolLeaderDisplayName",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFEF3C7)
                                    )
                                    Text(
                                        text = if (isPoolLeader) "Aile reisisiniz • Havuzunuz aktif 🟢" 
                                               else "Aile havuzuna dahilsiniz • Kıldığınız namazlar havuza aktarılır 🟢",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPoolLeader) Color(0xFF16A34A) else Color(0xFF0284C7)
                                ) {
                                    Text(
                                        text = if (isPoolLeader) "AİLE REİSİ 👑" else "AİLE FERDİ 👥",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👨‍👩‍👧‍👦", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aile İbadet Havuzu",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFEF3C7)
                                    )
                                    Text(
                                        text = "Henüz bir havuza bağlı değilsiniz. Kod girerek katılabilir veya kendi havuzunuzu başlatabilirsiniz.",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sınırsız Kişi & Çoklu Aile Havuzu",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Aileniz, eşiniz ve çocuklarınız tek bir aile kodu ile aynı havuza bağlanabilir.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. MEVCUT AİLE BAĞLANTI DURUMU
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
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
                                        if (partnerInfo.isMatched) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = if (partnerInfo.isMatched) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (partnerInfo.isMatched) "Çift Taraflı Bağlantı Aktif 🟢" 
                                               else "Bağlantı Bekleniyor ⏳",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // CANLI TEYİT BUTONU
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Surface(
                                        onClick = {
                                            viewModel.refreshPartnerData()
                                            viewModel.showStatusMessage("Havuz bağlantı durumu sorgulanıyor... Canlı teyit sağlandı! 🟢")
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Bağlantıyı Teyit Et 🔄", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (partnerInfo.isMatched) {
                                    "$person2Name ile çift taraflı bağlantı sağlandı. Kılınan namazlar anlık senkronize ediliyor."
                                } else {
                                    "2. Kişi henüz havuz kodunu onaylamadı. Bağlantının sağlanması için aşağıdaki davet kodunuzu aile ferdinizle paylaşın."
                                },
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )

                            if (partnerInfo.isMatched) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bağlantıyı Yeniden Kur / Sıfırla (Temizle)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.clickable { showResetHandshakeDialog = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. AİLE HAVUZ KODU VEYA KATILIM ALANI
                    if (isPoolLeader) {
                        // Sadece Aile Reisi için: Aile Paylaşım Kodu kartı görünür
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Aile Havuz Paylaşım Kodunuz (👑 Aile Reisi)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                                        Text(text = familyPoolCode.ifBlank { partnerInfo.myInviteCode.ifBlank { "REIS-00..." } }, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFEF3C7))
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Aile Davet Kodu", familyPoolCode)
                                                clipboard.setPrimaryClip(clip)
                                                viewModel.showStatusMessage("Aile Havuz Kodu kopyalandı: $familyPoolCode 📋")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Kodu Kopyala", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Selamün Aleyküm! Aile ile Namaz uygulamamızda aile havuzumuz açıldı. Aile Havuz Kodumuz: $familyPoolCode . Uygulamaya bu kodu girerek ailemizin ibadet halkasına dahil olabilirsiniz! 🤲"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Aile Havuz Kodunu Paylaş"))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = "Kodu Paylaş", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bu kodu aileniz ve çocuklarınızla paylaşın. Onlar uygulamadan bu kodu girince otomatik olarak havuza dahil olurlar.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { showLeavePoolDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Havuzu Kapat & Sıfırla", fontSize = 11.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (isJoinedMember) {
                        // Havuza dahil olan aile ferdi: Davet kodu ekranı tamamen kalkar!
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🕌", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Aile İbadet Havuzundasınız",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Aile Reisinizin ($poolLeaderDisplayName) ortak havuzuna dahilsiniz • Kıldığınız namazlar anlık havuza aktarılır 🟢",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC8E6C9)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showLeavePoolDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MissedRed),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Havuzdan Ayrıl", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        // Havuza henüz katılmamış kullanıcı: Hem kod girerek katılma alanı hem kendi kodunu paylaşma alanı
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. KART: KOD GİREREK AİLE HAVUZUNA KATIL (GÖZ ÖNÜNDE VE ERİŞİLEBİLİR)
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC).copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📥", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Aile Havuzuna Katıl (Kod Gir)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Aile Reisinizin (babanız/eşiniz) size ilettiği REIS-00 kodunu girerek havuza bağlanın:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = joinPoolCodeInput,
                                            onValueChange = { joinPoolCodeInput = it.uppercase() },
                                            placeholder = { Text("Örn: REIS-001234", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF86EFAC),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val raw = joinPoolCodeInput.trim()
                                                if (raw.isBlank()) {
                                                    viewModel.showStatusMessage("Lütfen bir aile havuz kodu giriniz.")
                                                    return@Button
                                                }
                                                val normInput = com.example.util.PartnerSyncManager.normalizeCode(raw)
                                                val normMine = com.example.util.PartnerSyncManager.normalizeCode(partnerInfo.myInviteCode)
                                                if (normInput == normMine) {
                                                    viewModel.showStatusMessage("⚠️ Bu sizin kendi aile kodunuzdur. Başka bir aile ferdinizin kodunu girmelisiniz.")
                                                    return@Button
                                                }
                                                // Rol seçimini zorunlu kılmak için onay ve rol seçim ekranını aç
                                                showJoinPoolDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text("Havuza Katıl", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // 2. KART: KENDİ AİLE HAVUZUNU KUR (AİLE REİSİ OL)
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("👑", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Veya Kendi Aile Havuzunuzu Başlatın", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7))
                                            }
                                            Text(
                                                text = partnerInfo.myInviteCode.ifBlank { "REIS-00..." },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Aile Davet Kodu", partnerInfo.myInviteCode)
                                                    clipboard.setPrimaryClip(clip)
                                                    viewModel.showStatusMessage("Kod kopyalandı: ${partnerInfo.myInviteCode} 📋")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = "Kodu Kopyala", tint = Color.White, modifier = Modifier.size(17.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            "Selamün Aleyküm! Aile ile Namaz uygulamamızda aile havuzumuz açıldı. Aile Havuz Kodumuz: ${partnerInfo.myInviteCode} . Bu kodu girerek havuzumuza dahil olabilirsiniz! 🤲"
                                                        )
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Kodu Paylaş"))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Share, contentDescription = "Kodu Paylaş", tint = Color.White, modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ailenizi davet etmek için bu kodu paylaşın veya hemen havuzu başlatıp Aile Reisi unvanınızı belirleyin.",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showStartPoolRoleDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("👑 Aile Havuzunu Başlat (Reis Rolü Seç)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Kişi 1 (Siz)
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
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
                                            text = p1DisplayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPoolLeader) "(Siz • 👑 Aile Reisi)" else if (isNameChildLike) "(Siz • 👦 Aile Ferdi / Çocuk)" else "(Siz • 👥 Aile Ferdi)",
                                            fontSize = 10.sp,
                                            color = if (isPoolLeader) Color(0xFFFEF3C7) else Color(0xFF86EFAC),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = if (isPoolLeader) "Google ile Bağlı ✓ • Aile Reisi" else if (isJoinedMember) "Google ile Bağlı ✓ • Aile Ferdi" else "Google ile Bağlı ✓",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingPersonIndex = 1
                                        editPersonNameInput = p1DisplayName
                                        editPersonIconInput = person1Icon
                                        editPersonActiveInput = true
                                        showEditPersonDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Kişi 2
                        if (partnerInfo.isMatched || hasPerson2 || isJoinedMember) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = person2Icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = person2Name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isPoolLeader) "• 👥 Aile Ferdi" else if (isJoinedMember) "• 👑 Aile Reisi" else "• Kişi 2",
                                                fontSize = 10.sp,
                                                color = if (isPoolLeader) Color(0xFF86EFAC) else Color(0xFFFEF3C7),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = if (partnerInfo.isMatched || isJoinedMember) "Aile Havuzunda Bağlı 🟢 • Aktif" else "Davet Bekleniyor ⏳ • Kod Gönderildi",
                                            fontSize = 10.sp,
                                            color = if (partnerInfo.isMatched || isJoinedMember) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (isPoolLeader) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editingPersonIndex = 2
                                                    editPersonNameInput = person2Name
                                                    editPersonIconInput = person2Icon
                                                    editPersonActiveInput = hasPerson2
                                                    showEditPersonDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    hasPerson2 = false
                                                    childPrefs.edit()
                                                        .putBoolean("has_p2", false)
                                                        .putBoolean("has_p2_customized", false)
                                                        .remove("p2_name")
                                                        .remove("p2_icon")
                                                        .apply()
                                                    if (selectedFamilyTab == 1) selectedFamilyTab = 0
                                                    if (partnerInfo.isMatched) {
                                                        viewModel.unmatchPartner()
                                                    }
                                                    refreshTrigger++
                                                    viewModel.showStatusMessage("Kişi 2 havuzdan kaldırıldı.")
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.PersonRemove, contentDescription = "Kaldır", tint = Color(0xFFFFCDD2), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isPoolLeader) {
                            Surface(
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = Color(0xFFFEF3C7), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Aile fertleriniz yukarıdaki REIS kodunuz ile havuza dahil oldukça burada listelenecektir ⏳",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                            }
                        }

                        // Ek Bireyler (Yalnızca kullanıcı açıkça eklemişse görünür)
                        extraMemberIds.forEach { id ->
                            val mName = getMemberName(id)
                            val mIcon = getMemberIcon(id)
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = mIcon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = mName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "• 👥 Aile Üyesi", fontSize = 10.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(text = "Aile Havuzunda Aktif 🟢", fontSize = 10.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                                    }
                                    if (isPoolLeader) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editingPersonIndex = id
                                                    editPersonNameInput = mName
                                                    editPersonIconInput = mIcon
                                                    editPersonActiveInput = true
                                                    showEditPersonDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val targetId = id
                                                    val updated = extraMemberIds.filter { it != targetId }
                                                    extraMemberIds = updated
                                                    childPrefs.edit()
                                                        .putString("custom_extra_member_ids", if (updated.isEmpty()) "NONE" else updated.joinToString(","))
                                                        .remove("has_p$targetId")
                                                        .remove("p${targetId}_name")
                                                        .remove("p${targetId}_icon")
                                                        .apply()
                                                    for (dayIdx in 0..1) {
                                                        for (pIdx in 0..4) {
                                                            childPrefs.edit().remove("m_${targetId}_d${dayIdx}_p$pIdx").apply()
                                                        }
                                                    }
                                                    if (selectedFamilyTab == targetId) selectedFamilyTab = 0
                                                    refreshTrigger++
                                                    viewModel.showStatusMessage("$mName havuzdan ve tablodan kaldırıldı.")
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.PersonRemove, contentDescription = "Kaldır", tint = Color(0xFFFFCDD2), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // =================================================================
            // AİLE İBADET TABLOSU (ORTAK MATRİS - TÜM AİLE FERTLERİ BİRLİKTE)
            // =================================================================
            item {
                    val p1Statuses = if (isGuestMode) {
                        simP1Prayers
                    } else if (isViewingTodayInFamily) {
                        p1TodayPrayers
                    } else {
                        p1YesterdayPrayers
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
                                            text = if (p1DisplayName.isNotBlank() && p1DisplayName != "Kişi 1") "$p1DisplayName Ailesi" else "Aile İbadet Halkası",
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
                                        Box(
                                            modifier = Modifier
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    editingPersonIndex = 1
                                                    editPersonNameInput = p1DisplayName
                                                    editPersonIconInput = person1Icon
                                                    editPersonActiveInput = true
                                                    showEditPersonDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$person1Icon $p1DisplayName",
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
                                                        } else {
                                                            val prayerType = when (idx) {
                                                                0 -> "FAJR"; 1 -> "DHUHR"; 2 -> "ASR"; 3 -> "MAGHRIB"; else -> "ISHA"
                                                            }
                                                            val next = when (status) {
                                                                "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                            }
                                                            viewModel.setPrayerStatusForDate(yesterdayDateStr, prayerType, next)
                                                            viewModel.showStatusMessage("Dünkü ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
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
                                            Box(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        editingPersonIndex = 2
                                                        editPersonNameInput = person2Name
                                                        editPersonIconInput = person2Icon
                                                        editPersonActiveInput = hasPerson2
                                                        showEditPersonDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
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
                                                                val pKey = when (idx) { 0 -> "fajr"; 1 -> "dhuhr"; 2 -> "asr"; 3 -> "maghrib"; else -> "isha" }
                                                                setP2Prayer(pKey, next, true)
                                                                viewModel.showStatusMessage("$person2Name: ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
                                                            }
                                                        } else {
                                                            val next = when (status) {
                                                                "NONE" -> "PRAYED"; "PRAYED" -> "MISSED"; "MISSED" -> "EXCUSED"; else -> "NONE"
                                                            }
                                                            val pKey = when (idx) { 0 -> "fajr"; 1 -> "dhuhr"; 2 -> "asr"; 3 -> "maghrib"; else -> "isha" }
                                                            setP2Prayer(pKey, next, false)
                                                            viewModel.showStatusMessage("$person2Name dünkü ${getPrayerNameStr(idx)} durumu $next yapıldı 🔄")
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
                                            Box(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        editingPersonIndex = extraId
                                                        editPersonNameInput = mName
                                                        editPersonIconInput = mIcon
                                                        editPersonActiveInput = true
                                                        showEditPersonDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
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
                                }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manevi Dua Butonu (Vakti Hatırlat butonu kaldırıldı, sadeleştirildi)
                            Button(
                                onClick = {
                                    duaTargetName = "Tüm Ailemiz"
                                    showSendDuaDialog = true
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kişi $editingPersonIndex Bilgilerini Düzenle",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            },
            containerColor = PureWhite,
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (isPoolLeader) {
                        // Kadın, Anne & Eş İkonları
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Kadın / Anne / Eş İkonları (Aile Reisi Ayarı)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Erkek / Baba / Eş İkonları (Aile Reisi Ayarı)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Çocuk & Bebek İkonları (Aile Reisi Ayarı)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Profil & Diğer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                    } else {
                        // İkon Seçimi Kilitli Uyarısı (Sadece Aile Reisi yetkilidir)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔒", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "İkon Ayarlama Yetkisi Aile Reisindedir",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Aile havuzunda ikonları sadece Aile Reisi ($poolLeaderDisplayName) ayarlayabilir. Siz aile havuzunda görünecek takma adınızı düzenleyebilirsiniz.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hızlı Aile Şablonları (Eş, Çocuk, Bebek, Anne, Baba, Kişi)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hızlı Aile Rolü Şablonu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val quickRoles = listOf(
                        "Oğlu" to "👦",
                        "Kızı" to "👧",
                        "Eşi" to "🧕",
                        "Annesi" to "🧕",
                        "Babası" to "🧔",
                        "Kardeşi" to "🧒"
                    )
                    quickRoles.chunked(3).forEach { rowRoles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowRoles.forEach { (role, ic) ->
                                val isSel = (editPersonNameInput == role)
                                Surface(
                                    onClick = {
                                        editPersonNameInput = role
                                        editPersonIconInput = ic
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$ic $role",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                        onValueChange = { inputVal ->
                            editPersonNameInput = inputVal
                            val lower = inputVal.lowercase(java.util.Locale.forLanguageTag("tr"))
                            if (lower.contains("oğul") || lower.contains("ogul") || lower.contains("oğlum") || lower.contains("oglum")) {
                                editPersonIconInput = "👦"
                            } else if (lower.contains("kız") || lower.contains("kiz") || lower.contains("kızım") || lower.contains("kizim")) {
                                editPersonIconInput = "👧"
                            }
                        },
                        label = { Text("İsim / Takma Ad (Kişi $editingPersonIndex)") },
                        placeholder = { Text("İsmi buraya yazınız...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Yalnızca Aile Reisi bireyleri havuzdan çıkarabilir
                    if (editingPersonIndex >= 2 && isPoolLeader) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                if (editingPersonIndex == 2) {
                                    hasPerson2 = false
                                    childPrefs.edit()
                                        .putBoolean("has_p2", false)
                                        .putBoolean("has_p2_customized", false)
                                        .remove("p2_name")
                                        .remove("p2_icon")
                                        .apply()
                                    if (selectedFamilyTab == 1) selectedFamilyTab = 0
                                    if (partnerInfo.isMatched) {
                                        viewModel.unmatchPartner()
                                    }
                                } else {
                                    val targetId = editingPersonIndex
                                    val updated = extraMemberIds.filter { it != targetId }
                                    extraMemberIds = updated
                                    childPrefs.edit()
                                        .putString("custom_extra_member_ids", if (updated.isEmpty()) "NONE" else updated.joinToString(","))
                                        .remove("has_p$targetId")
                                        .remove("p${targetId}_name")
                                        .remove("p${targetId}_icon")
                                        .apply()
                                    for (dayIdx in 0..1) {
                                        for (pIdx in 0..4) {
                                            childPrefs.edit().remove("m_${targetId}_d${dayIdx}_p$pIdx").apply()
                                        }
                                    }
                                    if (selectedFamilyTab == targetId) selectedFamilyTab = 0
                                }
                                refreshTrigger++
                                showEditPersonDialog = false
                                viewModel.showStatusMessage("Kişi havuzdan ve tablodan kaldırıldı.")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MissedRed),
                            border = BorderStroke(1.dp, MissedRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = null, tint = MissedRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bu Kişiyi Havuzdan ve Tablodan Kaldır", fontSize = 12.sp, color = MissedRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = editPersonNameInput.trim()
                        if (raw.contains("@")) {
                            viewModel.showStatusMessage("E-posta adresi değil, lütfen kendinize özel bir isim/takma ad yazınız.")
                            return@Button
                        }
                        if (raw.length < 2) {
                            viewModel.showStatusMessage("Lütfen en az 2 harfli bir takma ad yazınız.")
                            return@Button
                        }
                        val name = raw
                        when (editingPersonIndex) {
                            1 -> {
                                val lower = name.lowercase(java.util.Locale.forLanguageTag("tr"))
                                val isChild = lower.contains("çocuk") || lower.contains("cocuk") ||
                                    lower.contains("evlat") || lower.contains("kız") || lower.contains("kiz") ||
                                    lower.contains("oğul") || lower.contains("ogul") || lower.contains("bebek") ||
                                    lower.contains("torun") || lower.contains("fert")
                                if (isChild) {
                                    childPrefs.edit()
                                        .putString("user_role", "FERT")
                                        .putBoolean("is_pool_member_joined", true)
                                        .putBoolean("is_explicit_reis", false)
                                        .apply()
                                }
                                person1Icon = editPersonIconInput
                                childPrefs.edit()
                                    .putString("p1_icon", editPersonIconInput)
                                    .putString("p1_name", name)
                                    .apply()
                                viewModel.updateMyProfileName(name)
                            }
                            2 -> {
                                person2Name = name
                                if (isPoolLeader) {
                                    person2Icon = editPersonIconInput
                                    childPrefs.edit().putString("p2_icon", editPersonIconInput).apply()
                                }
                                hasPerson2 = true
                                childPrefs.edit()
                                    .putString("p2_name", name)
                                    .putBoolean("has_p2", true)
                                    .putBoolean("has_p2_customized", true)
                                    .apply()
                            }
                            else -> {
                                val targetId = editingPersonIndex
                                if (!extraMemberIds.contains(targetId)) {
                                    val updated = (extraMemberIds + targetId).sorted()
                                    extraMemberIds = updated
                                    childPrefs.edit().putString("custom_extra_member_ids", updated.joinToString(",")).apply()
                                }
                                if (isPoolLeader) {
                                    childPrefs.edit().putString("p${targetId}_icon", editPersonIconInput).apply()
                                }
                                childPrefs.edit()
                                    .putString("p${targetId}_name", name)
                                    .putBoolean("has_p${targetId}", true)
                                    .apply()
                            }
                        }
                        refreshTrigger++
                        showEditPersonDialog = false
                        viewModel.showStatusMessage("$name bilgileri kaydedildi! 🌟")
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
                    Text("Aile Reisinden aldığınız Aile Kodunu girerek ortak havuza dahil olun:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinPoolCodeInput,
                        onValueChange = { joinPoolCodeInput = it.uppercase() },
                        label = { Text("Aile Kodu (örn: REIS-001234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu ailedeki rolünüz:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    val joinRoles = listOf(
                        "Oğlu" to "👦",
                        "Kızı" to "👧",
                        "Eşi" to "🧕",
                        "Annesi" to "🧕",
                        "Babası" to "🧔",
                        "Kardeşi" to "🧒"
                    )
                    joinRoles.chunked(3).forEach { rowRoles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowRoles.forEach { (r, ic) ->
                                val isSel = (joinPoolRoleInput == r)
                                Surface(
                                    onClick = { joinPoolRoleInput = r },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) EmeraldPrimary else EmeraldContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$ic $r",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = joinPoolCodeInput.trim()
                        if (raw.isNotBlank()) {
                            val canonical = com.example.util.PartnerSyncManager.formatDisplayCode(raw)
                            val normInput = com.example.util.PartnerSyncManager.normalizeCode(raw)
                            val normMine = com.example.util.PartnerSyncManager.normalizeCode(partnerInfo.myInviteCode)
                            if (normInput == normMine) {
                                viewModel.showStatusMessage("⚠️ Bu sizin kendi aile kodunuzdur. Başka bir aile ferdinizin kodunu girmelisiniz.")
                            } else {
                                familyPoolCode = canonical
                                val chosenRole = if (joinPoolRoleInput.isNotBlank()) joinPoolRoleInput else "Oğlu"
                                val normRole = com.example.util.FamilyRoleHelper.normalizeRoleTitle(chosenRole)
                                val roleIcon = com.example.util.FamilyRoleHelper.getDefaultIconForRole(normRole)
                                childPrefs.edit()
                                    .putString("family_pool_code", canonical)
                                    .putBoolean("is_pool_member_joined", true)
                                    .putBoolean("is_explicit_reis", false)
                                    .putBoolean("user_explicitly_disconnected", false)
                                    .putString("user_role", "FERT")
                                    .putString("p1_name", normRole)
                                    .putString("p1_icon", roleIcon)
                                    .apply()
                                person1Icon = roleIcon
                                viewModel.updateMyProfileName(normRole)
                                viewModel.matchPartnerWithCode(canonical)
                                refreshTrigger++
                                viewModel.showStatusMessage("$canonical kodlu Aile Ortak Havuzuna katılım sağlandı! 🟢")
                            }
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

    // HAVUZDAN AYRILMA / SIFIRLAMA DİALOGU (Tek Tıkla Kolay ve Kalıcı Ayrılma)
    if (showLeavePoolDialog || showResetHandshakeDialog) {
        AlertDialog(
            onDismissRequest = {
                showLeavePoolDialog = false
                showResetHandshakeDialog = false
            },
            title = {
                Text(
                    text = if (isPoolLeader) "Aile Havuzunu Kapat & Sıfırla" else "Aile Havuzundan Ayrıl",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isPoolLeader)
                        "Aile havuzunu kapatıp sıfırlamak istediğinize emin misiniz? Tüm bağlı üyelerin bağlantısı kesilecek ve havuzunuz sonlandırılacaktır."
                    else
                        "Aile havuzundan ayrılmak istediğinize emin misiniz? Havuz bağlantınız sonlandırılacak ve ortak tablodan çıkış yapılacaktır.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        childPrefs.edit()
                            .putBoolean("user_explicitly_disconnected", true)
                            .putBoolean("family_pool_created", false)
                            .putBoolean("family_pool_has_members", false)
                            .remove("is_pool_member_joined")
                            .remove("family_pool_code")
                            .remove("is_explicit_reis")
                            .remove("p2_name")
                            .remove("p2_icon")
                            .remove("has_p2")
                            .remove("has_p2_customized")
                            .putString("user_role", "FERT")
                            .apply()
                        familyPoolCode = ""
                        hasPerson2 = false
                        viewModel.leaveFamilyPool()
                        refreshTrigger++
                        showLeavePoolDialog = false
                        showResetHandshakeDialog = false
                        viewModel.showStatusMessage(
                            if (isPoolLeader) "Aile havuzu kapatıldı ve sıfırlandı. ⏹️"
                            else "Aile havuzundan başarıyla ayrıldınız. 🚪"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) {
                    Text("Evet, Ayrıl", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLeavePoolDialog = false
                    showResetHandshakeDialog = false
                }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // AİLE HAVUZU BAŞLATMA DİALOGU (AİLE REİSİ ROL SEÇİMİ)
    if (showStartPoolRoleDialog) {
        val leaderRoles = listOf(
            "Baba" to "🧔",
            "Anne" to "🧕",
            "Dede" to "🧓",
            "Aile Reisi" to "👑"
        )
        AlertDialog(
            onDismissRequest = { showStartPoolRoleDialog = false },
            title = { Text("Aile Havuzu Başlat (Aile Reisi)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Aile havuzunu oluşturan Aile Reisi olarak bu ailedeki unvanınızı seçiniz:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        leaderRoles.forEach { (role, icon) ->
                            val isSel = (leaderRoleInput == role)
                            Surface(
                                onClick = {
                                    leaderRoleInput = role
                                    leaderIconInput = icon
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else EmeraldContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = role,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normRole = com.example.util.FamilyRoleHelper.normalizeRoleTitle(leaderRoleInput)
                        val icon = leaderIconInput
                        person1Icon = icon
                        childPrefs.edit()
                            .putBoolean("is_explicit_reis", true)
                            .putBoolean("family_pool_created", true)
                            .putBoolean("family_pool_has_members", true)
                            .putBoolean("is_pool_member_joined", false)
                            .putBoolean("user_explicitly_disconnected", false)
                            .putString("user_role", "REIS")
                            .putString("p1_name", normRole)
                            .putString("p1_icon", icon)
                            .putString("family_pool_code", partnerInfo.myInviteCode)
                            .apply()
                        viewModel.updateMyProfileName(normRole)
                        refreshTrigger++
                        showStartPoolRoleDialog = false
                        viewModel.showStatusMessage("Aile havuzunuz $normRole olarak başlatıldı! 👑 Kodunuz: ${partnerInfo.myInviteCode}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Havuzu Başlat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPoolRoleDialog = false }) {
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

    // AİLEYE / KİŞİYE DUA VE TEBRİK GÖNDERME DİALOGU (User request 6)
    if (showSendDuaDialog) {
        val staticGreetings = com.example.data.util.DailyContentProvider.STATIC_GREETINGS_AND_DUAS
        AlertDialog(
            onDismissRequest = { showSendDuaDialog = false },
            containerColor = PureWhite,
            title = {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤲", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Dua ve Tebrik Gönder",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Muhatap: $duaTargetName",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hazır Manevi Dualar & Tebrikler:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    staticGreetings.forEach { item ->
                        val isSelected = (selectedDuaPreset == item.text && customDuaText.isBlank())
                        Surface(
                            onClick = {
                                selectedDuaPreset = item.text
                                customDuaText = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else SoftInfoCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.iconEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    Text(
                                        text = item.text,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Veya Kendi Duanızı Yazın:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = customDuaText,
                        onValueChange = { customDuaText = it },
                        placeholder = { Text("Örn: Allah kabul etsin, hayırlı ve bereketli olsun...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMessage = customDuaText.trim().ifBlank { selectedDuaPreset }
                        // 1. Bulut / Eşleşme üzerinden aile havuzuna ilet
                        viewModel.broadcastFamilyDua(duaTargetName, finalMessage)
                        // 2. Telefonda da yerel bildirim çal
                        com.example.util.NotificationHelper.showPartnerAlertNotification(
                            context = context,
                            partnerName = duaTargetName,
                            actionText = "$duaTargetName için dua: \"$finalMessage\" 🤲"
                        )
                        showSendDuaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Gönder 🤲", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendDuaDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
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

