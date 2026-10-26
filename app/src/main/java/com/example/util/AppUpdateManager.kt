package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeFormatted: String,
    val publishedAt: String,
    val assetUpdatedAtMillis: Long
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/bilkil53/aile-ile-namaz/releases/latest"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_NOTIFIED_TAG = "last_notified_version_tag"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val GITHUB_LATEST_WEB_URL = "https://github.com/bilkil53/aile-ile-namaz/releases/latest"

    const val DEFAULT_FULL_RELEASE_NOTES = """🕌 Aile ile Namaz v1.8 - Resmi Güncel Sürüm Yenilikleri:

🔒 Giriş Yapılmadığında Aile Havuzu ve Başlık Düzeltmesi: Giriş yapılmadığında tabloda ve havuz kartında cache'de kalan ("BaBa Ailesi", "Eşi Ailesi" vb.) isimler kaldırıldı; tablo başlığı "Aile İbadet Halkası" olarak sabitlendi. Giriş yapılmadığında rol veya kişi düzenleme butonları gizlendi.
⏰ Tam Vaktinde ve Doğru Konumlu Diyanet Bildirimleri: Namaz öncesi hatırlatmalar artık bulunduğunuz ilin resmi Diyanet namaz vakitlerine ve gerçek duvar saatine göre tam zamanında hesaplanır; yanlış saatte veya erken gelen bildirim sorunu kökten giderildi.
📍 81 İl Diyanet Seçimi & Güvenilir GPS: Türkiye'nin tüm 81 ili için Diyanet vakitleri ve tek tıkla il seçme penceresi eklendi; GPS sinyali zayıf olsa dahi seçili iliniz asla varsayılan şehre sıfırlanmaz.
👨‍👩‍👧‍👦 Ortak Aile İbadet Tablosu: Giriş yapıldığında tüm fertler tek tabloda eksiksiz görünür; giriş yapılmadığında yalnızca yerel ibadet durumunuz temiz bir şekilde sunulur.
🔔 Dinamik Bildirim Başlıkları: Bildirimler kaç dakika kaldığını gerçek zamanlı ve doğru olarak bildirir (örn: Vaktine 10 Dakika Kaldı).
🚀 Doğrudan Uygulama İçi Güncelleme: v1.8 sürümü doğrudan tespit edilir ve kurulur."""

    suspend fun checkForUpdates(context: Context, notifyIfFound: Boolean = true): AppUpdateInfo? = withContext(Dispatchers.IO) {
        // 1. Birinci Aşama: GitHub REST API ile sorgula (Tam detaylar ve sürüm notları)
        val apiResult = checkViaGitHubApi(context, notifyIfFound)
        if (apiResult != null) {
            return@withContext apiResult
        }

        // 2. İkinci Aşama (Kalıcı Çözüm): API limitine veya hataya takılırsa Web 302 yönlendirmesiyle sorgula (Sıfır limit, kesintisiz)
        Log.i(TAG, "API sorgusu başarısız, web 302 yönlendirme sorgusuna geçiliyor...")
        return@withContext checkViaWebRedirect(context, notifyIfFound)
    }

    private fun checkViaGitHubApi(context: Context, notifyIfFound: Boolean): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AileIleNamaz-Android")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check API returned code: ${response.code}")
                    return null
                }

                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)

                val rawTagName = json.optString("tag_name", "").trim()
                val tagName = cleanVersionTag(rawTagName)
                val rawReleaseName = json.optString("name", "").trim()
                val releaseName = if (rawReleaseName.isNotBlank()) cleanReleaseTitle(rawReleaseName) else "Aile ile Namaz $tagName - Resmi Güncel Sürüm"
                val rawNotes = json.optString("body", "").trim()
                val cleanNotes = cleanReleaseTitle(rawNotes)
                val releaseNotes = if (cleanNotes.length >= 25) cleanNotes else DEFAULT_FULL_RELEASE_NOTES
                val publishedAt = json.optString("published_at", "")

                var downloadUrl = ""
                var apkSizeBytes = 0L
                var assetUpdatedAtMillis = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkSizeBytes = asset.optLong("size", 0L)
                            val updatedAtStr = asset.optString("updated_at", "")
                            assetUpdatedAtMillis = parseIsoDateToMillis(updatedAtStr)
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank()) {
                    downloadUrl = "https://github.com/bilkil53/aile-ile-namaz/releases/latest/download/Aile_Ile_Namaz.apk"
                }

                val formattedSize = if (apkSizeBytes > 0) {
                    String.format(Locale.getDefault(), "%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
                } else {
                    "25 MB"
                }

                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
                } catch (_: Exception) {
                    BuildConfig.VERSION_NAME
                }
                val currentVersionTag = cleanVersionTag(currentVersion)

                val isNewerVersionTag = tagName.isNotBlank() && isTagHigher(tagName, currentVersionTag)
                val hasUpdate = isNewerVersionTag

                val updateInfo = AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionTag = if (tagName.isNotBlank()) tagName else currentVersionTag,
                    releaseTitle = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = downloadUrl,
                    apkSizeFormatted = formattedSize,
                    publishedAt = publishedAt,
                    assetUpdatedAtMillis = assetUpdatedAtMillis
                )

                if (hasUpdate && notifyIfFound) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_TAG, "")

                    if (lastNotified != tagName) {
                        prefs.edit().putString(KEY_LAST_NOTIFIED_TAG, tagName).apply()
                        NotificationHelper.showAppUpdateNotification(
                            context = context,
                            versionTag = updateInfo.latestVersionTag,
                            releaseNotes = releaseNotes.take(150)
                        )
                    }
                }

                updateInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkViaGitHubApi error: ${e.message}")
            null
        }
    }

    private fun checkViaWebRedirect(context: Context, notifyIfFound: Boolean): AppUpdateInfo? {
        return try {
            val noRedirectClient = httpClient.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

            val request = Request.Builder()
                .url(GITHUB_LATEST_WEB_URL)
                .header("User-Agent", "AileIleNamaz-Android")
                .head()
                .build()

            noRedirectClient.newCall(request).execute().use { response ->
                val location = response.header("Location") ?: ""
                val tagName = if (location.contains("/tag/")) {
                    location.substringAfterLast("/tag/").trim().removeSuffix("/")
                } else ""

                val cleanTag = cleanVersionTag(tagName)
                if (cleanTag.isBlank()) {
                    Log.w(TAG, "Web redirect Location did not contain tag: $location")
                    return null
                }

                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
                } catch (_: Exception) {
                    BuildConfig.VERSION_NAME
                }
                val currentVersionTag = cleanVersionTag(currentVersion)
                val hasUpdate = isTagHigher(cleanTag, currentVersionTag)

                val downloadUrl = "https://github.com/bilkil53/aile-ile-namaz/releases/download/$cleanTag/Aile_Ile_Namaz.apk"

                val updateInfo = AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionTag = cleanTag,
                    releaseTitle = "Aile ile Namaz $cleanTag - Resmi Güncel Sürüm",
                    releaseNotes = DEFAULT_FULL_RELEASE_NOTES,
                    apkDownloadUrl = downloadUrl,
                    apkSizeFormatted = "25 MB",
                    publishedAt = "",
                    assetUpdatedAtMillis = System.currentTimeMillis()
                )

                if (hasUpdate && notifyIfFound) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_TAG, "")
                    if (lastNotified != tagName) {
                        prefs.edit().putString(KEY_LAST_NOTIFIED_TAG, tagName).apply()
                        NotificationHelper.showAppUpdateNotification(
                            context = context,
                            versionTag = updateInfo.latestVersionTag,
                            releaseNotes = updateInfo.releaseNotes.take(150)
                        )
                    }
                }

                updateInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkViaWebRedirect error: ${e.message}")
            null
        }
    }

    private fun parseIsoDateToMillis(isoDateStr: String): Long {
        if (isoDateStr.isBlank()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoDateStr)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun cleanVersionTag(tag: String): String {
        var t = tag.trim()
        if (t.contains("(")) {
            t = t.substringBefore("(")
        }
        if (t.contains("-")) {
            t = t.substringBefore("-")
        }
        t = t.trim()
        t = t.removePrefix("v.").removePrefix("V.").removePrefix("v").removePrefix("V").trim()
        return if (t.isNotBlank()) "v$t" else "v1.6"
    }

    fun cleanReleaseTitle(title: String): String {
        var clean = title
            .replace(Regex("(?i)\\s*\\(?build\\s*\\d+\\)?"), "")
            .replace(Regex("(?i)\\s*build\\s*\\d+"), "")
            .trim()
        return clean
    }

    private fun isTagHigher(latestTag: String, currentTag: String): Boolean {
        val cleanLatest = cleanVersionTag(latestTag).removePrefix("v").trim()
        val cleanCurrent = cleanVersionTag(currentTag).removePrefix("v").trim()

        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) {
            return false
        }

        val latestParts = cleanLatest.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun isTrustedGitHubDownloadUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase() ?: return false
            scheme == "https" && (
                host == "github.com" ||
                host.endsWith(".github.com") ||
                host == "githubusercontent.com" ||
                host.endsWith(".githubusercontent.com")
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Downloads the APK file from GitHub and triggers direct Android package installation.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!isTrustedGitHubDownloadUrl(downloadUrl)) {
            withContext(Dispatchers.Main) {
                onError("Güvenlik Uyarısı: İndirme adresi doğrulanmış güvenli GitHub sunucusu değil.")
            }
            return@withContext
        }

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "AileIleNamaz-Android")
                .get()
                .build()

            val downloadClient = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("İndirme başarısız oldu (Hata kodu: ${response.code})")
                }
                return@withContext
            }

            val body = response.body
            if (body == null) {
                withContext(Dispatchers.Main) {
                    onError("İndirilecek APK dosyası bulunamadı.")
                }
                return@withContext
            }

            val totalBytes = body.contentLength()
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(targetDir, "Aile_Ile_Namaz.apk")

            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val progress = bytesCopied.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                onComplete(apkFile)
                triggerInstall(context, apkFile, downloadUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError("İndirme sırasında hata oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}")
            }
        }
    }

    private const val KEY_PENDING_APK_PATH = "pending_install_apk_path"

    /**
     * Launches the Android native package installer for the downloaded APK.
     */
    fun triggerInstall(context: Context, apkFile: File, fallbackUrl: String) {
        try {
            // Check unknown sources installation permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(KEY_PENDING_APK_PATH, apkFile.absolutePath).apply()

                    Toast.makeText(
                        context,
                        "Güncellemeyi kurabilmek için lütfen 'Bu kaynaktan izin ver' seçeneğini açıp uygulamaya geri dönün.",
                        Toast.LENGTH_LONG
                    ).show()

                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            launchApkInstall(context, apkFile, fallbackUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed, opening browser: ${e.message}")
            openInBrowser(context, fallbackUrl)
        }
    }

    fun launchApkInstall(context: Context, apkFile: File, fallbackUrl: String = GITHUB_LATEST_WEB_URL) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "launchApkInstall failed: ${e.message}")
            Toast.makeText(context, "Doğrudan kurulum açılamadı. İndirme sayfası açılıyor...", Toast.LENGTH_SHORT).show()
            openInBrowser(context, fallbackUrl)
        }
    }

    /**
     * Checks if there is a downloaded APK waiting to be installed after returning from permission settings.
     */
    fun checkAndResumePendingInstall(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    return
                }
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingPath = prefs.getString(KEY_PENDING_APK_PATH, null) ?: return
            val apkFile = File(pendingPath)
            if (apkFile.exists() && apkFile.length() > 0) {
                prefs.edit().remove(KEY_PENDING_APK_PATH).apply()
                launchApkInstall(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndResumePendingInstall error: ${e.message}")
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) { }
    }
}
