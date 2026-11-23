package ca.machineh3d.lotgddcc.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?,
    val releaseNotes: String,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val latestVersion: String) : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}

@Serializable
private data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val draft: Boolean = false,
    val body: String = "",
    val assets: List<GitHubAssetDto> = emptyList(),
)

object UpdateChecker {
    private const val RELEASES_LIST_API =
        "https://api.github.com/repos/machineh3d/lotgd-dcc-android/releases?per_page=5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(RELEASES_LIST_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "lotgd-dcc-android/$currentVersionName")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext UpdateCheckResult.Failed("GitHub returned HTTP ${resp.code}")
                }
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext UpdateCheckResult.Failed("Empty response from GitHub")
                }
                val releases = json.decodeFromString(ListSerializer(GitHubReleaseDto.serializer()), body)
                val latestDto = releases.firstOrNull { !it.draft && it.tagName.isNotBlank() }
                    ?: return@withContext UpdateCheckResult.Failed("No published releases found")

                val latestTag = latestDto.tagName.trim()
                val latestClean = latestTag.removePrefix("v")
                val currentClean = currentVersionName.trim().removePrefix("v")

                val apkUrl = latestDto.assets
                    .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?.browserDownloadUrl
                    ?.takeIf { it.isNotBlank() }

                if (isNewerVersion(latestClean, currentClean)) {
                    UpdateCheckResult.UpdateAvailable(
                        AppUpdateInfo(
                            latestVersion = latestTag,
                            releaseUrl = latestDto.htmlUrl.ifBlank {
                                "https://github.com/machineh3d/lotgd-dcc-android/releases"
                            },
                            apkDownloadUrl = apkUrl,
                            releaseNotes = latestDto.body.trim().take(240),
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(latestTag)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Could not reach GitHub")
        }
    }

    /**
     * Downloads the release APK into app cache/updates/ with live progress (0f..1f)
     * and returns the downloaded File ready for FileProvider installation.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "lotgd-dcc-android-update.apk")
        if (targetFile.exists()) targetFile.delete()

        val req = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", "lotgd-dcc-android-updater")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Download failed (HTTP ${resp.code})")
            }
            val body = resp.body ?: throw IOException("Empty APK response")
            val totalBytes = body.contentLength()
            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    var readSoFar = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readSoFar += n
                        if (totalBytes > 0) {
                            onProgress((readSoFar.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        }
        onProgress(1f)
        targetFile
    }

    /**
     * Launches Android's native package installer for a downloaded APK via FileProvider.
     */
    fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val lNums = extractSemverNumbers(latest)
        val cNums = extractSemverNumbers(current)
        for (i in 0 until maxOf(lNums.size, cNums.size)) {
            val lv = lNums.getOrElse(i) { 0 }
            val cv = cNums.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        val latestPre = latest.contains("-")
        val currentPre = current.contains("-")
        return !latestPre && currentPre
    }

    private fun extractSemverNumbers(version: String): List<Int> {
        val core = version.substringBefore("-")
        return core.split(".").mapNotNull { it.toIntOrNull() }
    }
}
