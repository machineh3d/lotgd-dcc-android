package ca.machineh3d.lotgddcc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.machineh3d.lotgddcc.data.DccApiClient
import ca.machineh3d.lotgddcc.data.ServerPreferences
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val page: DccPageResponse? = null,
    val isInCreateFlow: Boolean = false,
    val showStatsSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ServerPreferences(application)
    private val api = DccApiClient(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = prefs.prefsFlow.first()
            _uiState.update {
                it.copy(
                    serverUrl = snap.lastServerUrl,
                    recentServerUrls = snap.recentServerUrls,
                    rememberCredentials = snap.rememberCredentials,
                    savedUsername = snap.savedUsername,
                    savedPassword = snap.savedPassword,
                    autoHealEnabled = snap.autoHealEnabled,
                    autoHealThresholdPercent = snap.autoHealThresholdPercent,
                )
            }
            if (snap.lastServerUrl.isNotBlank()) {
                resumeSession(snap.lastServerUrl)
            }
        }
    }

    private fun resumeSession(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = api.fetchPage(url, "village.php")
                if (resp.player?.loggedIn == true) {
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
                    isInCreateFlow = false,
                )
            }
            try {
                val resp = api.login(normalized, username, password)
                if (resp.player?.loggedIn == true) {
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

    fun navigate(url: String) {
        val baseUrl = _uiState.value.serverUrl
        if (baseUrl.isBlank() || url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = api.fetchPage(baseUrl, url)
                val loggedOut = resp.player == null && !_uiState.value.isInCreateFlow && resp.pageType == "login"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page = if (loggedOut) null else resp,
                        isInCreateFlow = if (resp.player?.loggedIn == true) false else it.isInCreateFlow,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Request failed.")
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = api.submitForm(
                    baseUrl = baseUrl,
                    currentPageUrl = currentUrl,
                    action = form.action,
                    method = form.method,
                    fields = values,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page = resp,
                        isInCreateFlow = if (resp.player?.loggedIn == true) false else it.isInCreateFlow,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Form submission failed.")
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
                )
            }
        }
    }
}
