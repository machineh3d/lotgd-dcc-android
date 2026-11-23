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

enum class AppThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "Auto"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromKey(raw: String?, legacyLight: Boolean): AppThemeMode = when (raw?.lowercase()) {
            "system" -> SYSTEM
            "dark" -> DARK
            "light" -> LIGHT
            else -> if (legacyLight) LIGHT else DARK
        }
    }
}

data class ClientPrefsSnapshot(
    val lastServerUrl: String = "",
    val recentServerUrls: List<String> = emptyList(),
    val rememberCredentials: Boolean = false,
    val savedUsername: String = "",
    val savedPassword: String = "",
    val autoHealEnabled: Boolean = true,
    val autoHealThresholdPercent: Int = 35,
    val newDayNotificationsEnabled: Boolean = false,
    val mailNotificationsEnabled: Boolean = false,
    val pvpNotificationsEnabled: Boolean = false,
    val notificationSoundsEnabled: Boolean = true,
    val lastNotifiedUnreadMail: Int = 0,
    val lastKnownAlive: Boolean = true,
    val combatFlairEnabled: Boolean = true,
    val combatFlairIntensity: Int = 60,
    val combatSoundsEnabled: Boolean = true,
    val menuSoundsEnabled: Boolean = false,
    val pulseTrainIndicator: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val narrativeFontSizeSp: Int = 15,
    val compactCombatLog: Boolean = false,
    val crtScanlinesEnabled: Boolean = false,
    val hudVignetteEnabled: Boolean = false,
    val neonEdgeGlowEnabled: Boolean = false,
    val autoCheckUpdatesEnabled: Boolean = true,
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
        private val KEY_MAIL_NOTIFS = booleanPreferencesKey("mail_notifications_enabled")
        private val KEY_PVP_NOTIFS = booleanPreferencesKey("pvp_notifications_enabled")
        private val KEY_NOTIF_SOUNDS = booleanPreferencesKey("notification_sounds_enabled")
        private val KEY_LAST_UNREAD_MAIL = intPreferencesKey("last_notified_unread_mail")
        private val KEY_LAST_KNOWN_ALIVE = booleanPreferencesKey("last_known_alive")
        private val KEY_COMBAT_FLAIR = booleanPreferencesKey("combat_flair_enabled")
        private val KEY_COMBAT_FLAIR_INTENSITY = intPreferencesKey("combat_flair_intensity")
        private val KEY_COMBAT_SOUNDS = booleanPreferencesKey("combat_sounds_enabled")
        private val KEY_MENU_SOUNDS = booleanPreferencesKey("menu_sounds_enabled")
        private val KEY_PULSE_TRAIN = booleanPreferencesKey("pulse_train_indicator")
        private val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        private val KEY_USE_LIGHT_THEME = booleanPreferencesKey("use_light_theme")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_NARRATIVE_FONT_SP = intPreferencesKey("narrative_font_size_sp")
        private val KEY_COMPACT_COMBAT_LOG = booleanPreferencesKey("compact_combat_log")
        private val KEY_CRT_SCANLINES = booleanPreferencesKey("crt_scanlines_enabled")
        private val KEY_HUD_VIGNETTE = booleanPreferencesKey("hud_vignette_enabled")
        private val KEY_NEON_EDGE_GLOW = booleanPreferencesKey("neon_edge_glow_enabled")
        private val KEY_AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates_enabled")
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
        val legacyLight = prefs[KEY_USE_LIGHT_THEME] ?: false

        ClientPrefsSnapshot(
            lastServerUrl = lastUrl,
            recentServerUrls = recentList,
            rememberCredentials = remember,
            savedUsername = prefs[KEY_LAST_USERNAME].orEmpty(),
            savedPassword = if (remember) prefs[KEY_SAVED_PASSWORD].orEmpty() else "",
            autoHealEnabled = prefs[KEY_AUTO_HEAL_ENABLED] ?: true,
            autoHealThresholdPercent = (prefs[KEY_AUTO_HEAL_THRESHOLD] ?: 35).coerceIn(10, 90),
            newDayNotificationsEnabled = prefs[KEY_NEW_DAY_NOTIFS] ?: false,
            mailNotificationsEnabled = prefs[KEY_MAIL_NOTIFS] ?: false,
            pvpNotificationsEnabled = prefs[KEY_PVP_NOTIFS] ?: false,
            notificationSoundsEnabled = prefs[KEY_NOTIF_SOUNDS] ?: true,
            lastNotifiedUnreadMail = (prefs[KEY_LAST_UNREAD_MAIL] ?: 0).coerceAtLeast(0),
            lastKnownAlive = prefs[KEY_LAST_KNOWN_ALIVE] ?: true,
            combatFlairEnabled = prefs[KEY_COMBAT_FLAIR] ?: true,
            combatFlairIntensity = (prefs[KEY_COMBAT_FLAIR_INTENSITY] ?: 60).coerceIn(20, 100),
            combatSoundsEnabled = prefs[KEY_COMBAT_SOUNDS] ?: true,
            menuSoundsEnabled = prefs[KEY_MENU_SOUNDS] ?: false,
            pulseTrainIndicator = prefs[KEY_PULSE_TRAIN] ?: true,
            keepScreenAwake = prefs[KEY_KEEP_SCREEN_AWAKE] ?: false,
            themeMode = AppThemeMode.fromKey(prefs[KEY_THEME_MODE], legacyLight),
            narrativeFontSizeSp = (prefs[KEY_NARRATIVE_FONT_SP] ?: 15).coerceIn(13, 20),
            compactCombatLog = prefs[KEY_COMPACT_COMBAT_LOG] ?: false,
            crtScanlinesEnabled = prefs[KEY_CRT_SCANLINES] ?: false,
            hudVignetteEnabled = prefs[KEY_HUD_VIGNETTE] ?: false,
            neonEdgeGlowEnabled = prefs[KEY_NEON_EDGE_GLOW] ?: false,
            autoCheckUpdatesEnabled = prefs[KEY_AUTO_CHECK_UPDATES] ?: true,
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
        context.dataStore.edit { prefs -> prefs[KEY_NEW_DAY_NOTIFS] = enabled }
    }

    suspend fun saveMailNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MAIL_NOTIFS] = enabled }
    }

    suspend fun savePvpNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_PVP_NOTIFS] = enabled }
    }

    suspend fun saveNotificationSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_NOTIF_SOUNDS] = enabled }
    }

    suspend fun saveLastNotifiedUnreadMail(count: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_UNREAD_MAIL] = count.coerceAtLeast(0) }
    }

    suspend fun saveLastKnownAlive(alive: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_KNOWN_ALIVE] = alive }
    }

    suspend fun saveCombatFlairSettings(enabled: Boolean, intensity: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMBAT_FLAIR] = enabled
            prefs[KEY_COMBAT_FLAIR_INTENSITY] = intensity.coerceIn(20, 100)
        }
    }

    suspend fun saveCombatSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_COMBAT_SOUNDS] = enabled }
    }

    suspend fun saveMenuSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MENU_SOUNDS] = enabled }
    }

    suspend fun savePulseTrainIndicator(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_PULSE_TRAIN] = enabled }
    }

    suspend fun saveKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.storageKey
            prefs[KEY_USE_LIGHT_THEME] = (mode == AppThemeMode.LIGHT)
        }
    }

    suspend fun saveNarrativeFontSizeSp(sizeSp: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_NARRATIVE_FONT_SP] = sizeSp.coerceIn(13, 20) }
    }

    suspend fun saveCompactCombatLog(compact: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_COMPACT_COMBAT_LOG] = compact }
    }

    suspend fun saveCrtScanlinesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_CRT_SCANLINES] = enabled }
    }

    suspend fun saveHudVignetteEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_HUD_VIGNETTE] = enabled }
    }

    suspend fun saveNeonEdgeGlowEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_NEON_EDGE_GLOW] = enabled }
    }

    suspend fun saveAutoCheckUpdatesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_CHECK_UPDATES] = enabled }
    }

    fun normalizeUrl(raw: String): String {
        var trimmed = raw.trim().trimEnd('/')
        if (trimmed.isNotEmpty() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
