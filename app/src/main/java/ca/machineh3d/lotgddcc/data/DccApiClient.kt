package ca.machineh3d.lotgddcc.data

import android.content.Context
import ca.machineh3d.lotgddcc.model.DccPageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Persistent CookieJar backed by SharedPreferences so LotGD PHP sessions
 * survive app restarts.
 */
private class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("dcc_cookies", Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existing = cache.getOrPut(host) { mutableListOf() }
        for (incoming in cookies) {
            existing.removeAll { it.name == incoming.name && it.path == incoming.path }
            if (incoming.expiresAt > System.currentTimeMillis()) {
                existing.add(incoming)
            }
        }
        persistHost(host, existing)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val list = cache.getOrPut(host) { loadHost(host, url).toMutableList() }
        val now = System.currentTimeMillis()
        list.removeAll { it.expiresAt <= now }
        return list.filter { it.matches(url) }
    }

    fun clearAll() {
        cache.clear()
        prefs.edit().clear().apply()
    }

    private fun persistHost(host: String, cookies: List<Cookie>) {
        val serialized = cookies.map { "${it.name}=${it.value}; path=${it.path}" }.toSet()
        prefs.edit().putStringSet(host, serialized).apply()
    }

    private fun loadHost(host: String, url: HttpUrl): List<Cookie> {
        val stored = prefs.getStringSet(host, emptySet()) ?: emptySet()
        return stored.mapNotNull { Cookie.parse(url, it) }
    }
}


data class FriendsStatusSnapshot(
    val online: Int = 0,
    val total: Int = 0,
    val pending: Int = 0,
)

class DccApiClient(context: Context) {

    private val cookieJar = PersistentCookieJar(context.applicationContext)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("X-DCC-Client", "1")
                .header("Accept", "application/json")
                .header("User-Agent", "LotGD-DCC-Android/0.1.0")
                .build()
            chain.proceed(req)
        }
        .build()


    /**
     * Read-only live poll for dcc_friends_poll.php (never touches allowednavs/badnav).
     */
    suspend fun fetchFriendsPoll(baseUrl: String): FriendsStatusSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val resolved = resolveUrl(baseUrl, "dcc_friends_poll.php")
            val request = Request.Builder().url(resolved).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string()?.trim().orEmpty()
                if (!body.startsWith("{")) return@runCatching null
                val obj = json.parseToJsonElement(body).jsonObject
                if (obj["ok"]?.jsonPrimitive?.content == "false") return@runCatching null
                val online = obj["online"]?.jsonPrimitive?.intOrNull
                    ?: obj["onlineCount"]?.jsonPrimitive?.intOrNull
                    ?: obj["online_count"]?.jsonPrimitive?.intOrNull
                    ?: 0
                val total = obj["total"]?.jsonPrimitive?.intOrNull
                    ?: obj["totalCount"]?.jsonPrimitive?.intOrNull
                    ?: obj["total_count"]?.jsonPrimitive?.intOrNull
                    ?: 0
                val pending = obj["pending"]?.jsonPrimitive?.intOrNull
                    ?: obj["pendingCount"]?.jsonPrimitive?.intOrNull
                    ?: obj["requests"]?.jsonPrimitive?.intOrNull
                    ?: 0
                FriendsStatusSnapshot(online = online, total = total, pending = pending)
            }
        }.getOrNull()
    }

    fun clearSession() {
        cookieJar.clearAll()
    }

    suspend fun fetchPage(baseUrl: String, relativeOrAbsoluteUrl: String = "home.php"): DccPageResponse {
        val resolved = resolveUrl(baseUrl, relativeOrAbsoluteUrl)
        val request = Request.Builder().url(resolved).get().build()
        return executeAndDecode(request)
    }

    suspend fun login(baseUrl: String, username: String, password: String): DccPageResponse {
        val resolved = resolveUrl(baseUrl, "login.php")
        val body = FormBody.Builder()
            .add("name", username.trim())
            .add("password", password)
            .build()
        val request = Request.Builder().url(resolved).post(body).build()
        return executeAndDecode(request)
    }

    suspend fun submitForm(
        baseUrl: String,
        currentPageUrl: String,
        action: String,
        method: String,
        fields: Map<String, String>,
    ): DccPageResponse {
        val target = if (action.isBlank()) currentPageUrl else action
        val resolved = resolveUrl(baseUrl, target)

        val request = if (method.equals("GET", ignoreCase = true)) {
            val urlBuilder = resolved.toHttpUrlOrNull()?.newBuilder()
                ?: throw IOException("Invalid form URL: $resolved")
            for ((k, v) in fields) {
                urlBuilder.addQueryParameter(k, v)
            }
            Request.Builder().url(urlBuilder.build()).get().build()
        } else {
            val formBuilder = FormBody.Builder()
            for ((k, v) in fields) {
                formBuilder.add(k, v)
            }
            Request.Builder().url(resolved).post(formBuilder.build()).build()
        }

        return executeAndDecode(request)
    }

    private suspend fun executeAndDecode(request: Request): DccPageResponse = withContext(Dispatchers.IO) {
        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Server returned HTTP ${response.code} for ${request.url.encodedPath}")
            }
            val trimmed = bodyStr.trimStart()
            if (!trimmed.startsWith("{")) {
                throw IOException(
                    "Server returned HTML instead of JSON. Install dcc_api_bridge.php on your lotgd-dcc server and try again."
                )
            }
            json.decodeFromString(DccPageResponse.serializer(), trimmed)
        }
    }

    private fun resolveUrl(baseUrl: String, target: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (target.startsWith("http://") || target.startsWith("https://")) {
            return target
        }
        val baseHttp = "$cleanBase/".toHttpUrlOrNull()
            ?: throw IOException("Invalid server URL: $baseUrl")
        val resolved = baseHttp.resolve(target.trimStart('/'))
            ?: throw IOException("Could not resolve URL: $target")
        return resolved.toString()
    }
}
