package ca.machineh3d.lotgddcc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.machineh3d.lotgddcc.ui.GameViewModel
import ca.machineh3d.lotgddcc.ui.screens.GameScreen
import ca.machineh3d.lotgddcc.ui.screens.LoginScreen
import ca.machineh3d.lotgddcc.ui.theme.LotgdDccTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LotgdDccTheme {
                val vm: GameViewModel = viewModel()
                val state by vm.uiState.collectAsState()

                val activePage = state.page
                if (activePage == null) {
                    LoginScreen(
                        initialServerUrl = state.serverUrl,
                        recentServerUrls = state.recentServerUrls,
                        initialRememberCredentials = state.rememberCredentials,
                        initialUsername = state.savedUsername,
                        initialPassword = state.savedPassword,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onLogin = { url, user, pass, remember -> vm.login(url, user, pass, remember) },
                        onOpenRegistration = { url -> vm.openRegistration(url) },
                        onRemoveRecentUrl = { url -> vm.removeRecentUrl(url) },
                    )
                } else {
                    GameScreen(
                        serverUrl = state.serverUrl,
                        page = activePage,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        showStatsSheet = state.showStatsSheet,
                        showSettingsSheet = state.showSettingsSheet,
                        autoHealEnabled = state.autoHealEnabled,
                        autoHealThresholdPercent = state.autoHealThresholdPercent,
                        newDayNotificationsEnabled = state.newDayNotificationsEnabled,
                        onNavigate = { url -> vm.navigate(url) },
                        onSubmitForm = { form, values -> vm.submitForm(form, values) },
                        onRecoverBadNav = { vm.recoverBadNav() },
                        onOpenComms = { vm.openSyndicateComms() },
                        onToggleStatsSheet = { vm.setStatsSheetVisible(it) },
                        onToggleSettingsSheet = { vm.setSettingsSheetVisible(it) },
                        onUpdateAutoHeal = { enabled, pct -> vm.updateAutoHealSettings(enabled, pct) },
                        onToggleNewDayNotifications = { enabled -> vm.updateNewDayNotifications(enabled) },
                        onLogout = {
                            if (state.isInCreateFlow) vm.returnToLogin() else vm.logout()
                        },
                    )
                }
            }
        }
    }
}
