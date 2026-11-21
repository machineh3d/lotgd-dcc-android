package ca.machineh3d.lotgddcc.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dcc_client_prefs")

class ServerPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_LAST_USERNAME = stringPreferencesKey("last_username")
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: ""
    }

    val lastUsernameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_USERNAME] ?: ""
    }

    suspend fun saveConnection(serverUrl: String, username: String) {
        val normalized = normalizeUrl(serverUrl)
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalized
            if (username.isNotBlank()) {
                prefs[KEY_LAST_USERNAME] = username.trim()
            }
        }
    }

    suspend fun saveServerUrl(serverUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalizeUrl(serverUrl)
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
