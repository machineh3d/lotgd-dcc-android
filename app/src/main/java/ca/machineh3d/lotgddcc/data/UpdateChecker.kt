package ca.machineh3d.lotgddcc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
)

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String = "",
)

object UpdateChecker {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/machineh3d/lotgd-dcc-android/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "lotgd-dcc-android/$currentVersionName")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching null
                val dto = json.decodeFromString(GitHubReleaseDto.serializer(), body)
                val latest = dto.tagName.trim().removePrefix("v")
                val current = currentVersionName.trim().removePrefix("v")
                if (latest.isNotBlank() && isNewerVersion(latest, current)) {
                    AppUpdateInfo(
                        latestVersion = dto.tagName.trim(),
                        releaseUrl = dto.htmlUrl.ifBlank {
                            "https://github.com/machineh3d/lotgd-dcc-android/releases"
                        },
                        releaseNotes = dto.body.trim().take(240),
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val lNums = extractSemverNumbers(latest)
        val cNums = extractSemverNumbers(current)
        for (i in 0 until maxOf(lNums.size, cNums.size)) {
            val lv = lNums.getOrElse(i) { 0 }
            val cv = cNums.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        // If numeric parts match, a stable release (no "-alpha") is newer than "-alpha"
        val latestPre = latest.contains("-")
        val currentPre = current.contains("-")
        return !latestPre && currentPre
    }

    private fun extractSemverNumbers(version: String): List<Int> {
        val core = version.substringBefore("-")
        return core.split(".").mapNotNull { it.toIntOrNull() }
    }
}
