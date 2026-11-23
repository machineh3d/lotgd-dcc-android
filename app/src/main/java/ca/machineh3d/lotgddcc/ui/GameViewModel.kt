package ca.machineh3d.lotgddcc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.machineh3d.lotgddcc.BuildConfig
import ca.machineh3d.lotgddcc.audio.SoundFxManager
import ca.machineh3d.lotgddcc.data.AppThemeMode
import ca.machineh3d.lotgddcc.data.AppUpdateInfo
import ca.machineh3d.lotgddcc.data.DccApiClient
import ca.machineh3d.lotgddcc.data.ServerPreferences
import ca.machineh3d.lotgddcc.data.UpdateCheckResult
import ca.machineh3d.lotgddcc.data.UpdateChecker
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

data class SessionTelemetry(
    val enemiesDefeated: Int = 0,
    val goldNet: Int = 0,
    val xpGained: Int = 0,
    val tokensNet: Int = 0,
)

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
    val pvpNotificationsEnabled: Boolean = false,
    val notificationSoundsEnabled: Boolean = true,
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
    val availableUpdate: AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val updateCheckStatus: String? = null,
    val sessionTelemetry: SessionTelemetry = SessionTelemetry(),
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
    private var lastKnownAlive: Boolean = true
    private var lastFailedUrl: String? = null
    private var lastFailedForm: Pair<GameForm, Map<String, String>>? = null
    private var pendingOpenMailOnResume: Boolean = false

    private var prevGold: Int? = null
    private var prevXp: Int? = null
    private var prevGems: Int? = null

    init {
        viewModelScope.launch {
            val snap = prefs.prefsFlow.first()
            lastKnownUnreadMail = snap.lastNotifiedUnreadMail
            lastKnownAlive = snap.lastKnownAlive
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
                    pvpNotificationsEnabled = snap.pvpNotificationsEnabled,
                    notificationSoundsEnabled = snap.notificationSoundsEnabled,
                    combatFlairEnabled = snap.combatFlairEnabled,
                    combatFlairIntensity = snap.combatFlairIntensity,
                    combatSoundsEnabled = snap.combatSoundsEnabled,
                    menuSoundsEnabled = snap.menuSoundsEnabled,
                    pulseTrainIndicator = snap.pulseTrainIndicator,
                    keepScreenAwake = snap.keepScreenAwake,
                    themeMode = snap.themeMode,
                    narrativeFontSizeSp = snap.narrativeFontSizeSp,
                    compactCombatLog = snap.compactCombatLog,
                    crtScanlinesEnabled = snap.crtScanlinesEnabled,
                    hudVignetteEnabled = snap.hudVignetteEnabled,
                    neonEdgeGlowEnabled = snap.neonEdgeGlowEnabled,
                )
            }
            if (snap.mailNotificationsEnabled || snap.pvpNotificationsEnabled) {
                MailNotificationScheduler.startPeriodicPolling(appContext)
            }
            if (snap.lastServerUrl.isNotBlank()) {
                resumeSession(snap.lastServerUrl)
            }
            checkForUpdates(silent = true)
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isCheckingUpdate = true, updateCheckStatus = "Checking GitHub Releases...") }
            }
            when (val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            availableUpdate = result.info,
                            isCheckingUpdate = false,
                            updateCheckStatus = "Update available: ${result.info.latestVersion}",
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            availableUpdate = null,
                            isCheckingUpdate = false,
                            updateCheckStatus = if (silent) null else "Up to date (latest on GitHub: ${result.latestVersion})",
                        )
                    }
                }
                is UpdateCheckResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateCheckStatus = if (silent) null else "Update check failed: ${result.reason}",
                        )
                    }
                }
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

    private fun syncMailAndPvpNotifications(resp: DccPageResponse) {
        val player = resp.player ?: return
        if (!player.loggedIn) return
        val state = _uiState.value
        val unread = resp.mail.unread

        if (state.mailNotificationsEnabled && unread > lastKnownUnreadMail) {
            MailNotificationScheduler.postMailNotification(
                context = appContext,
                unreadCount = unread,
                soundEnabled = state.notificationSoundsEnabled,
            )
            if (state.menuSoundsEnabled) {
                SoundFxManager.playMailPing()
            }
        }

        if (unread != lastKnownUnreadMail) {
            lastKnownUnreadMail = unread
            viewModelScope.launch { prefs.saveLastNotifiedUnreadMail(unread) }
        }
        if (player.alive != lastKnownAlive) {
            lastKnownAlive = player.alive
            viewModelScope.launch { prefs.saveLastKnownAlive(player.alive) }
        }
    }

    private fun updateSessionTelemetry(resp: DccPageResponse) {
        val player = resp.player ?: return
        if (!player.loggedIn) return

        val plain = resp.paragraphs.joinToString(" ") { line -> line.joinToString("") { it.text } }
        val wonFight = plain.contains("You have defeated", ignoreCase = true) ||
            plain.contains("You have slain", ignoreCase = true) ||
            plain.contains("HOSTILE ELIMINATED", ignoreCase = true)

        val dGold = prevGold?.let { player.gold - it } ?: 0
        val dXp = prevXp?.let { (player.experience - it).coerceAtLeast(0) } ?: 0
        val dGems = prevGems?.let { player.gems - it } ?: 0

        prevGold = player.gold
        prevXp = player.experience
        prevGems = player.gems

        if (wonFight || dGold != 0 || dXp != 0 || dGems != 0) {
            _uiState.update { st ->
                val cur = st.sessionTelemetry
                st.copy(
                    sessionTelemetry = cur.copy(
                        enemiesDefeated = cur.enemiesDefeated + if (wonFight) 1 else 0,
                        goldNet = cur.goldNet + dGold,
                        xpGained = cur.xpGained + dXp,
                        tokensNet = cur.tokensNet + dGems,
                    )
                )
            }
        }
    }

    private fun resetSessionTelemetry() {
        prevGold = null
        prevXp = null
        prevGems = null
        _uiState.update { it.copy(sessionTelemetry = SessionTelemetry()) }
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
                    syncMailAndPvpNotifications(resp)
                    updateSessionTelemetry(resp)
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
                    resetSessionTelemetry()
                    syncNewDayAlarm(resp)
                    syncMailAndPvpNotifications(resp)
                    updateSessionTelemetry(resp)
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
        viewModelScope.launch { prefs.saveAutoHealSettings(enabled, clamped) }
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
            syncBackgroundPoller()
        }
    }

    fun updatePvpNotifications(enabled: Boolean) {
        _uiState.update { it.copy(pvpNotificationsEnabled = enabled) }
        viewModelScope.launch {
            prefs.savePvpNotificationsEnabled(enabled)
            syncBackgroundPoller()
        }
    }

    fun updateNotificationSounds(enabled: Boolean) {
        _uiState.update { it.copy(notificationSoundsEnabled = enabled) }
        viewModelScope.launch { prefs.saveNotificationSoundsEnabled(enabled) }
    }

    private fun syncBackgroundPoller() {
        val st = _uiState.value
        if (st.mailNotificationsEnabled || st.pvpNotificationsEnabled) {
            MailNotificationScheduler.startPeriodicPolling(appContext)
        } else {
            MailNotificationScheduler.cancel(appContext)
        }
    }

    fun updateCombatFlair(enabled: Boolean, intensity: Int) {
        val clamped = intensity.coerceIn(20, 100)
        _uiState.update { it.copy(combatFlairEnabled = enabled, combatFlairIntensity = clamped) }
        viewModelScope.launch { prefs.saveCombatFlairSettings(enabled, clamped) }
    }

    fun updateCombatSounds(enabled: Boolean) {
        _uiState.update { it.copy(combatSoundsEnabled = enabled) }
        viewModelScope.launch {
            prefs.saveCombatSoundsEnabled(enabled)
            if (enabled) SoundFxManager.playSwordSlash()
        }
    }

    fun updateMenuSounds(enabled: Boolean) {
        _uiState.update { it.copy(menuSoundsEnabled = enabled) }
        viewModelScope.launch {
            prefs.saveMenuSoundsEnabled(enabled)
            if (enabled) SoundFxManager.playUiClick()
        }
    }

    fun updatePulseTrainIndicator(enabled: Boolean) {
        _uiState.update { it.copy(pulseTrainIndicator = enabled) }
        viewModelScope.launch { prefs.savePulseTrainIndicator(enabled) }
    }

    fun updateKeepScreenAwake(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenAwake = enabled) }
        viewModelScope.launch { prefs.saveKeepScreenAwake(enabled) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { prefs.saveThemeMode(mode) }
    }

    fun updateNarrativeFontSize(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(13, 20)
        _uiState.update { it.copy(narrativeFontSizeSp = clamped) }
        viewModelScope.launch { prefs.saveNarrativeFontSizeSp(clamped) }
    }

    fun updateCompactCombatLog(compact: Boolean) {
        _uiState.update { it.copy(compactCombatLog = compact) }
        viewModelScope.launch { prefs.saveCompactCombatLog(compact) }
    }

    fun updateCrtScanlines(enabled: Boolean) {
        _uiState.update { it.copy(crtScanlinesEnabled = enabled) }
        viewModelScope.launch { prefs.saveCrtScanlinesEnabled(enabled) }
    }

    fun updateHudVignette(enabled: Boolean) {
        _uiState.update { it.copy(hudVignetteEnabled = enabled) }
        viewModelScope.launch { prefs.saveHudVignetteEnabled(enabled) }
    }

    fun updateNeonEdgeGlow(enabled: Boolean) {
        _uiState.update { it.copy(neonEdgeGlowEnabled = enabled) }
        viewModelScope.launch { prefs.saveNeonEdgeGlowEnabled(enabled) }
    }

    fun downloadAndInstallUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        val apkUrl = update.apkDownloadUrl
        if (apkUrl.isNullOrBlank()) {
            _uiState.update { it.copy(updateCheckStatus = "No APK asset attached yet — opening GitHub Releases.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingUpdate = true,
                    updateDownloadProgress = 0f,
                    updateCheckStatus = "Downloading ${update.latestVersion} APK...",
                )
            }
            try {
                val apkFile = UpdateChecker.downloadApk(appContext, apkUrl) { progress ->
                    _uiState.update { it.copy(updateDownloadProgress = progress) }
                }
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateDownloadProgress = 1f,
                        updateCheckStatus = "Launching Android installer for ${update.latestVersion}...",
                    )
                }
                UpdateChecker.launchApkInstaller(appContext, apkFile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateCheckStatus = "Download failed: ${e.message ?: "check connection"}",
                    )
                }
            }
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
        val state = _uiState.value
        val baseUrl = state.serverUrl
        if (baseUrl.isBlank() || url.isBlank()) return

        val isCombatAction = url.contains("op=fight", ignoreCase = true) ||
            url.contains("op=run", ignoreCase = true)
        if (state.menuSoundsEnabled && !isCombatAction) {
            SoundFxManager.playUiClick()
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canRetry = false) }
            try {
                val resp = api.fetchPage(baseUrl, url)
                lastFailedUrl = null
                lastFailedForm = null
                val loggedOut = resp.player == null && !_uiState.value.isInCreateFlow && resp.pageType == "login"
                if (!loggedOut) {
                    syncNewDayAlarm(resp)
                    syncMailAndPvpNotifications(resp)
                    updateSessionTelemetry(resp)
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

        if (state.menuSoundsEnabled) {
            SoundFxManager.playUiClick()
        }

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
                syncMailAndPvpNotifications(resp)
                updateSessionTelemetry(resp)
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
        val cur = _uiState.value.page
        val curUrl = cur?.currentUrl.orEmpty()
        val firstNav = cur?.navGroups?.firstOrNull()?.items?.firstOrNull()?.url
        val target = when {
            !firstNav.isNullOrBlank() && !firstNav.contains("badnav.php", ignoreCase = true) -> firstNav
            curUrl.contains("badnav.php", ignoreCase = true) -> "village.php"
            else -> cur?.recoveryUrl?.takeIf { it.isNotBlank() } ?: "badnav.php"
        }
        navigate(target)
    }

    fun openSyndicateComms() {
        val mailUrl = _uiState.value.page?.mail?.url ?: "mail.php"
        navigate(mailUrl)
    }

    fun setStatsSheetVisible(visible: Boolean) {
        if (visible && _uiState.value.menuSoundsEnabled) SoundFxManager.playUiClick()
        _uiState.update { it.copy(showStatsSheet = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        if (visible && _uiState.value.menuSoundsEnabled) SoundFxManager.playUiClick()
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun returnToLogin() {
        api.clearSession()
        resetSessionTelemetry()
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
            resetSessionTelemetry()
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
