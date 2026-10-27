package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.PrayerDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import com.example.util.FamilyRoleHelper
import com.example.util.NotificationHelper
import com.example.util.PartnerSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PrayerRepository(
    private val prayerDao: PrayerDao,
    private val context: Context? = null
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingPrayerEventJobs = ConcurrentHashMap<String, Job>()
    private val processedEventSignatures = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private var lastProcessedEventTime: Long = System.currentTimeMillis() - 60_000L

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun ensureUniqueInviteCode(): PartnerInfoEntity {
        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        // Ensure code starts with REIS-00 prefix
        if (current.myInviteCode.isBlank() || !current.myInviteCode.startsWith("REIS-00")) {
            val randomSuffix = (1000..9999).random()
            val newCode = "REIS-00$randomSuffix"
            val updated = current.copy(myInviteCode = newCode)
            prayerDao.savePartnerInfo(updated)
            return updated
        }
        return current
    }

    fun getPrayerForDate(date: String): Flow<DailyPrayerEntity?> {
        return prayerDao.getPrayerForDate(date)
    }

    fun getRecentDailyPrayers(): Flow<List<DailyPrayerEntity>> {
        return prayerDao.getRecentDailyPrayers()
    }

    suspend fun setPrayerStatus(date: String, prayerType: String, status: String) {
        val rowsAffected = when (prayerType.uppercase()) {
            "FAJR", "SABAH" -> prayerDao.updateFajrStatus(date, status)
            "DHUHR", "OGLE", "ÖĞLE" -> prayerDao.updateDhuhrStatus(date, status)
            "ASR", "IKINDI", "İKİNDİ" -> prayerDao.updateAsrStatus(date, status)
            "MAGHRIB", "AKSAM", "AKŞAM" -> prayerDao.updateMaghribStatus(date, status)
            "ISHA", "YATSI" -> prayerDao.updateIshaStatus(date, status)
            else -> 0
        }

        val updated = if (rowsAffected == 0) {
            val newEntity = when (prayerType.uppercase()) {
                "FAJR", "SABAH" -> DailyPrayerEntity(date = date, fajrStatus = status)
                "DHUHR", "OGLE", "ÖĞLE" -> DailyPrayerEntity(date = date, dhuhrStatus = status)
                "ASR", "IKINDI", "İKİNDİ" -> DailyPrayerEntity(date = date, asrStatus = status)
                "MAGHRIB", "AKSAM", "AKŞAM" -> DailyPrayerEntity(date = date, maghribStatus = status)
                "ISHA", "YATSI" -> DailyPrayerEntity(date = date, ishaStatus = status)
                else -> DailyPrayerEntity(date = date)
            }
            prayerDao.insertOrUpdateDailyPrayer(newEntity)
            newEntity
        } else {
            prayerDao.getPrayerForDateDirect(date) ?: DailyPrayerEntity(date = date)
        }

        // Broadcast to partner if matched
        broadcastCurrentStatusToPartner(todayDate = date, dailyPrayer = updated)

        // Otomatik Bulut Yedekleme (Kullanıcı giriş yapmışsa anında buluta yedekle)
        triggerAutoCloudBackup()

        // Cancel previous pending notification job for this prayer type
        // This prevents double notifications if user taps through or changes their mind (e.g. kıldı then kılınmadı)
        val prayerKey = prayerType.uppercase()
        pendingPrayerEventJobs[prayerKey]?.cancel()

        val trName = when (prayerKey) {
            "FAJR", "SABAH" -> "Sabah"
            "DHUHR", "OGLE", "ÖĞLE" -> "Öğle"
            "ASR", "IKINDI", "İKİNDİ" -> "İkindi"
            "MAGHRIB", "AKSAM", "AKŞAM" -> "Akşam"
            "ISHA", "YATSI" -> "Yatsı"
            else -> prayerType
        }

        // Send prayer notification to partner after a 1.2-second delay (debounce)
        if (status == "PRAYED" || status == "MISSED") {
            pendingPrayerEventJobs[prayerKey] = repositoryScope.launch {
                try {
                    delay(1200L) // Hızlı ve güvenilir teyit (1.2 saniye)

                    // Verify latest status from database
                    val latest = prayerDao.getPrayerForDate(date).firstOrNull() ?: return@launch
                    val currentStatus = when (prayerKey) {
                        "FAJR", "SABAH" -> latest.fajrStatus
                        "DHUHR", "OGLE", "ÖĞLE" -> latest.dhuhrStatus
                        "ASR", "IKINDI", "İKİNDİ" -> latest.asrStatus
                        "MAGHRIB", "AKSAM", "AKŞAM" -> latest.maghribStatus
                        "ISHA", "YATSI" -> latest.ishaStatus
                        else -> status
                    }

                    val partner = prayerDao.getPartnerInfo().firstOrNull()
                    if (partner != null && partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
                        val spouseName = partner.myDisplayName.ifBlank { "Aile Ferdi" }
                        if (currentStatus == "PRAYED") {
                            PartnerSyncManager.broadcastPartnerEvent(
                                codeA = partner.myInviteCode,
                                codeB = partner.partnerInviteCode,
                                myCode = partner.myInviteCode,
                                myName = spouseName,
                                eventType = "PRAYED",
                                prayerName = trName,
                                actionText = "$trName namazını kıldı. Allah kabul etsin! 🤲"
                            )
                        } else if (currentStatus == "MISSED") {
                            PartnerSyncManager.broadcastPartnerEvent(
                                codeA = partner.myInviteCode,
                                codeB = partner.partnerInviteCode,
                                myCode = partner.myInviteCode,
                                myName = spouseName,
                                eventType = "MISSED",
                                prayerName = trName,
                                actionText = "$trName namazını kılmadı."
                            )
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Kaza
    fun getAllKazaPrayers(): Flow<List<KazaPrayerEntity>> {
        return prayerDao.getAllKazaPrayers()
    }

    suspend fun adjustKazaOwed(type: String, delta: Int) {
        prayerDao.adjustKazaOwed(type.uppercase(), delta)
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()
    }

    suspend fun completeOneKaza(type: String) {
        prayerDao.completeOneKaza(type.uppercase())
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()

        val partner = prayerDao.getPartnerInfo().firstOrNull()
        if (partner != null && partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
            val trName = when (type.uppercase()) {
                "FAJR", "SABAH" -> "Sabah"
                "DHUHR", "OGLE", "ÖĞLE" -> "Öğle"
                "ASR", "IKINDI", "İKİNDİ" -> "İkindi"
                "MAGHRIB", "AKSAM", "AKŞAM" -> "Akşam"
                "ISHA", "YATSI" -> "Yatsı"
                "WITR", "VITIR" -> "Vitir"
                else -> type
            }
            PartnerSyncManager.broadcastPartnerEvent(
                codeA = partner.myInviteCode,
                codeB = partner.partnerInviteCode,
                myCode = partner.myInviteCode,
                myName = partner.myDisplayName.ifBlank { "Aile Ferdi" },
                eventType = "KAZA",
                prayerName = trName,
                actionText = "1 vakit $trName kaza namazı kıldı. Tebrikler! 🤲"
            )
            val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
            val poolCode = childPrefs?.getString("family_pool_code", null) ?: partner.partnerInviteCode.ifBlank { partner.myInviteCode }
            if (poolCode.isNotBlank()) {
                PartnerSyncManager.broadcastPoolEvent(
                    poolCode = poolCode,
                    myCode = partner.myInviteCode,
                    myName = partner.myDisplayName.ifBlank { "Aile Ferdi" },
                    eventType = "KAZA",
                    prayerName = trName,
                    actionText = "1 vakit $trName kaza namazı kıldı. Tebrikler! 🤲"
                )
            }
        }
    }

    suspend fun sendSpiritualDua(targetName: String, messageText: String) {
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: return
        val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
        val poolCode = childPrefs?.getString("family_pool_code", null) ?: partner.partnerInviteCode.ifBlank { partner.myInviteCode }
        val myName = partner.myDisplayName.ifBlank { "Aile Ferdi" }
        val actionText = "$targetName için dua: \"$messageText\" 🤲"

        if (partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
            PartnerSyncManager.broadcastPartnerEvent(
                codeA = partner.myInviteCode,
                codeB = partner.partnerInviteCode,
                myCode = partner.myInviteCode,
                myName = myName,
                eventType = "DUA",
                prayerName = "Dua ve Tebrik",
                actionText = actionText
            )
        }
        if (poolCode.isNotBlank()) {
            PartnerSyncManager.broadcastPoolEvent(
                poolCode = poolCode,
                myCode = partner.myInviteCode,
                myName = myName,
                eventType = "DUA",
                prayerName = "Dua ve Tebrik",
                actionText = actionText
            )
        }
        prayerDao.updatePartnerReceivedDua("Sizden $targetName için dua: \"$messageText\"")
    }

    suspend fun undoCompleteOneKaza(type: String) {
        prayerDao.undoCompleteOneKaza(type.uppercase())
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()
    }

    suspend fun addBulkKaza(amount: Int) {
        prayerDao.addBulkKaza(amount)
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()
    }

    // Partner
    fun getPartnerInfo(): Flow<PartnerInfoEntity?> {
        return prayerDao.getPartnerInfo()
    }

    private suspend fun broadcastCurrentStatusToPartner(
        todayDate: String = getTodayDateString(),
        dailyPrayer: DailyPrayerEntity? = null
    ) {
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: return
        if (partner.myInviteCode.isBlank() && partner.partnerInviteCode.isBlank()) return

        val prayers = dailyPrayer ?: prayerDao.getPrayerForDate(todayDate).firstOrNull() ?: DailyPrayerEntity(date = todayDate)
        val kazas = prayerDao.getAllKazaPrayers().firstOrNull() ?: emptyList()
        val totalCompleted = kazas.sumOf { it.completedCount }
        val totalOwed = kazas.sumOf { it.owedCount }

        if (partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
            PartnerSyncManager.broadcastPrayerStatus(
                codeA = partner.myInviteCode,
                codeB = partner.partnerInviteCode,
                myCode = partner.myInviteCode,
                myName = partner.myDisplayName.ifBlank { "Aile Ferdi" },
                myEmail = partner.myEmail,
                date = todayDate,
                fajr = prayers.fajrStatus,
                dhuhr = prayers.dhuhrStatus,
                asr = prayers.asrStatus,
                maghrib = prayers.maghribStatus,
                isha = prayers.ishaStatus,
                kazaCompleted = totalCompleted,
                kazaOwed = totalOwed
            )
        }

        val poolCode = if (partner.partnerInviteCode.isNotBlank()) partner.partnerInviteCode else partner.myInviteCode
        if (poolCode.isNotBlank()) {
            PartnerSyncManager.broadcastPrayerStatusToPool(
                poolCode = poolCode,
                myCode = partner.myInviteCode,
                myName = partner.myDisplayName.ifBlank { "Aile Ferdi" },
                myEmail = partner.myEmail,
                date = todayDate,
                fajr = prayers.fajrStatus,
                dhuhr = prayers.dhuhrStatus,
                asr = prayers.asrStatus,
                maghrib = prayers.maghribStatus,
                isha = prayers.ishaStatus,
                kazaCompleted = totalCompleted,
                kazaOwed = totalOwed
            )
        }
    }

    suspend fun refreshPartnerData(): PartnerInfoEntity {
        var current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        if (current.myInviteCode.isBlank() || !current.myInviteCode.startsWith("REIS-00")) {
            current = ensureUniqueInviteCode()
        }

        val today = getTodayDateString()
        val lastSyncDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(current.lastSyncTime))
        val isSameDay = (lastSyncDay == today)

        // If today is a new day and partner hasn't posted today's prayers yet, shift yesterday's prayers
        if (!isSameDay && current.partnerFajr != "NONE") {
            current = current.copy(
                partnerYesterdayFajr = current.partnerFajr,
                partnerYesterdayDhuhr = current.partnerDhuhr,
                partnerYesterdayAsr = current.partnerAsr,
                partnerYesterdayMaghrib = current.partnerMaghrib,
                partnerYesterdayIsha = current.partnerIsha,
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE"
            )
            prayerDao.savePartnerInfo(current)
        }

        val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
        // 1. If NOT matched yet: check if someone has claimed my invite code!
        if (!current.isMatched) {
            // Re-publish my profile so spouse can see my name/email/icon upon entering code
            val myIcon = childPrefs?.getString("p1_icon", "🧔") ?: "🧔"
            PartnerSyncManager.publishMyProfile(current.myInviteCode, current.myDisplayName, current.myEmail, myIcon)

            val incomingMatch = PartnerSyncManager.checkForIncomingMatch(current.myInviteCode)
            if (incomingMatch != null && incomingMatch.partnerCode.isNotBlank()) {
                val normPartnerName = FamilyRoleHelper.normalizeRoleTitle(incomingMatch.partnerName.ifBlank { "Aile Ferdi" })
                val roleIcon = if (incomingMatch.partnerIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(incomingMatch.partnerIcon, normPartnerName)) {
                    incomingMatch.partnerIcon
                } else {
                    FamilyRoleHelper.getDefaultIconForRole(normPartnerName)
                }
                childPrefs?.edit()
                    ?.putBoolean("user_explicitly_disconnected", false)
                    ?.putString("p2_name", normPartnerName)
                    ?.putString("p2_icon", roleIcon)
                    ?.apply()

                val matched = current.copy(
                    isMatched = true,
                    partnerDisplayName = normPartnerName,
                    partnerEmail = incomingMatch.partnerEmail,
                    partnerInviteCode = PartnerSyncManager.formatDisplayCode(incomingMatch.partnerCode),
                    lastSyncTime = System.currentTimeMillis()
                )
                prayerDao.savePartnerInfo(matched)

                // Show notification that match happened!
                if (context != null) {
                    NotificationHelper.showPartnerAlertNotification(
                        context,
                        normPartnerName,
                        FamilyRoleHelper.formatJoinMessageForReis(normPartnerName)
                    )
                }

                // Immediately broadcast my status back to spouse
                broadcastCurrentStatusToPartner(todayDate = today)
                return matched
            }
        }

        val savedPoolCode = childPrefs?.getString("family_pool_code", null) ?: ""
        val effectivePoolCode = savedPoolCode.ifBlank {
            if (current.isMatched) current.partnerInviteCode else current.myInviteCode
        }

        // 2. Fetch updates, events and check unmatch in single call
        val syncResult = PartnerSyncManager.fetchPartnerUpdatesAndEvents(
            codeA = current.myInviteCode,
            codeB = current.partnerInviteCode,
            partnerCode = current.partnerInviteCode,
            myCode = current.myInviteCode,
            todayDate = today,
            lastProcessedEventTime = lastProcessedEventTime,
            poolCode = effectivePoolCode
        )

        // Check if an incoming match claim was delivered via pool or inbox
        val claim = syncResult.incomingClaim
        if (claim != null && claim.partnerCode.isNotBlank() && (!current.isMatched || current.partnerInviteCode != claim.partnerCode)) {
            val normClaimName = FamilyRoleHelper.normalizeRoleTitle(claim.partnerName.ifBlank { "Aile Ferdi" })
            val claimIcon = if (claim.partnerIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(claim.partnerIcon, normClaimName)) {
                claim.partnerIcon
            } else {
                FamilyRoleHelper.getDefaultIconForRole(normClaimName)
            }
            childPrefs?.edit()
                ?.putBoolean("user_explicitly_disconnected", false)
                ?.putString("p2_name", normClaimName)
                ?.putString("p2_icon", claimIcon)
                ?.apply()

            val matched = current.copy(
                isMatched = true,
                partnerDisplayName = normClaimName,
                partnerEmail = claim.partnerEmail,
                partnerInviteCode = claim.partnerCode,
                lastSyncTime = System.currentTimeMillis()
            )
            prayerDao.savePartnerInfo(matched)
            current = matched
            if (context != null) {
                NotificationHelper.showPartnerAlertNotification(
                    context,
                    normClaimName,
                    FamilyRoleHelper.formatJoinMessageForReis(normClaimName)
                )
            }
            broadcastCurrentStatusToPartner(todayDate = today)
        }

        // Handle unmatch from partner
        if (syncResult.partnerUnmatched) {
            val unmatchEntity = current.copy(
                isMatched = false,
                partnerDisplayName = "",
                partnerEmail = "",
                partnerInviteCode = "",
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE",
                partnerKazaCompleted = 0,
                partnerKazaOwed = 0
            )
            prayerDao.savePartnerInfo(unmatchEntity)
            return unmatchEntity
        }

        // Trigger notifications for new events (prayers, pool join/leave, etc.)
        if (syncResult.newEvents.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val validEvents = syncResult.newEvents.filter { event ->
                val sig = "${event.senderCode}_${event.eventType}_${event.prayerName}_${event.timestamp}"
                val isRecent = (now - event.timestamp) < 15 * 60 * 1000L && (event.timestamp - now) < 5 * 60 * 1000L
                val notProcessed = !processedEventSignatures.contains(sig)
                (notProcessed && isRecent) || (event.timestamp > lastProcessedEventTime)
            }
            if (validEvents.isNotEmpty()) {
                for (event in validEvents.sortedBy { it.timestamp }) {
                    val sig = "${event.senderCode}_${event.eventType}_${event.prayerName}_${event.timestamp}"
                    processedEventSignatures.add(sig)
                    if (event.timestamp > lastProcessedEventTime) {
                        lastProcessedEventTime = event.timestamp
                    }

                    if (event.eventType == "MEMBER_JOINED") {
                        val normEventName = FamilyRoleHelper.normalizeRoleTitle(event.senderName.ifBlank { "Aile Ferdi" })
                        val eventIcon = if (event.senderIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(event.senderIcon, normEventName)) {
                            event.senderIcon
                        } else {
                            FamilyRoleHelper.getDefaultIconForRole(normEventName)
                        }
                        childPrefs?.edit()?.putString("p2_name", normEventName)?.putString("p2_icon", eventIcon)?.apply()

                        if (context != null) {
                            NotificationHelper.showPartnerAlertNotification(
                                context = context,
                                partnerName = normEventName,
                                actionText = FamilyRoleHelper.formatJoinMessageForReis(normEventName)
                            )
                        }
                        if (!current.isMatched || current.partnerInviteCode.isBlank()) {
                            val matched = current.copy(
                                isMatched = true,
                                partnerDisplayName = normEventName,
                                partnerInviteCode = event.senderCode,
                                lastSyncTime = System.currentTimeMillis()
                            )
                            prayerDao.savePartnerInfo(matched)
                            current = matched
                            broadcastCurrentStatusToPartner(todayDate = today)
                        }
                    } else if (event.eventType == "MEMBER_LEFT") {
                        val normLeftName = FamilyRoleHelper.normalizeRoleTitle(event.senderName.ifBlank { "Aile Ferdi" })
                        if (context != null) {
                            NotificationHelper.showPartnerAlertNotification(
                                context = context,
                                partnerName = normLeftName,
                                actionText = FamilyRoleHelper.formatLeftMessageForReis(normLeftName)
                            )
                        }
                        if (current.partnerInviteCode == event.senderCode) {
                            val unmatchEntity = current.copy(
                                isMatched = false,
                                partnerDisplayName = "",
                                partnerEmail = "",
                                partnerInviteCode = "",
                                partnerFajr = "NONE",
                                partnerDhuhr = "NONE",
                                partnerAsr = "NONE",
                                partnerMaghrib = "NONE",
                                partnerIsha = "NONE",
                                partnerKazaCompleted = 0,
                                partnerKazaOwed = 0
                            )
                            prayerDao.savePartnerInfo(unmatchEntity)
                            current = unmatchEntity
                        }
                    } else {
                        if (context != null) {
                            val sanitized = event.actionText
                                .replace("kazaya bıraktı", "kılmadı")
                                .replace("Kazaya bıraktı", "kılmadı")
                            NotificationHelper.showPartnerAlertNotification(
                                context = context,
                                partnerName = current.partnerDisplayName.ifBlank { event.senderName.ifBlank { "Aile Ferdi" } },
                                actionText = sanitized
                            )
                        }
                    }
                }
            }
        }

        // Update partner prayer status in Room
        val todayUpdate = syncResult.latestStatus
        val yesterdayUpdate = syncResult.yesterdayStatus

        if (todayUpdate != null || yesterdayUpdate != null) {
            val activeUpdate = todayUpdate ?: yesterdayUpdate!!
            val updated = current.copy(
                lastSyncTime = if (activeUpdate.timestamp > 0) activeUpdate.timestamp else System.currentTimeMillis(),
                partnerDisplayName = if (current.partnerDisplayName.isNotBlank()) current.partnerDisplayName else activeUpdate.senderName.ifBlank { "Aile Ferdi" },
                partnerEmail = if (activeUpdate.senderEmail.isNotBlank()) activeUpdate.senderEmail else current.partnerEmail,
                partnerFajr = todayUpdate?.fajr ?: current.partnerFajr,
                partnerDhuhr = todayUpdate?.dhuhr ?: current.partnerDhuhr,
                partnerAsr = todayUpdate?.asr ?: current.partnerAsr,
                partnerMaghrib = todayUpdate?.maghrib ?: current.partnerMaghrib,
                partnerIsha = todayUpdate?.isha ?: current.partnerIsha,
                partnerYesterdayFajr = yesterdayUpdate?.fajr ?: current.partnerYesterdayFajr,
                partnerYesterdayDhuhr = yesterdayUpdate?.dhuhr ?: current.partnerYesterdayDhuhr,
                partnerYesterdayAsr = yesterdayUpdate?.asr ?: current.partnerYesterdayAsr,
                partnerYesterdayMaghrib = yesterdayUpdate?.maghrib ?: current.partnerYesterdayMaghrib,
                partnerYesterdayIsha = yesterdayUpdate?.isha ?: current.partnerYesterdayIsha,
                partnerKazaCompleted = activeUpdate.kazaCompleted,
                partnerKazaOwed = activeUpdate.kazaOwed
            )
            prayerDao.savePartnerInfo(updated)
            return updated
        }

        return current
    }

    suspend fun matchPartner(code: String, partnerName: String = "", partnerEmail: String = ""): Boolean {
        val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
        childPrefs?.edit()?.putBoolean("user_explicitly_disconnected", false)?.apply()

        var current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        if (current.myInviteCode.isBlank() || !current.myInviteCode.startsWith("REIS-00")) {
            current = ensureUniqueInviteCode()
        }

        val targetCode = PartnerSyncManager.normalizeCode(code)
        if (targetCode.isBlank() || targetCode.length < 5) {
            return false
        }

        var myCleanCode = PartnerSyncManager.normalizeCode(current.myInviteCode)
        if (targetCode == myCleanCode) {
            // If this device had the same code (e.g. from a shared cloud backup restore), generate a unique code for this device
            var randomSuffix = (1000..9999).random()
            while ("REIS00$randomSuffix" == targetCode) {
                randomSuffix = (1000..9999).random()
            }
            val newCode = "REIS-00$randomSuffix"
            current = current.copy(myInviteCode = newCode)
            prayerDao.savePartnerInfo(current)
            myCleanCode = PartnerSyncManager.normalizeCode(newCode)
        }

        val displayTargetCode = PartnerSyncManager.formatDisplayCode(code)

        // Look up partner's profile from cloud
        val cloudProfile = PartnerSyncManager.lookupProfileByCode(targetCode)

        val rawDeterminedName = when {
            partnerName.isNotBlank() -> partnerName.trim()
            !cloudProfile?.first.isNullOrBlank() -> cloudProfile!!.first.trim()
            !cloudProfile?.second.isNullOrBlank() -> cloudProfile!!.second.substringBefore("@")
            else -> "Aile Reisi"
        }
        val determinedName = FamilyRoleHelper.normalizeRoleTitle(rawDeterminedName)

        val determinedEmail = when {
            !cloudProfile?.second.isNullOrBlank() -> cloudProfile!!.second.trim()
            partnerEmail.isNotBlank() -> partnerEmail.trim()
            else -> ""
        }

        val partnerIcon = cloudProfile?.third ?: ""
        val resolvedPartnerIcon = if (partnerIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(partnerIcon, determinedName)) {
            partnerIcon
        } else {
            FamilyRoleHelper.getDefaultIconForRole(determinedName)
        }
        childPrefs?.edit()?.putString("p2_name", determinedName)?.putString("p2_icon", resolvedPartnerIcon)?.apply()

        val myIcon = childPrefs?.getString("p1_icon", "👦") ?: "👦"
        val myDisplayName = FamilyRoleHelper.normalizeRoleTitle(current.myDisplayName.ifBlank { "Aile Ferdi" })

        // 1. Send claim to partner's inbox so they get matched automatically
        PartnerSyncManager.sendPairClaim(
            targetCode = displayTargetCode,
            myCode = current.myInviteCode,
            myName = myDisplayName,
            myEmail = current.myEmail,
            myIcon = myIcon
        )

        // 2. Broadcast to the family pool topic so everyone in the pool gets notified
        PartnerSyncManager.broadcastPoolMemberJoined(
            poolCode = displayTargetCode,
            myCode = current.myInviteCode,
            myName = myDisplayName,
            myEmail = current.myEmail,
            myIcon = myIcon
        )

        // 3. Broadcast direct event to canonical pair topic
        PartnerSyncManager.broadcastPartnerEvent(
            codeA = current.myInviteCode,
            codeB = displayTargetCode,
            myCode = current.myInviteCode,
            myName = myDisplayName,
            eventType = "MEMBER_JOINED",
            prayerName = "Aile Havuzu",
            actionText = FamilyRoleHelper.formatJoinMessageForReis(myDisplayName),
            senderIcon = myIcon
        )

        val updated = current.copy(
            isMatched = true,
            partnerDisplayName = determinedName,
            partnerEmail = determinedEmail,
            partnerInviteCode = displayTargetCode,
            lastSyncTime = System.currentTimeMillis()
        )
        prayerDao.savePartnerInfo(updated)

        // Immediately broadcast our status to spouse and pool
        broadcastCurrentStatusToPartner()
        // And immediately fetch partner's latest status from cloud
        try {
            refreshPartnerData()
        } catch (_: Exception) { }
        return true
    }

    suspend fun leaveFamilyPool(poolCode: String = "") {
        val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
        childPrefs?.edit()
            ?.putBoolean("user_explicitly_disconnected", true)
            ?.putBoolean("family_pool_created", false)
            ?.putBoolean("family_pool_has_members", false)
            ?.remove("is_pool_member_joined")
            ?.remove("family_pool_code")
            ?.remove("is_explicit_reis")
            ?.remove("p2_name")
            ?.remove("p2_icon")
            ?.putString("user_role", "FERT")
            ?.apply()

        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val effectivePool = if (poolCode.isNotBlank()) poolCode else current.partnerInviteCode
        val myName = current.myDisplayName.ifBlank { "Aile Ferdi" }

        if (effectivePool.isNotBlank()) {
            PartnerSyncManager.broadcastPoolMemberLeft(
                poolCode = effectivePool,
                myCode = current.myInviteCode,
                myName = myName
            )
        }

        if (current.isMatched && current.partnerInviteCode.isNotBlank()) {
            PartnerSyncManager.broadcastPartnerEvent(
                codeA = current.myInviteCode,
                codeB = current.partnerInviteCode,
                myCode = current.myInviteCode,
                myName = myName,
                eventType = "MEMBER_LEFT",
                prayerName = "Aile Havuzu",
                actionText = "$myName aile havuzundan ayrıldı 🔴"
            )
            PartnerSyncManager.broadcastUnmatch(
                codeA = current.myInviteCode,
                codeB = current.partnerInviteCode,
                myCode = current.myInviteCode
            )
        }

        prayerDao.savePartnerInfo(
            current.copy(
                isMatched = false,
                partnerDisplayName = "",
                partnerEmail = "",
                partnerInviteCode = "",
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE",
                partnerYesterdayFajr = "NONE",
                partnerYesterdayDhuhr = "NONE",
                partnerYesterdayAsr = "NONE",
                partnerYesterdayMaghrib = "NONE",
                partnerYesterdayIsha = "NONE",
                partnerKazaCompleted = 0,
                partnerKazaOwed = 0
            )
        )
    }

    suspend fun unmatchPartner() {
        leaveFamilyPool()
    }

    suspend fun updatePartnerName(name: String) {
        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        prayerDao.savePartnerInfo(current.copy(partnerDisplayName = name.trim()))
    }

    // Settings & Login
    fun getSettings(): Flow<AppSettingsEntity?> {
        return prayerDao.getSettings()
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        prayerDao.saveSettings(settings)
    }

    suspend fun triggerAutoCloudBackup() {
        try {
            val settings = prayerDao.getSettings().firstOrNull() ?: return
            val email = settings.userEmail.trim().lowercase()
            if (!settings.isLoggedIn || email.isBlank()) return

            val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
            val kazas = prayerDao.getAllKazaPrayersDirect()
            val kazasArray = org.json.JSONArray()
            kazas.forEach { k ->
                kazasArray.put(
                    org.json.JSONObject().apply {
                        put("prayerType", k.prayerType)
                        put("owedCount", k.owedCount)
                        put("completedCount", k.completedCount)
                    }
                )
            }

            // Günlük kıldı / kılmadı verilerini de buluta yedekle
            val dailyPrayers = prayerDao.getAllDailyPrayersDirect()
            val dailyArray = org.json.JSONArray()
            dailyPrayers.forEach { dp ->
                dailyArray.put(
                    org.json.JSONObject().apply {
                        put("date", dp.date)
                        put("fajrStatus", dp.fajrStatus)
                        put("dhuhrStatus", dp.dhuhrStatus)
                        put("asrStatus", dp.asrStatus)
                        put("maghribStatus", dp.maghribStatus)
                        put("ishaStatus", dp.ishaStatus)
                    }
                )
            }

            PartnerSyncManager.backupUserDataToCloud(
                email = email,
                name = settings.userName,
                inviteCode = partner.myInviteCode,
                kazalarJson = kazasArray.toString(),
                dailyPrayersJson = dailyArray.toString(),
                isMatched = partner.isMatched,
                partnerCode = partner.partnerInviteCode,
                partnerName = partner.partnerDisplayName
            )
        } catch (_: Exception) { }
    }

    suspend fun loginUser(name: String, email: String) {
        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val updated = current.copy(
            userName = name.trim(),
            userEmail = email.trim(),
            isLoggedIn = true
        )
        prayerDao.saveSettings(updated)

        // Also update myDisplayName and myEmail in partner_info so partner sees it
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val updatedPartner = partner.copy(
            myDisplayName = name.trim(),
            myEmail = email.trim()
        )
        prayerDao.savePartnerInfo(updatedPartner)

        // Publish my profile to cloud
        PartnerSyncManager.publishMyProfile(
            myCode = updatedPartner.myInviteCode,
            name = name.trim(),
            email = email.trim()
        )

        // Otomatik Bulut Kurtarma: Yeni telefonda veya yeniden kurulumda Google oturumu açılınca
        // kullanıcının kayıtlı kaza, günlük kıldı/kılmadı ve aile bilgilerini buluttan güvenle geri yükle
        try {
            val cloudBackup = PartnerSyncManager.restoreUserDataFromCloud(email.trim())
            if (cloudBackup != null) {
                // 1. Kaza namazları: Kullanıcının yerel olarak girdiği sayılar ASLA sıfırlanmaz veya silinmez
                val kazalarStr = cloudBackup.optString("kazalarJson", "[]")
                val kazasArr = org.json.JSONArray(kazalarStr)
                val cloudKazaList = mutableListOf<KazaPrayerEntity>()
                for (i in 0 until kazasArr.length()) {
                    val item = kazasArr.getJSONObject(i)
                    cloudKazaList.add(
                        KazaPrayerEntity(
                            prayerType = item.getString("prayerType"),
                            owedCount = item.optInt("owedCount", 0),
                            completedCount = item.optInt("completedCount", 0)
                        )
                    )
                }

                val localKazas = prayerDao.getAllKazaPrayersDirect().associateBy { it.prayerType.uppercase() }
                val mergedKazas = mutableListOf<KazaPrayerEntity>()
                val prayerTypes = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA", "WITR")
                for (type in prayerTypes) {
                    val local = localKazas[type]
                    val cloud = cloudKazaList.firstOrNull { it.prayerType.equals(type, ignoreCase = true) }
                    val localOwed = local?.owedCount ?: 0
                    val cloudOwed = cloud?.owedCount ?: 0
                    val localCompleted = local?.completedCount ?: 0
                    val cloudCompleted = cloud?.completedCount ?: 0

                    // Kullanıcının kendi tarafında girdiği kaza borcu ve kılınan kaza sayısı ASLA sıfırlanmaz
                    val finalOwed = when {
                        localOwed > 0 && cloudOwed == 0 -> localOwed
                        localOwed == 0 && cloudOwed > 0 -> cloudOwed
                        localOwed > 0 && cloudOwed > 0 -> maxOf(localOwed, cloudOwed)
                        else -> 0
                    }
                    val finalCompleted = maxOf(localCompleted, cloudCompleted)
                    mergedKazas.add(
                        KazaPrayerEntity(
                            prayerType = type,
                            owedCount = finalOwed,
                            completedCount = finalCompleted
                        )
                    )
                }
                if (mergedKazas.isNotEmpty()) {
                    prayerDao.insertAllKazaPrayers(mergedKazas)
                }

                // 2. Günlük kıldı / kılmadı geçmişi: Kullanıcının kendi tarafında işaretlediği kıldı/kılmadı ASLA silinmez
                val dailyStr = cloudBackup.optString("dailyPrayersJson", "[]")
                val dailyArr = org.json.JSONArray(dailyStr)
                val cloudDailyList = mutableListOf<DailyPrayerEntity>()
                for (i in 0 until dailyArr.length()) {
                    val item = dailyArr.getJSONObject(i)
                    cloudDailyList.add(
                        DailyPrayerEntity(
                            date = item.getString("date"),
                            fajrStatus = item.optString("fajrStatus", "NONE"),
                            dhuhrStatus = item.optString("dhuhrStatus", "NONE"),
                            asrStatus = item.optString("asrStatus", "NONE"),
                            maghribStatus = item.optString("maghribStatus", "NONE"),
                            ishaStatus = item.optString("ishaStatus", "NONE")
                        )
                    )
                }

                val localDailyMap = prayerDao.getAllDailyPrayersDirect().associateBy { it.date }.toMutableMap()
                fun mergeStatus(localStat: String?, cloudStat: String?): String {
                    val l = localStat ?: "NONE"
                    val c = cloudStat ?: "NONE"
                    return if (l != "NONE") l else c
                }
                for (cItem in cloudDailyList) {
                    val lItem = localDailyMap[cItem.date]
                    if (lItem == null) {
                        localDailyMap[cItem.date] = cItem
                    } else {
                        localDailyMap[cItem.date] = lItem.copy(
                            fajrStatus = mergeStatus(lItem.fajrStatus, cItem.fajrStatus),
                            dhuhrStatus = mergeStatus(lItem.dhuhrStatus, cItem.dhuhrStatus),
                            asrStatus = mergeStatus(lItem.asrStatus, cItem.asrStatus),
                            maghribStatus = mergeStatus(lItem.maghribStatus, cItem.maghribStatus),
                            ishaStatus = mergeStatus(lItem.ishaStatus, cItem.ishaStatus)
                        )
                    }
                }
                if (localDailyMap.isNotEmpty()) {
                    prayerDao.insertAllDailyPrayers(localDailyMap.values.toList())
                }

                // 3. Eşleşme ve aile bilgileri yedeği
                val savedPartnerCode = cloudBackup.optString("partnerCode", "")
                val savedPartnerName = cloudBackup.optString("partnerName", "")
                val savedIsMatched = cloudBackup.optBoolean("isMatched", false)
                if (savedIsMatched && savedPartnerCode.isNotBlank()) {
                    prayerDao.savePartnerInfo(
                        updatedPartner.copy(
                            isMatched = true,
                            partnerInviteCode = savedPartnerCode,
                            partnerDisplayName = savedPartnerName
                        )
                    )
                }

                // Buluttan geri yüklenen ve yerel ile birleştirilen en güncel verileri hemen buluta tekrar kaydet
                triggerAutoCloudBackup()
            } else {
                // Eğer bulutta henüz yedek yoksa mevcut yerel verileri ilk kez buluta yedekle
                triggerAutoCloudBackup()
            }
        } catch (_: Exception) { }
    }

    suspend fun updateMyProfileName(newName: String) {
        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val updated = current.copy(
            userName = newName.trim(),
            isLoggedIn = true
        )
        prayerDao.saveSettings(updated)

        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val updatedPartner = partner.copy(myDisplayName = newName.trim())
        prayerDao.savePartnerInfo(updatedPartner)

        PartnerSyncManager.publishMyProfile(
            myCode = updatedPartner.myInviteCode,
            name = newName.trim(),
            email = updatedPartner.myEmail
        )
    }

    suspend fun clearAllPrayerRecords() {
        prayerDao.clearAllDailyPrayers()
    }

    suspend fun logoutUser() {
        val childPrefs = context?.getSharedPreferences("family_child_prefs", android.content.Context.MODE_PRIVATE)
        childPrefs?.edit()
            ?.putBoolean("user_explicitly_disconnected", true)
            ?.putBoolean("family_pool_created", false)
            ?.putBoolean("family_pool_has_members", false)
            ?.remove("is_pool_member_joined")
            ?.remove("family_pool_code")
            ?.remove("is_explicit_reis")
            ?.remove("p2_name")
            ?.remove("p2_icon")
            ?.remove("has_p2")
            ?.remove("has_p2_customized")
            ?.remove("p1_name")
            ?.remove("p1_icon")
            ?.remove("user_role")
            ?.remove("user_explicit_role")
            ?.apply()

        val currentPartner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        prayerDao.savePartnerInfo(
            currentPartner.copy(
                isMatched = false,
                partnerDisplayName = "",
                partnerEmail = "",
                partnerInviteCode = "",
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE",
                partnerYesterdayFajr = "NONE",
                partnerYesterdayDhuhr = "NONE",
                partnerYesterdayAsr = "NONE",
                partnerYesterdayMaghrib = "NONE",
                partnerYesterdayIsha = "NONE"
            )
        )

        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        prayerDao.saveSettings(
            current.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        )
    }

    // =========================================================================
    // VERİ DIŞA / İÇE AKTARMA (MİGRASYON SİHİRBAZI: Aile ile Namaz)
    // =========================================================================
    suspend fun exportDataToJson(): String {
        val settings = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val dailyPrayers = prayerDao.getAllDailyPrayersDirect()
        val kazaPrayers = prayerDao.getAllKazaPrayersDirect()

        val root = org.json.JSONObject()
        root.put("sourceApp", "AileIleNamaz")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settingsObj = org.json.JSONObject().apply {
            put("userName", settings.userName)
            put("userEmail", settings.userEmail)
            put("cityName", settings.cityName)
            put("isLoggedIn", settings.isLoggedIn)
        }
        root.put("settings", settingsObj)

        val partnerObj = org.json.JSONObject().apply {
            put("myInviteCode", partner.myInviteCode)
            put("myDisplayName", partner.myDisplayName)
            put("isMatched", partner.isMatched)
            put("partnerDisplayName", partner.partnerDisplayName)
            put("partnerEmail", partner.partnerEmail)
            put("partnerInviteCode", partner.partnerInviteCode)
            put("partnerKazaOwed", partner.partnerKazaOwed)
            put("partnerKazaCompleted", partner.partnerKazaCompleted)
        }
        root.put("partner", partnerObj)

        val prayersArray = org.json.JSONArray()
        dailyPrayers.forEach { p ->
            val pObj = org.json.JSONObject().apply {
                put("date", p.date)
                put("fajrStatus", p.fajrStatus)
                put("dhuhrStatus", p.dhuhrStatus)
                put("asrStatus", p.asrStatus)
                put("maghribStatus", p.maghribStatus)
                put("ishaStatus", p.ishaStatus)
                put("note", p.note)
            }
            prayersArray.put(pObj)
        }
        root.put("dailyPrayers", prayersArray)

        val kazasArray = org.json.JSONArray()
        kazaPrayers.forEach { k ->
            val kObj = org.json.JSONObject().apply {
                put("prayerType", k.prayerType)
                put("owedCount", k.owedCount)
                put("completedCount", k.completedCount)
            }
            kazasArray.put(kObj)
        }
        root.put("kazaPrayers", kazasArray)

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonStr.trim())

            // 1. Ayarları Geri Yükle
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
                prayerDao.saveSettings(
                    current.copy(
                        userName = sObj.optString("userName", current.userName),
                        userEmail = sObj.optString("userEmail", current.userEmail),
                        cityName = sObj.optString("cityName", current.cityName),
                        isLoggedIn = sObj.optBoolean("isLoggedIn", true)
                    )
                )
            }

            // 2. Partner (Eş) Bilgilerini Geri Yükle ve Aile Havuzu 2. Kişisi Yap
            if (root.has("partner")) {
                val pObj = root.getJSONObject("partner")
                val currentPartner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
                val deviceCode = if (currentPartner.myInviteCode.isNotBlank() && currentPartner.myInviteCode.startsWith("REIS-00")) {
                    currentPartner.myInviteCode
                } else {
                    val fromBackup = pObj.optString("myInviteCode", "")
                    if (fromBackup.isNotBlank() && fromBackup.startsWith("REIS-00")) {
                        fromBackup
                    } else {
                        "REIS-00${(1000..9999).random()}"
                    }
                }
                val updatedPartner = currentPartner.copy(
                    myInviteCode = deviceCode,
                    isMatched = pObj.optBoolean("isMatched", currentPartner.isMatched),
                    partnerDisplayName = pObj.optString("partnerDisplayName", currentPartner.partnerDisplayName),
                    partnerEmail = pObj.optString("partnerEmail", currentPartner.partnerEmail),
                    partnerInviteCode = pObj.optString("partnerInviteCode", currentPartner.partnerInviteCode),
                    partnerKazaOwed = pObj.optInt("partnerKazaOwed", currentPartner.partnerKazaOwed),
                    partnerKazaCompleted = pObj.optInt("partnerKazaCompleted", currentPartner.partnerKazaCompleted)
                )
                prayerDao.savePartnerInfo(updatedPartner)
            }

            // 3. Günlük Namaz Kayıtlarını Aktar
            if (root.has("dailyPrayers")) {
                val prayersArr = root.getJSONArray("dailyPrayers")
                val list = mutableListOf<DailyPrayerEntity>()
                for (i in 0 until prayersArr.length()) {
                    val item = prayersArr.getJSONObject(i)
                    list.add(
                        DailyPrayerEntity(
                            date = item.getString("date"),
                            fajrStatus = item.optString("fajrStatus", "NONE"),
                            dhuhrStatus = item.optString("dhuhrStatus", "NONE"),
                            asrStatus = item.optString("asrStatus", "NONE"),
                            maghribStatus = item.optString("maghribStatus", "NONE"),
                            ishaStatus = item.optString("ishaStatus", "NONE"),
                            note = item.optString("note", "")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    prayerDao.insertAllDailyPrayers(list)
                }
            }

            // 4. Kaza Namazlarını Aktar
            if (root.has("kazaPrayers")) {
                val kazasArr = root.getJSONArray("kazaPrayers")
                val kList = mutableListOf<KazaPrayerEntity>()
                for (i in 0 until kazasArr.length()) {
                    val item = kazasArr.getJSONObject(i)
                    kList.add(
                        KazaPrayerEntity(
                            prayerType = item.getString("prayerType"),
                            owedCount = item.optInt("owedCount", 0),
                            completedCount = item.optInt("completedCount", 0)
                        )
                    )
                }
                if (kList.isNotEmpty()) {
                    prayerDao.insertAllKazaPrayers(kList)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
