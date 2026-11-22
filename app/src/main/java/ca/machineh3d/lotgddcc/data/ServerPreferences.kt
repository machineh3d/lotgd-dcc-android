package ca.machineh3d.lotgddcc.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dcc_client_prefs")

data class ClientPrefsSnapshot(
    val lastServerUrl: String = "",
    val recentServerUrls: List<String> = emptyList(),
    val rememberCredentials: Boolean = false,
    val savedUsername: String = "",
    val savedPassword: String = "",
    val autoHealEnabled: Boolean = true,
    val autoHealThresholdPercent: Int = 35,
    val newDayNotificationsEnabled: Boolean = false,
    val combatFlairEnabled: Boolean = true,
)

class ServerPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_RECENT_URLS = stringPreferencesKey("recent_server_urls")
        private val KEY_REMEMBER_CREDS = booleanPreferencesKey("remember_credentials")
        private val KEY_LAST_USERNAME = stringPreferencesKey("last_username")
        private val KEY_SAVED_PASSWORD = stringPreferencesKey("saved_password")
        private val KEY_AUTO_HEAL_ENABLED = booleanPreferencesKey("auto_heal_enabled")
        private val KEY_AUTO_HEAL_THRESHOLD = intPreferencesKey("auto_heal_threshold")
        private val KEY_NEW_DAY_NOTIFS = booleanPreferencesKey("new_day_notifications_enabled")
        private val KEY_COMBAT_FLAIR = booleanPreferencesKey("combat_flair_enabled")
        private const val URL_DELIMITER = "\n"
        private const val MAX_RECENT_URLS = 8
    }

    val prefsFlow: Flow<ClientPrefsSnapshot> = context.dataStore.data.map { prefs ->
        val rawRecent = prefs[KEY_RECENT_URLS].orEmpty()
        val recentList = rawRecent
            .split(URL_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val lastUrl = prefs[KEY_SERVER_URL].orEmpty().ifBlank { recentList.firstOrNull().orEmpty() }
        val remember = prefs[KEY_REMEMBER_CREDS] ?: false

        ClientPrefsSnapshot(
            lastServerUrl = lastUrl,
            recentServerUrls = recentList,
            rememberCredentials = remember,
            savedUsername = prefs[KEY_LAST_USERNAME].orEmpty(),
            savedPassword = if (remember) prefs[KEY_SAVED_PASSWORD].orEmpty() else "",
            autoHealEnabled = prefs[KEY_AUTO_HEAL_ENABLED] ?: true,
            autoHealThresholdPercent = (prefs[KEY_AUTO_HEAL_THRESHOLD] ?: 35).coerceIn(10, 90),
            newDayNotificationsEnabled = prefs[KEY_NEW_DAY_NOTIFS] ?: false,
            combatFlairEnabled = prefs[KEY_COMBAT_FLAIR] ?: true,
        )
    }

    suspend fun saveConnection(
        serverUrl: String,
        username: String,
        password: String,
        rememberCredentials: Boolean,
    ) {
        val normalized = normalizeUrl(serverUrl)
        context.dataStore.edit { prefs ->
            if (normalized.isNotBlank()) {
                prefs[KEY_SERVER_URL] = normalized
                val existing = prefs[KEY_RECENT_URLS]
                    .orEmpty()
                    .split(URL_DELIMITER)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals(normalized, ignoreCase = true) }
                val updated = (listOf(normalized) + existing).take(MAX_RECENT_URLS)
                prefs[KEY_RECENT_URLS] = updated.joinToString(URL_DELIMITER)
            }
            prefs[KEY_REMEMBER_CREDS] = rememberCredentials
            if (username.isNotBlank()) {
                prefs[KEY_LAST_USERNAME] = username.trim()
            }
            if (rememberCredentials) {
                prefs[KEY_SAVED_PASSWORD] = password
            } else {
                prefs.remove(KEY_SAVED_PASSWORD)
            }
        }
    }

    suspend fun saveServerUrl(serverUrl: String) {
        val normalized = normalizeUrl(serverUrl)
        if (normalized.isBlank()) return
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalized
            val existing = prefs[KEY_RECENT_URLS]
                .orEmpty()
                .split(URL_DELIMITER)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(normalized, ignoreCase = true) }
            val updated = (listOf(normalized) + existing).take(MAX_RECENT_URLS)
            prefs[KEY_RECENT_URLS] = updated.joinToString(URL_DELIMITER)
        }
    }

    suspend fun removeRecentUrl(urlToRemove: String) {
        context.dataStore.edit { prefs ->
            val updated = prefs[KEY_RECENT_URLS]
                .orEmpty()
                .split(URL_DELIMITER)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(urlToRemove, ignoreCase = true) }
            prefs[KEY_RECENT_URLS] = updated.joinToString(URL_DELIMITER)
        }
    }

    suspend fun saveAutoHealSettings(enabled: Boolean, thresholdPercent: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_HEAL_ENABLED] = enabled
            prefs[KEY_AUTO_HEAL_THRESHOLD] = thresholdPercent.coerceIn(10, 90)
        }
    }

    suspend fun saveNewDayNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NEW_DAY_NOTIFS] = enabled
        }
    }

    suspend fun saveCombatFlairEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMBAT_FLAIR] = enabled
        }
    }

    fun normalizeUrl(raw: String): String {
        var trimmed = raw.trim().trimEnd('/')
        if (trimmed.isNotEmpty() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
