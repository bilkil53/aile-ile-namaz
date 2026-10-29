package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import com.example.data.model.DailyPrayerTimes
import com.example.data.repository.PrayerRepository
import com.example.data.network.DiyanetPrayerService
import com.example.data.util.DailyContentProvider
import com.example.data.util.PrayerCalculator
import com.example.util.GpsLocationHelper
import com.example.util.NotificationHelper
import com.example.util.PartnerSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrayerRepository

    // Date state
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("d MMMM EEEE", Locale("tr"))

    private val _selectedDateStr = MutableStateFlow(dateFormat.format(Date()))
    val selectedDateStr: StateFlow<String> = _selectedDateStr.asStateFlow()

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())

    private val _displayDateTitle = MutableStateFlow("Bugün, " + displayFormat.format(Date()))
    val displayDateTitle: StateFlow<String> = _displayDateTitle.asStateFlow()

    private val _isViewingToday = MutableStateFlow(true)
    val isViewingToday: StateFlow<Boolean> = _isViewingToday.asStateFlow()

    // Database Flows
    val settings: StateFlow<AppSettingsEntity>
    val partnerInfo: StateFlow<PartnerInfoEntity>
    val kazaPrayers: StateFlow<List<KazaPrayerEntity>>
    val recentDailyPrayers: StateFlow<List<DailyPrayerEntity>>
    val pendingJoinRequests: StateFlow<List<com.example.util.IncomingJoinRequest>>

    // Today and Yesterday Prayer flows
    lateinit var yesterdayPrayer: StateFlow<DailyPrayerEntity>
    private val _todayPrayer = MutableStateFlow(DailyPrayerEntity(date = dateFormat.format(Date())))
    val todayPrayer: StateFlow<DailyPrayerEntity> = _todayPrayer.asStateFlow()

    // Calculated Prayer Times for display
    private val _prayerTimes = MutableStateFlow(PrayerCalculator.calculatePrayerTimes(41.0082, 28.9784))
    val prayerTimes: StateFlow<DailyPrayerTimes> = _prayerTimes.asStateFlow()

    // Qibla direction bearing (degrees)
    private val _qiblaBearing = MutableStateFlow(PrayerCalculator.calculateQiblaBearing(41.0082, 28.9784))
    val qiblaBearing: StateFlow<Double> = _qiblaBearing.asStateFlow()

    // Partner refresh animation flag
    private val _isRefreshingPartner = MutableStateFlow(false)
    val isRefreshingPartner: StateFlow<Boolean> = _isRefreshingPartner.asStateFlow()

    // Status snackbar / banner message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun showStatusMessage(msg: String) {
        viewModelScope.launch {
            _statusMessage.value = msg
            delay(2500)
            _statusMessage.value = null
        }
    }

    // GPS location loading flag
    private val _isUpdatingLocation = MutableStateFlow(false)
    val isUpdatingLocation: StateFlow<Boolean> = _isUpdatingLocation.asStateFlow()

    // Pending email invites received from friends
    data class EmailInvite(
        val email: String,
        val inviteCode: String,
        val senderName: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _pendingEmailInvites = MutableStateFlow<List<EmailInvite>>(emptyList())
    val pendingEmailInvites: StateFlow<List<EmailInvite>> = _pendingEmailInvites.asStateFlow()

    private val _notificationDayCounter = MutableStateFlow(1)
    val notificationDayCounter: StateFlow<Int> = _notificationDayCounter.asStateFlow()

    private var lastNotifiedPrayerName = ""

    // In-App Updater States
    private val _availableUpdate = MutableStateFlow<com.example.util.AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<com.example.util.AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _updateDownloadError = MutableStateFlow<String?>(null)
    val updateDownloadError: StateFlow<String?> = _updateDownloadError.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PrayerRepository(database.prayerDao(), application)

        NotificationHelper.createNotificationChannels(application)

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureUniqueInviteCode()
            repository.clearGuestDataIfNotLoggedIn()
            repository.sanitizeUserRolesIfEmptyPool()
        }

        settings = repository.getSettings()
            .map { it ?: AppSettingsEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppSettingsEntity()
            )

        partnerInfo = repository.getPartnerInfo()
            .map { it ?: PartnerInfoEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PartnerInfoEntity()
            )

        kazaPrayers = repository.getAllKazaPrayers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        recentDailyPrayers = repository.getRecentDailyPrayers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        pendingJoinRequests = repository.pendingJoinRequests.asStateFlow()

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDateStr = dateFormat.format(yesterdayCal.time)
        yesterdayPrayer = repository.getPrayerForDate(yesterdayDateStr)
            .map { it ?: DailyPrayerEntity(date = yesterdayDateStr) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DailyPrayerEntity(date = yesterdayDateStr)
            )

        loadPrayerForCurrentDate()
        recalculateTimes()

        // Sync settings to SharedPreferences for background AlarmManager execution
        viewModelScope.launch {
            settings.collect { s ->
                val prefs = application.getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putFloat("latitude", s.latitude.toFloat())
                    putFloat("longitude", s.longitude.toFloat())
                    putBoolean("prayerRemindersEnabled", true)
                    putBoolean("motivationNotificationEnabled", true)
                    putString("motivationNotificationTime", "12:00")
                    apply()
                }
                com.example.util.PrayerAlarmScheduler.scheduleAllPrayerAlarms(application)
            }
        }

        // Timer loop for remaining prayer time updates
        viewModelScope.launch {
            while (true) {
                delay(15000)
                recalculateTimes()
            }
        }

        // Periodic partner cloud sync loop (every 5 seconds for real-time status and spouse notifications)
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            while (true) {
                try {
                    repository.refreshPartnerData()
                } catch (_: Exception) { }
                delay(5000)
            }
        }

        // Automatic GitHub Release Update Check:
        // Her daim kesintisiz sorgulama: Açılışta 1.5 sn sonra hemen kontrol eder, ardından uygulama açıkken her 45 dakikada bir otomatik yeniler
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            checkForAppUpdate(silent = true)
            while (true) {
                delay(45 * 60 * 1000L) // 45 dakikada bir periyodik kontrol
                checkForAppUpdate(silent = true)
            }
        }
    }

    private var lastNotifiedPrayerKey: String = ""

    private fun checkAndNotifyApproachingPrayer() {
        val times = _prayerTimes.value ?: return
        if (!settings.value.prayerRemindersEnabled) return

        val nextPrayerName = times.nextPrayerName
        val remainingMillis = times.nextPrayerRemainingMillis
        val remainingMinutes = remainingMillis / (60 * 1000L)

        val isStandardPrayer = listOf("imsak", "sabah", "cuma", "öğle", "ogle", "ikindi", "akşam", "aksam", "yatsı", "yatsi")
            .any { nextPrayerName.lowercase().contains(it) }

        if (!isStandardPrayer) return

        val todayDateStr = dateFormat.format(Date())
        val notificationKey = "$todayDateStr-$nextPrayerName-10min"

        if (remainingMillis in 1000L..(10 * 60 * 1000L) && lastNotifiedPrayerKey != notificationKey) {
            lastNotifiedPrayerKey = notificationKey
            NotificationHelper.showPrayerApproachingNotification(
                context = getApplication(),
                prayerName = nextPrayerName,
                minutesRemaining = remainingMinutes
            )
        }
    }

    fun testPrayerNotification() {
        val cal = java.util.Calendar.getInstance()
        val isFriday = cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        val rawNext = _prayerTimes.value?.nextPrayerName?.ifBlank { null }
        val prayerName = rawNext ?: if (isFriday) "Cuma Namazı" else "Öğle"
        NotificationHelper.showPrayerApproachingNotification(
            context = getApplication(),
            prayerName = prayerName,
            minutesRemaining = 10
        )
        viewModelScope.launch {
            val isCuma = prayerName.contains("cuma", ignoreCase = true) || (isFriday && (prayerName.contains("öğle", ignoreCase = true) || prayerName.contains("ogle", ignoreCase = true)))
            val label = if (isCuma) {
                "🕌 Cuma Namazı 10 dk öncesi Diyanet Cuma Sûresi & salavat bildirimi örneklendi!"
            } else {
                "🕌 10 dk öncesi Diyanet vakit & hadis bildirimi örneklendi!"
            }
            _statusMessage.value = label
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun getDisplayDateTitle(): String {
        val todayStr = dateFormat.format(Date())
        val selected = _selectedDateStr.value
        return if (selected == todayStr) {
            "Bugün, " + displayFormat.format(_currentCalendar.value.time)
        } else {
            displayFormat.format(_currentCalendar.value.time)
        }
    }

    fun isToday(): Boolean {
        return _selectedDateStr.value == dateFormat.format(Date())
    }

    fun changeDateOffset(offsetDays: Int) {
        val newCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        _currentCalendar.value = newCal
        _selectedDateStr.value = dateFormat.format(newCal.time)
        _displayDateTitle.value = getDisplayDateTitle()
        _isViewingToday.value = isToday()
        loadPrayerForCurrentDate()
        recalculateTimes()
    }

    fun resetToToday() {
        _currentCalendar.value = Calendar.getInstance()
        _selectedDateStr.value = dateFormat.format(Date())
        _displayDateTitle.value = getDisplayDateTitle()
        _isViewingToday.value = true
        loadPrayerForCurrentDate()
        recalculateTimes()
    }

    fun resetAllPrayerData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPrayerRecords()
            resetToToday()
            _statusMessage.value = "Namaz kayıtları sıfırlandı. Temiz şekilde yeniden girebilirsiniz."
            delay(3000)
            _statusMessage.value = null
        }
    }

    private var dateJob: Job? = null

    private fun loadPrayerForCurrentDate() {
        val dateStr = _selectedDateStr.value
        dateJob?.cancel()
        dateJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getPrayerForDate(dateStr).collectLatest { prayer ->
                _todayPrayer.value = prayer ?: DailyPrayerEntity(date = dateStr)
            }
        }
    }

    private val _showLoginRequiredDialog = MutableStateFlow(false)
    val showLoginRequiredDialog: StateFlow<Boolean> = _showLoginRequiredDialog.asStateFlow()

    fun dismissLoginRequiredDialog() {
        _showLoginRequiredDialog.value = false
    }

    fun promptLoginRequired(customMsg: String? = null) {
        _showLoginRequiredDialog.value = true
        showStatusMessage(customMsg ?: "İbadet kayıtlarınızın saklanabilmesi için lütfen Google ile giriş yapın 🔒")
    }

    fun requireLoginPrompt(): Boolean {
        if (!settings.value.isLoggedIn || settings.value.userEmail.isBlank()) {
            promptLoginRequired()
            return true
        }
        return false
    }

    fun setPrayerStatus(prayerType: String, status: String) {
        if (requireLoginPrompt()) return
        val dateStr = _selectedDateStr.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPrayerStatus(dateStr, prayerType, status)
            if (status == "PRAYED") {
                val trName = when (prayerType.uppercase()) {
                    "FAJR" -> "Sabah"
                    "DHUHR" -> "Öğle"
                    "ASR" -> "İkindi"
                    "MAGHRIB" -> "Akşam"
                    "ISHA" -> "Yatsı"
                    else -> prayerType
                }
                if (partnerInfo.value.isMatched) {
                    _statusMessage.value = "$trName namazı kaydedildi ve aile fertlerinize bildirildi! 🤲"
                } else {
                    _statusMessage.value = "$trName namazı kılındı olarak kaydedildi! Allah kabul etsin."
                }
                delay(2500)
                _statusMessage.value = null
            }
        }
    }

    fun setPrayerStatusForDate(dateStr: String, prayerType: String, status: String) {
        if (requireLoginPrompt()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPrayerStatus(dateStr, prayerType, status)
            if (status == "PRAYED") {
                val trName = when (prayerType.uppercase()) {
                    "FAJR" -> "Sabah"
                    "DHUHR" -> "Öğle"
                    "ASR" -> "İkindi"
                    "MAGHRIB" -> "Akşam"
                    "ISHA" -> "Yatsı"
                    else -> prayerType
                }
                _statusMessage.value = "Dünkü $trName namazı kılındı olarak kaydedildi! 🤲"
                delay(2000)
                _statusMessage.value = null
            }
        }
    }

    // Kaza actions
    fun adjustKaza(type: String, delta: Int) {
        adjustKazaOwed(type, delta)
    }

    fun adjustKazaOwed(type: String, delta: Int) {
        if (requireLoginPrompt()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.adjustKazaOwed(type, delta)
        }
    }

    private val _kazaUndoMessage = MutableStateFlow<String?>(null)
    val kazaUndoMessage: StateFlow<String?> = _kazaUndoMessage.asStateFlow()
    private var lastCompletedKazaType: String? = null
    private var kazaUndoJob: Job? = null

    fun completeOneKaza(type: String) {
        if (requireLoginPrompt()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeOneKaza(type)
            lastCompletedKazaType = type
            val trName = when (type.uppercase()) {
                "FAJR" -> "Sabah"
                "DHUHR" -> "Öğle"
                "ASR" -> "İkindi"
                "MAGHRIB" -> "Akşam"
                "ISHA" -> "Yatsı"
                "WITR" -> "Vitir"
                else -> type
            }
            kazaUndoJob?.cancel()
            _kazaUndoMessage.value = "1 adet $trName kazası kılındı."
            kazaUndoJob = viewModelScope.launch {
                delay(6000)
                _kazaUndoMessage.value = null
                lastCompletedKazaType = null
            }
        }
    }

    fun undoLastKaza() {
        if (requireLoginPrompt()) return
        val type = lastCompletedKazaType ?: return
        lastCompletedKazaType = null
        kazaUndoJob?.cancel()
        _kazaUndoMessage.value = null
        val trName = when (type.uppercase()) {
            "FAJR" -> "Sabah"
            "DHUHR" -> "Öğle"
            "ASR" -> "İkindi"
            "MAGHRIB" -> "Akşam"
            "ISHA" -> "Yatsı"
            "WITR" -> "Vitir"
            else -> type
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.undoCompleteOneKaza(type)
            _statusMessage.value = "$trName kazası geri alındı (+1 borç iade edildi)."
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun dismissKazaUndo() {
        kazaUndoJob?.cancel()
        _kazaUndoMessage.value = null
        lastCompletedKazaType = null
    }

    fun addBulkKaza(amount: Int) {
        if (requireLoginPrompt()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBulkKaza(amount)
            _statusMessage.value = "Her vakte +$amount kaza namazı borcu eklendi."
            delay(3000)
            _statusMessage.value = null
        }
    }

    // Partner actions
    fun refreshPartnerData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingPartner.value = true
            val updated = repository.refreshPartnerData()
            _isRefreshingPartner.value = false
            if (updated.isMatched) {
                _statusMessage.value = "${updated.partnerDisplayName} ile namaz durumları güncellendi!"
            } else {
                _statusMessage.value = "Davet kodu henüz eşleşmedi. Davet edilen kişinin kodu girmesi bekleniyor..."
            }
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun ensureUniqueInviteCode() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureUniqueInviteCode()
        }
    }

    fun matchPartnerWithCode(code: String, partnerName: String = "", partnerEmail: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingPartner.value = true
            val canonical = PartnerSyncManager.formatDisplayCode(code)
            val success = repository.matchPartner(canonical, partnerName, partnerEmail)
            _isRefreshingPartner.value = false
            if (success) {
                val current = partnerInfo.value
                val name = if (current.partnerDisplayName.isNotBlank()) current.partnerDisplayName else "Aile ferdiniz"
                _statusMessage.value = "$name ile başarıyla eşleştiniz! 🟢"
            } else {
                val myClean = PartnerSyncManager.normalizeCode(partnerInfo.value.myInviteCode)
                val targetClean = PartnerSyncManager.normalizeCode(code)
                if (targetClean.isNotBlank() && targetClean == myClean) {
                    _statusMessage.value = "⚠️ Kendi aile kodunuzu girdiniz. Başka bir aile ferdinizin kodunu girmelisiniz."
                } else {
                    _statusMessage.value = "Davet kodu doğrulanamadı. Lütfen kodu kontrol edin."
                }
            }
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun togglePrayerReminders(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(prayerRemindersEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun togglePartnerAlerts(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(partnerAlertsEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun toggleMotivationNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(motivationNotificationEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun updateMotivationNotificationTime(timeStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(motivationNotificationTime = timeStr)
            repository.saveSettings(updated)
            _statusMessage.value = "Günlük dua & söz saati $timeStr olarak ayarlandı ⏰"
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun broadcastFamilyDua(targetName: String, duaText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.sendSpiritualDua(targetName, duaText)
            } catch (_: Exception) { }
            _statusMessage.value = "$targetName için dua kaydedildi ve iletildi: Allah kabul etsin! 🤲"
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun leaveFamilyPool(poolCode: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.leaveFamilyPool(poolCode)
            _statusMessage.value = "Aile havuzundan ayrıldınız."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun unmatchPartner() {
        leaveFamilyPool()
    }

    fun updatePartnerName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePartnerName(name)
            _statusMessage.value = "Kişi ismi güncellendi: $name"
            delay(2500)
            _statusMessage.value = null
        }
    }

    // User Login / Profile
    fun loginUser(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loginUser(name, email)
            _statusMessage.value = "Hoş geldiniz, $name! Profiliniz güncellendi."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun loginWithGoogleBrowser(email: String = "", name: String = "") {
        if (email.isBlank()) return
        val cleanEmail = email.trim()
        val emailKey = cleanEmail.lowercase()
        val childPrefs = getApplication<Application>().getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE)
        val savedNick = childPrefs.getString("user_nickname_$emailKey", null)
        val cleanName = if (!savedNick.isNullOrBlank() && !savedNick.contains("@") && savedNick != "Mümin") {
            savedNick.trim()
        } else if (name.isNotBlank() && !name.contains("@") && name != "Mümin") {
            name.trim()
        } else {
            ""
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.loginUser(cleanName, cleanEmail)
            _statusMessage.value = "Google girişi başarılı ✓ Lütfen takma adınızı belirleyin."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun updateMyProfileName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMyProfileName(newName)
            _statusMessage.value = "Profil isminiz güncellendi: $newName"
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun updatePartnerProfileName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePartnerName(newName)
        }
    }

    fun requestJoinFamilyPool(codeOrEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.requestJoinFamilyPool(codeOrEmail)
            if (success) {
                _statusMessage.value = "Katılım talebiniz Aile Reisine iletildi. Onay bekleniyor... ⏳"
            } else {
                _statusMessage.value = "Geçersiz aile kodu veya e-posta adresi."
            }
        }
    }

    fun approveJoinRequest(
        request: com.example.util.IncomingJoinRequest,
        leaderRole: String,
        leaderIcon: String,
        memberRole: String,
        memberIcon: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.approveJoinRequest(request, leaderRole, leaderIcon, memberRole, memberIcon)
            if (success) {
                _statusMessage.value = "${request.claimerName} aile havuzunuza katıldı! 🟢"
            }
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun rejectJoinRequest(request: com.example.util.IncomingJoinRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.rejectJoinRequest(request)
            _statusMessage.value = "${request.claimerName} katılım talebi reddedildi."
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun cancelJoinRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cancelJoinRequest()
            _statusMessage.value = "Katılım talebi iptal edildi."
        }
    }

    fun logoutUser() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logoutUser()
            _statusMessage.value = "Çıkış yapıldı."
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun backupNowToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            if (!current.isLoggedIn || current.userEmail.isBlank()) {
                _statusMessage.value = "Yedekleme için lütfen önce Google ile giriş yapın."
            } else {
                repository.triggerAutoCloudBackup()
                _statusMessage.value = "Tüm namaz ve kaza kayıtlarınız ${current.userEmail} hesabına başarıyla yedeklendi ☁️"
            }
            delay(3000)
            _statusMessage.value = null
        }
    }

    // City & Location
    fun selectCity(city: PrayerCalculator.CityInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(
                cityName = city.name,
                latitude = city.lat,
                longitude = city.lng
            )
            repository.saveSettings(updated)
            recalculateTimes(city.lat, city.lng)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "${city.name} seçildi • Diyanet vakitleri güncellendi 🕌"
            }
            delay(3000)
            withContext(Dispatchers.Main) {
                _statusMessage.value = null
            }
        }
    }

    fun acceptEmailInvite(invite: EmailInvite) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.matchPartner(
                invite.inviteCode,
                invite.senderName.ifBlank { invite.email.substringBefore("@") },
                invite.email
            )
            _pendingEmailInvites.value = _pendingEmailInvites.value.filter { it.inviteCode != invite.inviteCode }
            _statusMessage.value = "${invite.email} ile başarıyla eşleştiniz!"
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun updateLocationGps() {
        viewModelScope.launch {
            _isUpdatingLocation.value = true
            val loc = GpsLocationHelper.getDeviceLocation(
                context = getApplication(),
                fallbackCity = settings.value.cityName,
                fallbackLat = settings.value.latitude,
                fallbackLng = settings.value.longitude
            )
            _isUpdatingLocation.value = false

            if (loc.isSuccess) {
                val updated = settings.value.copy(
                    cityName = loc.cityName,
                    latitude = loc.latitude,
                    longitude = loc.longitude
                )
                repository.saveSettings(updated)
                fetchOrCalculatePrayerTimes(loc.latitude, loc.longitude)
                _statusMessage.value = "Diyanet vakitleri GPS ile güncellendi: ${loc.cityName} 📍"
            } else {
                _statusMessage.value = "GPS sinyali alınamadı. Mevcut şehir (${settings.value.cityName}) korundu. Şehrinizi listeden seçebilirsiniz."
            }
            delay(3500)
            _statusMessage.value = null
        }
    }

    private fun recalculateTimes(
        lat: Double = settings.value.latitude,
        lng: Double = settings.value.longitude
    ) {
        viewModelScope.launch {
            fetchOrCalculatePrayerTimes(lat, lng)
        }
    }

    private suspend fun fetchOrCalculatePrayerTimes(lat: Double, lng: Double) {
        val diyanetTimes = DiyanetPrayerService.fetchDiyanetPrayerTimes(
            latitude = lat,
            longitude = lng,
            calendar = _currentCalendar.value
        )
        val prefs = getApplication<Application>().getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)
        if (diyanetTimes != null) {
            _prayerTimes.value = diyanetTimes
            prefs.edit().apply {
                putString("cached_diyanet_imsak", diyanetTimes.imsak)
                putString("cached_diyanet_gunes", diyanetTimes.gunes)
                putString("cached_diyanet_ogle", diyanetTimes.ogle)
                putString("cached_diyanet_ikindi", diyanetTimes.ikindi)
                putString("cached_diyanet_aksam", diyanetTimes.aksam)
                putString("cached_diyanet_yatsi", diyanetTimes.yatsi)
                putString("cached_diyanet_date", dateFormat.format(Date()))
                putFloat("latitude", lat.toFloat())
                putFloat("longitude", lng.toFloat())
                apply()
            }
        } else {
            _prayerTimes.value = PrayerCalculator.calculatePrayerTimes(lat, lng, _currentCalendar.value)
        }
        _qiblaBearing.value = PrayerCalculator.calculateQiblaBearing(lat, lng)
        checkAndNotifyApproachingPrayer()
        com.example.util.PrayerAlarmScheduler.scheduleAllPrayerAlarms(getApplication())
    }

    fun updateDriveShareUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(driveShareUrl = url.trim())
            repository.saveSettings(updated)
            _statusMessage.value = "Google Drive indirme linkiniz kaydedildi!"
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun saveSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(newSettings)
        }
    }

    // App Updater methods
    fun checkForAppUpdate(silent: Boolean = false, forceShow: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!silent) _isCheckingForUpdate.value = true
            val update = com.example.util.AppUpdateManager.checkForUpdates(
                context = getApplication(),
                notifyIfFound = !silent
            )
            if (!silent) _isCheckingForUpdate.value = false

            if (update != null) {
                val prefs = getApplication<Application>().getSharedPreferences("app_updater_prefs", android.content.Context.MODE_PRIVATE)
                val lastDismissedTime = prefs.getLong("last_dismissed_time", 0L)
                val dismissedTag = prefs.getString("dismissed_update_tag", "")
                val hoursSinceDismiss = (System.currentTimeMillis() - lastDismissedTime) / (1000 * 60 * 60)

                if (update.hasUpdate) {
                    if (forceShow || !silent || dismissedTag != update.latestVersionTag || hoursSinceDismiss >= 2) {
                        _availableUpdate.value = update
                    }
                    if (!silent) {
                        _statusMessage.value = "Yeni güncelleme bulundu: ${update.latestVersionTag} 📲"
                        delay(3000)
                        _statusMessage.value = null
                    }
                } else {
                    // Uygulama güncel (v1.2 == v1.2) - Güncelleme kartını ve diyaloğunu kesinlikle sıfırla
                    _availableUpdate.value = null
                    if (!silent) {
                        _statusMessage.value = "Uygulamanız en güncel sürümde! (${com.example.BuildConfig.VERSION_NAME})"
                        delay(3000)
                        _statusMessage.value = null
                    }
                }
            } else if (!silent) {
                _statusMessage.value = "Güncelleme sunucusuna ulaşılamadı. Lütfen bağlantınızı kontrol edin."
                delay(3000)
                _statusMessage.value = null
            }
        }
    }

    fun startApkUpdateDownload() {
        val update = _availableUpdate.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _updateDownloadError.value = null
            _downloadProgress.value = 0.01f

            com.example.util.AppUpdateManager.downloadAndInstallApk(
                context = getApplication(),
                downloadUrl = update.apkDownloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                },
                onComplete = {
                    _downloadProgress.value = null
                },
                onError = { error ->
                    _downloadProgress.value = null
                    _updateDownloadError.value = error
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        val currentTag = _availableUpdate.value?.latestVersionTag
        if (!currentTag.isNullOrBlank()) {
            val prefs = getApplication<Application>().getSharedPreferences("app_updater_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .putString("dismissed_update_tag", currentTag)
                .putLong("last_dismissed_time", System.currentTimeMillis())
                .apply()
        }
        _availableUpdate.value = null
        _downloadProgress.value = null
        _updateDownloadError.value = null
    }

    // =========================================================================
    // VERİ AKTARIMI (Yedekleme ve Geri Yükleme)
    // =========================================================================
    fun exportDataBackup(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportDataToJson()
            withContext(Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun importDataBackup(jsonStr: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.importDataFromJson(jsonStr)
            withContext(Dispatchers.Main) {
                if (success) {
                    loadPrayerForCurrentDate()
                    _statusMessage.value = "Veriler başarıyla Aile ile Namaz uygulamasına aktarıldı! 🤲"
                    onSuccess()
                } else {
                    _statusMessage.value = "Yedek verisi okunamadı. Lütfen geçerli bir yedek metni girin."
                }
            }
        }
    }
}
