package ca.machineh3d.lotgddcc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val latestVersion: String) : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val draft: Boolean = false,
    val body: String = "",
)

object UpdateChecker {
    // Use /releases (not /releases/latest) so alpha / prerelease tags are included!
    private const val RELEASES_LIST_API =
        "https://api.github.com/repos/machineh3d/lotgd-dcc-android/releases?per_page=5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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

                if (isNewerVersion(latestClean, currentClean)) {
                    UpdateCheckResult.UpdateAvailable(
                        AppUpdateInfo(
                            latestVersion = latestTag,
                            releaseUrl = latestDto.htmlUrl.ifBlank {
                                "https://github.com/machineh3d/lotgd-dcc-android/releases"
                            },
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
