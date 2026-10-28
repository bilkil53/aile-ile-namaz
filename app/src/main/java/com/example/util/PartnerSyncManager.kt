package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PartnerPrayerUpdate(
    val senderCode: String,
    val senderName: String,
    val senderEmail: String,
    val date: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val kazaCompleted: Int,
    val kazaOwed: Int,
    val timestamp: Long
)

data class PartnerRemoteEvent(
    val senderCode: String,
    val senderName: String,
    val eventType: String, // "PRAYED", "MISSED", "KAZA"
    val prayerName: String,
    val actionText: String,
    val timestamp: Long,
    val senderIcon: String = ""
)

data class IncomingMatchInfo(
    val partnerCode: String,
    val partnerName: String,
    val partnerEmail: String,
    val partnerIcon: String = ""
)

data class IncomingJoinRequest(
    val claimerCode: String,
    val claimerName: String,
    val claimerEmail: String,
    val claimerIcon: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class JoinApprovalInfo(
    val leaderCode: String,
    val leaderRole: String,
    val leaderIcon: String = "👑",
    val leaderEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RoleAssignmentInfo(
    val leaderCode: String,
    val leaderRole: String,
    val leaderIcon: String = "👑",
    val memberRole: String = "Aile Ferdi",
    val memberIcon: String = "👥",
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncResult(
    val latestStatus: PartnerPrayerUpdate?,
    val yesterdayStatus: PartnerPrayerUpdate? = null,
    val newEvents: List<PartnerRemoteEvent>,
    val partnerUnmatched: Boolean,
    val incomingClaim: IncomingMatchInfo? = null
)

object PartnerSyncManager {

    private const val TAG = "PartnerSyncManager"
    private const val BASE_URL = "https://ntfy.sh"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    fun normalizeCode(code: String): String {
        val raw = code.trim().uppercase().replace(" ", "").replace("-", "")
        if (raw.isBlank()) return ""
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("REIS00") -> raw
            raw.startsWith("REIS") && digits.isNotBlank() -> {
                val num = if (digits.startsWith("00")) digits else if (digits.startsWith("0")) "0$digits" else "00$digits"
                "REIS$num"
            }
            digits.length in 4..6 -> {
                val num = if (digits.startsWith("00")) digits else if (digits.length == 4) "00$digits" else digits
                "REIS$num"
            }
            else -> raw
        }
    }

    fun formatDisplayCode(code: String): String {
        val clean = normalizeCode(code)
        if (clean.isBlank()) return ""
        return when {
            clean.startsWith("REIS") && clean.length > 4 -> {
                val sub = clean.substring(4)
                if (!sub.startsWith("00") && sub.length == 4) "REIS-00$sub" else "REIS-$sub"
            }
            clean.startsWith("EMB53") && clean.length > 5 -> "EMB-53" + clean.substring(5)
            clean.startsWith("EMB") && clean.length > 3 -> "EMB-" + clean.substring(3)
            clean.length > 4 -> clean.take(4) + "-" + clean.drop(4)
            else -> clean
        }
    }

    fun getPoolTopic(poolCode: String): String {
        val clean = normalizeCode(poolCode)
        return "ain_pool_$clean"
    }

    fun getCanonicalPairTopic(codeA: String, codeB: String): String {
        val a = normalizeCode(codeA)
        val b = normalizeCode(codeB)
        val sorted = if (a < b) "${a}_${b}" else "${b}_${a}"
        return "ain_pair_$sorted"
    }

    /**
     * Broadcasts when a member joins the family pool.
     */
    suspend fun broadcastPoolMemberJoined(
        poolCode: String,
        myCode: String,
        myName: String,
        myEmail: String = "",
        myIcon: String = ""
    ) = withContext(Dispatchers.IO) {
        val clean = normalizeCode(poolCode)
        if (clean.isBlank()) return@withContext
        val topic = getPoolTopic(poolCode)
        val normalizedName = FamilyRoleHelper.normalizeRoleTitle(myName)
        val resolvedIcon = if (myIcon.isNotBlank()) myIcon else FamilyRoleHelper.getDefaultIconForRole(normalizedName)
        try {
            val json = JSONObject().apply {
                put("type", "POOL_EVENT")
                put("eventType", "MEMBER_JOINED")
                put("senderCode", formatDisplayCode(myCode))
                put("senderName", normalizedName)
                put("senderEmail", myEmail.trim())
                put("senderIcon", resolvedIcon)
                put("actionText", FamilyRoleHelper.formatJoinMessageForReis(normalizedName))
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
            Log.i(TAG, "Pool member joined broadcasted to $topic: $normalizedName ($resolvedIcon)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast pool member joined: ${e.message}")
        }
    }

    /**
     * Broadcasts when a member leaves the family pool.
     */
    suspend fun broadcastPoolMemberLeft(
        poolCode: String,
        myCode: String,
        myName: String
    ) = withContext(Dispatchers.IO) {
        val clean = normalizeCode(poolCode)
        if (clean.isBlank()) return@withContext
        val topic = getPoolTopic(poolCode)
        val normalizedName = FamilyRoleHelper.normalizeRoleTitle(myName)
        try {
            val json = JSONObject().apply {
                put("type", "POOL_EVENT")
                put("eventType", "MEMBER_LEFT")
                put("senderCode", formatDisplayCode(myCode))
                put("senderName", normalizedName)
                put("actionText", FamilyRoleHelper.formatLeftMessageForReis(normalizedName))
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
            Log.i(TAG, "Pool member left broadcasted to $topic: $normalizedName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast pool member left: ${e.message}")
        }
    }

    /**
     * Publishes my profile so that when someone enters my code, they can see my name and email.
     */
    suspend fun publishMyProfile(myCode: String, name: String, email: String, icon: String = "") = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(myCode)
        if (cleaned.isBlank()) return@withContext
        val normalizedName = FamilyRoleHelper.normalizeRoleTitle(name)
        val resolvedIcon = if (icon.isNotBlank()) icon else FamilyRoleHelper.getDefaultIconForRole(normalizedName)

        try {
            val json = JSONObject().apply {
                put("type", "USER_PROFILE")
                put("inviteCode", formatDisplayCode(myCode))
                put("displayName", normalizedName)
                put("email", email.trim())
                put("icon", resolvedIcon)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/ain_usr_$cleaned")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish profile: ${e.message}")
        }
    }

    /**
     * Looks up user profile by invite code.
     * Returns Triple(displayName, email, icon).
     */
    suspend fun lookupProfileByCode(targetCode: String): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(targetCode)
        if (cleaned.isBlank()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$BASE_URL/ain_usr_$cleaned/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var lastProfile: Triple<String, String, String>? = null
                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                if (msgJson.optString("type") == "USER_PROFILE") {
                                    val name = msgJson.optString("displayName", "")
                                    val email = msgJson.optString("email", "")
                                    val icon = msgJson.optString("icon", "")
                                    lastProfile = Triple(name, email, icon)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                lastProfile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to lookup profile: ${e.message}")
            null
        }
    }

    /**
     * Called by User 2 when they enter User 1's code.
     * User 2 sends a PAIR_CLAIMED notification directly to User 1's inbox.
     */
    suspend fun sendPairClaim(
        targetCode: String,
        myCode: String,
        myName: String,
        myEmail: String,
        myIcon: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanedTarget = normalizeCode(targetCode)
        if (cleanedTarget.isBlank()) return@withContext false
        val normalizedName = FamilyRoleHelper.normalizeRoleTitle(myName)
        val resolvedIcon = if (myIcon.isNotBlank()) myIcon else FamilyRoleHelper.getDefaultIconForRole(normalizedName)

        try {
            val json = JSONObject().apply {
                put("type", "PAIR_CLAIMED")
                put("partnerInviteCode", formatDisplayCode(targetCode))
                put("claimerCode", formatDisplayCode(myCode))
                put("claimerName", normalizedName)
                put("claimerEmail", myEmail.trim())
                put("claimerIcon", resolvedIcon)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/ain_inv_$cleanedTarget")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send pair claim: ${e.message}")
            false
        }
    }

    /**
     * Called by User 1 to check if someone claimed their invite code.
     * When found, User 1 is automatically paired without ever entering a code!
     */
    suspend fun checkForIncomingMatch(myCode: String): IncomingMatchInfo? = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(myCode)
        if (cleaned.isBlank()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$BASE_URL/ain_inv_$cleaned/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var lastClaim: IncomingMatchInfo? = null
                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                if (msgJson.optString("type") == "PAIR_CLAIMED") {
                                    val claimerCode = msgJson.optString("claimerCode", "")
                                    val claimerName = msgJson.optString("claimerName", "")
                                    val claimerEmail = msgJson.optString("claimerEmail", "")
                                    val claimerIcon = msgJson.optString("claimerIcon", "")
                                    if (claimerCode.isNotBlank()) {
                                        val normName = FamilyRoleHelper.normalizeRoleTitle(claimerName)
                                        val finalIcon = if (claimerIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(claimerIcon, normName)) {
                                            claimerIcon
                                        } else {
                                            FamilyRoleHelper.getDefaultIconForRole(normName)
                                        }
                                        lastClaim = IncomingMatchInfo(
                                            partnerCode = formatDisplayCode(claimerCode),
                                            partnerName = normName,
                                            partnerEmail = claimerEmail,
                                            partnerIcon = finalIcon
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                lastClaim
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check incoming match: ${e.message}")
            null
        }
    }

    /**
     * Called by Aile Reisi to assign mutual roles (both leader's role and member's role).
     * Broadcasts to pair topic and directly to the member's personal inbox.
     */
    suspend fun broadcastRoleAssignment(
        targetCode: String,
        myCode: String,
        leaderRole: String,
        leaderIcon: String,
        memberRole: String,
        memberIcon: String
    ) = withContext(Dispatchers.IO) {
        val cleanedTarget = normalizeCode(targetCode)
        val cleanedMine = normalizeCode(myCode)
        if (cleanedTarget.isBlank() || cleanedMine.isBlank()) return@withContext

        val payload = JSONObject().apply {
            put("type", "ROLE_ASSIGNMENT")
            put("leaderCode", formatDisplayCode(myCode))
            put("memberCode", formatDisplayCode(targetCode))
            put("leaderRole", leaderRole)
            put("leaderIcon", leaderIcon)
            put("memberRole", memberRole)
            put("memberIcon", memberIcon)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        try {
            val pairTopic = getCanonicalPairTopic(cleanedMine, cleanedTarget)
            val req1 = Request.Builder()
                .url("$BASE_URL/$pairTopic")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req1).execute().close()
        } catch (_: Exception) { }

        try {
            val req2 = Request.Builder()
                .url("$BASE_URL/ain_inv_$cleanedTarget")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req2).execute().close()
        } catch (_: Exception) { }
    }

    suspend fun checkForIncomingRoleAssignment(myCode: String): RoleAssignmentInfo? = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(myCode)
        if (cleaned.isBlank()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$BASE_URL/ain_inv_$cleaned/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var lastAssignment: RoleAssignmentInfo? = null
                val now = System.currentTimeMillis()
                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                if (msgJson.optString("type") == "ROLE_ASSIGNMENT") {
                                    val leaderCode = msgJson.optString("leaderCode", "")
                                    val leaderRole = msgJson.optString("leaderRole", "Aile Reisi")
                                    val leaderIcon = msgJson.optString("leaderIcon", "👑")
                                    val memberRole = msgJson.optString("memberRole", "Aile Ferdi")
                                    val memberIcon = msgJson.optString("memberIcon", "👥")
                                    val ts = msgJson.optLong("timestamp", now)
                                    if (leaderCode.isNotBlank() && (now - ts) < 30 * 60 * 1000L) {
                                        lastAssignment = RoleAssignmentInfo(
                                            leaderCode = formatDisplayCode(leaderCode),
                                            leaderRole = leaderRole,
                                            leaderIcon = leaderIcon,
                                            memberRole = memberRole,
                                            memberIcon = memberIcon,
                                            timestamp = ts
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                lastAssignment
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check role assignment: ${e.message}")
            null
        }
    }

    /**
     * Broadcasts a real-time prayer event (e.g. "Sabah namazını kıldı") so spouse receives an instant notification.
     */
    suspend fun broadcastPartnerEvent(
        codeA: String,
        codeB: String,
        myCode: String,
        myName: String,
        eventType: String,
        prayerName: String,
        actionText: String,
        senderIcon: String = ""
    ) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        val normalizedName = FamilyRoleHelper.normalizeRoleTitle(myName)
        val resolvedIcon = if (senderIcon.isNotBlank()) senderIcon else FamilyRoleHelper.getDefaultIconForRole(normalizedName)
        try {
            val json = JSONObject().apply {
                put("type", "PARTNER_EVENT")
                put("senderCode", normalizeCode(myCode))
                put("senderName", normalizedName)
                put("senderIcon", resolvedIcon)
                put("eventType", eventType)
                put("prayerName", prayerName)
                put("actionText", actionText)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast partner event: ${e.message}")
        }
    }

    /**
     * Broadcasts my current complete prayer & kaza status to the shared pair topic.
     */
    suspend fun broadcastPrayerStatus(
        codeA: String,
        codeB: String,
        myCode: String,
        myName: String,
        myEmail: String,
        date: String,
        fajr: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String,
        kazaCompleted: Int,
        kazaOwed: Int
    ) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        try {
            val json = JSONObject().apply {
                put("type", "PRAYER_UPDATE")
                put("senderCode", normalizeCode(myCode))
                put("senderName", myName.trim())
                put("senderEmail", myEmail.trim())
                put("date", date)
                put("fajr", fajr)
                put("dhuhr", dhuhr)
                put("asr", asr)
                put("maghrib", maghrib)
                put("isha", isha)
                put("kazaCompleted", kazaCompleted)
                put("kazaOwed", kazaOwed)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast prayer status: ${e.message}")
        }
    }

    suspend fun broadcastPrayerStatusToPool(
        poolCode: String,
        myCode: String,
        myName: String,
        myEmail: String,
        date: String,
        fajr: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String,
        kazaCompleted: Int,
        kazaOwed: Int
    ) = withContext(Dispatchers.IO) {
        val topic = getPoolTopic(poolCode)
        try {
            val json = JSONObject().apply {
                put("type", "PRAYER_UPDATE")
                put("senderCode", normalizeCode(myCode))
                put("senderName", myName.trim())
                put("senderEmail", myEmail.trim())
                put("date", date)
                put("fajr", fajr)
                put("dhuhr", dhuhr)
                put("asr", asr)
                put("maghrib", maghrib)
                put("isha", isha)
                put("kazaCompleted", kazaCompleted)
                put("kazaOwed", kazaOwed)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast prayer status to pool: ${e.message}")
        }
    }

    /**
     * Broadcasts a real-time event (e.g. Dua, prayer completion) to the shared family pool topic.
     */
    suspend fun broadcastPoolEvent(
        poolCode: String,
        myCode: String,
        myName: String,
        eventType: String,
        prayerName: String,
        actionText: String
    ) = withContext(Dispatchers.IO) {
        val clean = normalizeCode(poolCode)
        if (clean.isBlank()) return@withContext
        val topic = getPoolTopic(poolCode)
        try {
            val json = JSONObject().apply {
                put("type", "PARTNER_EVENT")
                put("senderCode", normalizeCode(myCode))
                put("senderName", myName.trim())
                put("eventType", eventType)
                put("prayerName", prayerName)
                put("actionText", actionText)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast pool event: ${e.message}")
        }
    }

    /**
     * Fetches both the latest prayer status and any new events sent by partner or family pool since lastTimestamp.
     */
    suspend fun fetchPartnerUpdatesAndEvents(
        codeA: String,
        codeB: String,
        partnerCode: String,
        myCode: String,
        todayDate: String,
        lastProcessedEventTime: Long,
        poolCode: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {
        val cleanPartner = normalizeCode(partnerCode)
        val cleanMyCode = normalizeCode(myCode)
        val cleanPool = normalizeCode(poolCode)

        val topicsToPoll = mutableListOf<String>()
        if (codeA.isNotBlank() && codeB.isNotBlank() && normalizeCode(codeA) != normalizeCode(codeB)) {
            topicsToPoll.add(getCanonicalPairTopic(codeA, codeB))
        }
        if (cleanPool.isNotBlank()) {
            val pt = getPoolTopic(poolCode)
            if (!topicsToPoll.contains(pt)) {
                topicsToPoll.add(pt)
            }
        }
        if (cleanMyCode.isNotBlank()) {
            val invTopic = "ain_inv_$cleanMyCode"
            if (!topicsToPoll.contains(invTopic)) {
                topicsToPoll.add(invTopic)
            }
            val myPoolTopic = getPoolTopic(myCode)
            if (!topicsToPoll.contains(myPoolTopic)) {
                topicsToPoll.add(myPoolTopic)
            }
        }
        if (topicsToPoll.isEmpty()) {
            return@withContext SyncResult(null, null, emptyList(), false)
        }

        try {
            var latestStatus: PartnerPrayerUpdate? = null
            var yesterdayStatus: PartnerPrayerUpdate? = null
            val newEvents = mutableListOf<PartnerRemoteEvent>()
            var unmatchDetected = false
            var lastClaim: IncomingMatchInfo? = null

            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCal.time)

            for (topic in topicsToPoll) {
                try {
                    val request = Request.Builder()
                        .url("$BASE_URL/$topic/json?poll=1")
                        .get()
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                BufferedReader(StringReader(body)).useLines { lines ->
                                    for (line in lines) {
                                        try {
                                            val obj = JSONObject(line)
                                            if (obj.optString("event") == "message") {
                                                val msgStr = obj.optString("message", "")
                                                val msgJson = JSONObject(msgStr)
                                                val type = msgJson.optString("type", "")
                                                val sender = normalizeCode(msgJson.optString("senderCode", ""))
                                                val timestamp = msgJson.optLong("timestamp", 0L)

                                                when (type) {
                                                    "PRAYER_UPDATE" -> {
                                                        if (sender == cleanPartner || (cleanPool.isNotBlank() && sender != cleanMyCode)) {
                                                            val date = msgJson.optString("date", "")
                                                            if (date == todayDate) {
                                                                if (latestStatus == null || timestamp >= latestStatus!!.timestamp) {
                                                                    latestStatus = PartnerPrayerUpdate(
                                                                        senderCode = formatDisplayCode(sender),
                                                                        senderName = msgJson.optString("senderName", ""),
                                                                        senderEmail = msgJson.optString("senderEmail", ""),
                                                                        date = date,
                                                                        fajr = msgJson.optString("fajr", "NONE"),
                                                                        dhuhr = msgJson.optString("dhuhr", "NONE"),
                                                                        asr = msgJson.optString("asr", "NONE"),
                                                                        maghrib = msgJson.optString("maghrib", "NONE"),
                                                                        isha = msgJson.optString("isha", "NONE"),
                                                                        kazaCompleted = msgJson.optInt("kazaCompleted", 0),
                                                                        kazaOwed = msgJson.optInt("kazaOwed", 0),
                                                                        timestamp = timestamp
                                                                    )
                                                                }
                                                            } else if (date == yesterdayDate) {
                                                                if (yesterdayStatus == null || timestamp >= yesterdayStatus!!.timestamp) {
                                                                    yesterdayStatus = PartnerPrayerUpdate(
                                                                        senderCode = formatDisplayCode(sender),
                                                                        senderName = msgJson.optString("senderName", ""),
                                                                        senderEmail = msgJson.optString("senderEmail", ""),
                                                                        date = date,
                                                                        fajr = msgJson.optString("fajr", "NONE"),
                                                                        dhuhr = msgJson.optString("dhuhr", "NONE"),
                                                                        asr = msgJson.optString("asr", "NONE"),
                                                                        maghrib = msgJson.optString("maghrib", "NONE"),
                                                                        isha = msgJson.optString("isha", "NONE"),
                                                                        kazaCompleted = msgJson.optInt("kazaCompleted", 0),
                                                                        kazaOwed = msgJson.optInt("kazaOwed", 0),
                                                                        timestamp = timestamp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "POOL_EVENT" -> {
                                                        if (sender != cleanMyCode && timestamp > lastProcessedEventTime) {
                                                            val rawAction = msgJson.optString("actionText", "")
                                                            val evType = msgJson.optString("eventType", "")
                                                            val cleanAction = rawAction
                                                                .replace("kazaya bıraktı", "kılmadı")
                                                                .replace("Kazaya bıraktı", "kılmadı")
                                                            val sName = FamilyRoleHelper.normalizeRoleTitle(msgJson.optString("senderName", "Aile Ferdi"))
                                                            val sIcon = msgJson.optString("senderIcon", "")
                                                            val resolvedIcon = if (sIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(sIcon, sName)) {
                                                                sIcon
                                                            } else {
                                                                FamilyRoleHelper.getDefaultIconForRole(sName)
                                                            }
                                                            newEvents.add(
                                                                PartnerRemoteEvent(
                                                                    senderCode = formatDisplayCode(sender),
                                                                    senderName = sName,
                                                                    eventType = if (evType.isNotBlank()) evType else "POOL_EVENT",
                                                                    prayerName = "Aile Havuzu",
                                                                    actionText = cleanAction,
                                                                    timestamp = timestamp,
                                                                    senderIcon = resolvedIcon
                                                                )
                                                            )
                                                        }
                                                    }
                                                    "PARTNER_EVENT" -> {
                                                        if (sender != cleanMyCode && timestamp > lastProcessedEventTime) {
                                                            val rawAction = msgJson.optString("actionText", "")
                                                            val cleanAction = rawAction
                                                                .replace("kazaya bıraktı", "kılmadı")
                                                                .replace("Kazaya bıraktı", "kılmadı")
                                                            val sName = FamilyRoleHelper.normalizeRoleTitle(msgJson.optString("senderName", ""))
                                                            val sIcon = msgJson.optString("senderIcon", "")
                                                            val resolvedIcon = if (sIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(sIcon, sName)) {
                                                                sIcon
                                                            } else {
                                                                FamilyRoleHelper.getDefaultIconForRole(sName)
                                                            }
                                                            newEvents.add(
                                                                PartnerRemoteEvent(
                                                                    senderCode = formatDisplayCode(sender),
                                                                    senderName = sName,
                                                                    eventType = msgJson.optString("eventType", ""),
                                                                    prayerName = msgJson.optString("prayerName", ""),
                                                                    actionText = cleanAction,
                                                                    timestamp = timestamp,
                                                                    senderIcon = resolvedIcon
                                                                )
                                                            )
                                                        }
                                                    }
                                                    "PAIR_CLAIMED" -> {
                                                        val claimerCode = msgJson.optString("claimerCode", "")
                                                        val claimerName = msgJson.optString("claimerName", "")
                                                        val claimerEmail = msgJson.optString("claimerEmail", "")
                                                        val claimerIcon = msgJson.optString("claimerIcon", "")
                                                        if (claimerCode.isNotBlank() && normalizeCode(claimerCode) != cleanMyCode) {
                                                            val normName = FamilyRoleHelper.normalizeRoleTitle(claimerName)
                                                            val resolvedIcon = if (claimerIcon.isNotBlank() && !FamilyRoleHelper.isIconConflictingWithRole(claimerIcon, normName)) {
                                                                claimerIcon
                                                            } else {
                                                                FamilyRoleHelper.getDefaultIconForRole(normName)
                                                            }
                                                            lastClaim = IncomingMatchInfo(
                                                                partnerCode = formatDisplayCode(claimerCode),
                                                                partnerName = normName,
                                                                partnerEmail = claimerEmail,
                                                                partnerIcon = resolvedIcon
                                                            )
                                                        }
                                                    }
                                                    "UNMATCH" -> {
                                                        if (sender == cleanPartner) {
                                                            unmatchDetected = true
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (ignored: Exception) { }
                                    }
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) { }
            }

            SyncResult(
                latestStatus = latestStatus,
                yesterdayStatus = yesterdayStatus,
                newEvents = newEvents,
                partnerUnmatched = unmatchDetected,
                incomingClaim = lastClaim
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync with partner: ${e.message}")
            SyncResult(null, null, emptyList(), false)
        }
    }

    /**
     * Broadcasts unmatch event so the partner can also disconnect.
     */
    suspend fun broadcastUnmatch(codeA: String, codeB: String, myCode: String) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        try {
            val json = JSONObject().apply {
                put("type", "UNMATCH")
                put("senderCode", normalizeCode(myCode))
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast unmatch: ${e.message}")
        }
    }

    /**
     * Otomatik Bulut Yedekleme: Telefon internete bağlandığında veya veriler güncellendiğinde
     * kullanıcının Gmail hesabı ile kaza namazı, günlük kıldı/kılmadı kayıtları ve profil verilerini bulutta güvenle saklar.
     */
    suspend fun backupUserDataToCloud(
        email: String,
        name: String,
        inviteCode: String,
        kazalarJson: String,
        dailyPrayersJson: String = "[]",
        isMatched: Boolean,
        partnerCode: String,
        partnerName: String
    ) = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext

        // SHA-256 hash topic for user's personal private cloud backup
        val safeTopic = "ain_cld_" + cleanEmail.replace("@", "_at_").replace(".", "_")
        try {
            val json = JSONObject().apply {
                put("type", "AUTO_CLOUD_BACKUP")
                put("email", cleanEmail)
                put("name", name.trim())
                put("inviteCode", inviteCode)
                put("isMatched", isMatched)
                put("partnerCode", partnerCode)
                put("partnerName", partnerName)
                put("kazalarJson", kazalarJson)
                put("dailyPrayersJson", dailyPrayersJson)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$safeTopic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
            Log.i(TAG, "Otomatik bulut yedeği başarıyla gönderildi: $cleanEmail")
        } catch (e: Exception) {
            Log.w(TAG, "Otomatik bulut yedekleme hatası: ${e.message}")
        }
    }

    /**
     * Otomatik Bulut Geri Yükleme: Kullanıcı yeni bir telefona geçtiğinde veya uygulamayı
     * yeniden kurup Gmail ile giriş yaptığında buluttan verilerini otomatik indirir.
     */
    suspend fun restoreUserDataFromCloud(email: String): JSONObject? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext null

        val safeTopic = "ain_cld_" + cleanEmail.replace("@", "_at_").replace(".", "_")
        try {
            val request = Request.Builder()
                .url("$BASE_URL/$safeTopic/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var latestBackup: JSONObject? = null
                var latestTime = 0L

                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgJson = JSONObject(obj.optString("message", "{}"))
                                if (msgJson.optString("type") == "AUTO_CLOUD_BACKUP") {
                                    val time = msgJson.optLong("timestamp", 0L)
                                    if (time >= latestTime) {
                                        latestTime = time
                                        latestBackup = msgJson
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                latestBackup
            }
        } catch (e: Exception) {
            Log.w(TAG, "Buluttan otomatik geri yükleme hatası: ${e.message}")
            null
        }
    }
}
