package ca.machineh3d.lotgddcc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.machineh3d.lotgddcc.data.DccApiClient
import ca.machineh3d.lotgddcc.data.ServerPreferences
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.notifications.MailNotificationScheduler
import ca.machineh3d.lotgddcc.notifications.NewDayScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val serverUrl: String = "",
    val recentServerUrls: List<String> = emptyList(),
    val rememberCredentials: Boolean = false,
    val savedUsername: String = "",
    val savedPassword: String = "",
    val autoHealEnabled: Boolean = true,
    val autoHealThresholdPercent: Int = 35,
    val newDayNotificationsEnabled: Boolean = false,
    val mailNotificationsEnabled: Boolean = false,
    val combatFlairEnabled: Boolean = true,
    val useLightTheme: Boolean = false,
    val narrativeFontSizeSp: Int = 15,
    val compactCombatLog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val page: DccPageResponse? = null,
    val isInCreateFlow: Boolean = false,
    val showStatsSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = ServerPreferences(application)
    private val api = DccApiClient(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastKnownUnreadMail: Int = 0
    private var lastFailedUrl: String? = null
    private var lastFailedForm: Pair<GameForm, Map<String, String>>? = null
    private var pendingOpenMailOnResume: Boolean = false

    init {
        viewModelScope.launch {
            val snap = prefs.prefsFlow.first()
            lastKnownUnreadMail = snap.lastNotifiedUnreadMail
            _uiState.update {
                it.copy(
                    serverUrl = snap.lastServerUrl,
                    recentServerUrls = snap.recentServerUrls,
                    rememberCredentials = snap.rememberCredentials,
                    savedUsername = snap.savedUsername,
                    savedPassword = snap.savedPassword,
                    autoHealEnabled = snap.autoHealEnabled,
                    autoHealThresholdPercent = snap.autoHealThresholdPercent,
                    newDayNotificationsEnabled = snap.newDayNotificationsEnabled,
                    mailNotificationsEnabled = snap.mailNotificationsEnabled,
                    combatFlairEnabled = snap.combatFlairEnabled,
                    useLightTheme = snap.useLightTheme,
                    narrativeFontSizeSp = snap.narrativeFontSizeSp,
                    compactCombatLog = snap.compactCombatLog,
                )
            }
            if (snap.mailNotificationsEnabled) {
                MailNotificationScheduler.startPeriodicPolling(appContext)
            }
            if (snap.lastServerUrl.isNotBlank()) {
                resumeSession(snap.lastServerUrl)
            }
        }
    }

    fun requestOpenMailFromNotification() {
        val currentPage = _uiState.value.page
        if (currentPage?.player?.loggedIn == true) {
            openSyndicateComms()
        } else {
            pendingOpenMailOnResume = true
        }
    }

    private fun syncNewDayAlarm(resp: DccPageResponse) {
        if (_uiState.value.newDayNotificationsEnabled && resp.nextNewDaySeconds > 0) {
            NewDayScheduler.scheduleNextNewDay(
                context = appContext,
                delaySeconds = resp.nextNewDaySeconds,
                secondsPerGameDay = resp.secondsPerGameDay,
            )
        }
    }

    private fun syncMailNotification(resp: DccPageResponse) {
        if (resp.player?.loggedIn != true) return
        val unread = resp.mail.unread
        if (_uiState.value.mailNotificationsEnabled && unread > lastKnownUnreadMail) {
            MailNotificationScheduler.postMailNotification(appContext, unread)
        }
        if (unread != lastKnownUnreadMail) {
            lastKnownUnreadMail = unread
            viewModelScope.launch {
                prefs.saveLastNotifiedUnreadMail(unread)
            }
        }
    }

    private fun resumeSession(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canRetry = false) }
            try {
                val startTarget = if (pendingOpenMailOnResume) "mail.php" else "village.php"
                pendingOpenMailOnResume = false
                val resp = api.fetchPage(url, startTarget)
                if (resp.player?.loggedIn == true) {
                    syncNewDayAlarm(resp)
                    syncMailNotification(resp)
                    _uiState.update { it.copy(isLoading = false, page = resp) }
                } else {
                    _uiState.update { it.copy(isLoading = false, page = null) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, page = null) }
            }
        }
    }

    fun login(
        serverUrl: String,
        username: String,
        password: String,
        rememberCredentials: Boolean,
    ) {
        val normalized = prefs.normalizeUrl(serverUrl)
        if (normalized.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your lotgd-dcc server URL first.") }
            return
        }
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both your Crawler login and password.") }
            return
        }

        viewModelScope.launch {
            prefs.saveConnection(normalized, username, password, rememberCredentials)
            val updatedSnap = prefs.prefsFlow.first()

            _uiState.update {
                it.copy(
                    serverUrl = normalized,
                    recentServerUrls = updatedSnap.recentServerUrls,
                    rememberCredentials = rememberCredentials,
                    savedUsername = username.trim(),
                    savedPassword = if (rememberCredentials) password else "",
                    isLoading = true,
                    errorMessage = null,
                    canRetry = false,
                    isInCreateFlow = false,
                )
            }
            try {
                val resp = api.login(normalized, username, password)
                if (resp.player?.loggedIn == true) {
                    syncNewDayAlarm(resp)
                    syncMailNotification(resp)
                    _uiState.update { it.copy(isLoading = false, page = resp) }
                } else {
                    val serverHint = resp.paragraphs
                        .flatten()
                        .joinToString("") { it.text }
                        .trim()
                        .take(180)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = serverHint.ifBlank {
                                "Login failed. Check your Crawler name, password, or email verification."
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not connect to server."
                    )
                }
            }
        }
    }

    fun openRegistration(serverUrl: String) {
        val normalized = prefs.normalizeUrl(serverUrl)
        if (normalized.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your lotgd-dcc server URL first.") }
            return
        }
        viewModelScope.launch {
            prefs.saveServerUrl(normalized)
            val updatedSnap = prefs.prefsFlow.first()
            _uiState.update {
                it.copy(
                    serverUrl = normalized,
                    recentServerUrls = updatedSnap.recentServerUrls,
                    isLoading = true,
                    errorMessage = null,
                    canRetry = false,
                )
            }
            try {
                val resp = api.fetchPage(normalized, "create.php")
                _uiState.update {
                    it.copy(isLoading = false, page = resp, isInCreateFlow = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load registration.")
                }
            }
        }
    }

    fun removeRecentUrl(url: String) {
        viewModelScope.launch {
            prefs.removeRecentUrl(url)
            val snap = prefs.prefsFlow.first()
            _uiState.update { it.copy(recentServerUrls = snap.recentServerUrls) }
        }
    }

    fun updateAutoHealSettings(enabled: Boolean, thresholdPercent: Int) {
        val clamped = thresholdPercent.coerceIn(10, 90)
        _uiState.update {
            it.copy(autoHealEnabled = enabled, autoHealThresholdPercent = clamped)
        }
        viewModelScope.launch {
            prefs.saveAutoHealSettings(enabled, clamped)
        }
    }

    fun updateNewDayNotifications(enabled: Boolean) {
        _uiState.update { it.copy(newDayNotificationsEnabled = enabled) }
        viewModelScope.launch {
            prefs.saveNewDayNotificationsEnabled(enabled)
            if (enabled) {
                val curPage = _uiState.value.page
                if (curPage != null && curPage.nextNewDaySeconds > 0) {
                    NewDayScheduler.scheduleNextNewDay(
                        context = appContext,
                        delaySeconds = curPage.nextNewDaySeconds,
                        secondsPerGameDay = curPage.secondsPerGameDay,
                    )
                }
            } else {
                NewDayScheduler.cancel(appContext)
            }
        }
    }

    fun updateMailNotifications(enabled: Boolean) {
        _uiState.update { it.copy(mailNotificationsEnabled = enabled) }
        viewModelScope.launch {
            prefs.saveMailNotificationsEnabled(enabled)
            if (enabled) {
                MailNotificationScheduler.startPeriodicPolling(appContext)
            } else {
                MailNotificationScheduler.cancel(appContext)
            }
        }
    }

    fun updateCombatFlair(enabled: Boolean) {
        _uiState.update { it.copy(combatFlairEnabled = enabled) }
        viewModelScope.launch {
            prefs.saveCombatFlairEnabled(enabled)
        }
    }

    fun updateUseLightTheme(useLight: Boolean) {
        _uiState.update { it.copy(useLightTheme = useLight) }
        viewModelScope.launch {
            prefs.saveUseLightTheme(useLight)
        }
    }

    fun updateNarrativeFontSize(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(13, 20)
        _uiState.update { it.copy(narrativeFontSizeSp = clamped) }
        viewModelScope.launch {
            prefs.saveNarrativeFontSizeSp(clamped)
        }
    }

    fun updateCompactCombatLog(compact: Boolean) {
        _uiState.update { it.copy(compactCombatLog = compact) }
        viewModelScope.launch {
            prefs.saveCompactCombatLog(compact)
        }
    }

    fun refreshCurrentPage() {
        val url = _uiState.value.page?.currentUrl?.takeIf { it.isNotBlank() } ?: "village.php"
        navigate(url)
    }

    fun retryLastAction() {
        val failedForm = lastFailedForm
        val failedUrl = lastFailedUrl
        when {
            failedForm != null -> submitForm(failedForm.first, failedForm.second)
            !failedUrl.isNullOrBlank() -> navigate(failedUrl)
            else -> refreshCurrentPage()
        }
    }

    fun navigate(url: String) {
        val baseUrl = _uiState.value.serverUrl
        if (baseUrl.isBlank() || url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canRetry = false) }
            try {
                val resp = api.fetchPage(baseUrl, url)
                lastFailedUrl = null
                lastFailedForm = null
                val loggedOut = resp.player == null && !_uiState.value.isInCreateFlow && resp.pageType == "login"
                if (!loggedOut) {
                    syncNewDayAlarm(resp)
                    syncMailNotification(resp)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page = if (loggedOut) null else resp,
                        isInCreateFlow = if (resp.player?.loggedIn == true) false else it.isInCreateFlow,
                    )
                }
            } catch (e: Exception) {
                lastFailedUrl = url
                lastFailedForm = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Request failed. Check your connection.",
                        canRetry = true,
                    )
                }
            }
        }
    }

    fun submitForm(form: GameForm, values: Map<String, String>) {
        val state = _uiState.value
        val baseUrl = state.serverUrl
        val currentUrl = state.page?.currentUrl ?: "village.php"
        if (baseUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canRetry = false) }
            try {
                val resp = api.submitForm(
                    baseUrl = baseUrl,
                    currentPageUrl = currentUrl,
                    action = form.action,
                    method = form.method,
                    fields = values,
                )
                lastFailedUrl = null
                lastFailedForm = null
                syncNewDayAlarm(resp)
                syncMailNotification(resp)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page = resp,
                        isInCreateFlow = if (resp.player?.loggedIn == true) false else it.isInCreateFlow,
                    )
                }
            } catch (e: Exception) {
                lastFailedForm = form to values
                lastFailedUrl = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Form submission failed.",
                        canRetry = true,
                    )
                }
            }
        }
    }

    fun recoverBadNav() {
        val recovery = _uiState.value.page?.recoveryUrl ?: "badnav.php"
        navigate(recovery)
    }

    fun openSyndicateComms() {
        val mailUrl = _uiState.value.page?.mail?.url ?: "mail.php"
        navigate(mailUrl)
    }

    fun setStatsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showStatsSheet = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun returnToLogin() {
        api.clearSession()
        _uiState.update {
            it.copy(
                page = null,
                isInCreateFlow = false,
                showStatsSheet = false,
                showSettingsSheet = false,
                errorMessage = null,
                canRetry = false,
            )
        }
    }

    fun logout() {
        val baseUrl = _uiState.value.serverUrl
        viewModelScope.launch {
            if (baseUrl.isNotBlank()) {
                runCatching { api.fetchPage(baseUrl, "login.php?op=logout") }
            }
            api.clearSession()
            _uiState.update {
                it.copy(
                    page = null,
                    isInCreateFlow = false,
                    showStatsSheet = false,
                    showSettingsSheet = false,
                    errorMessage = null,
                    canRetry = false,
                )
            }
        }
    }
}
