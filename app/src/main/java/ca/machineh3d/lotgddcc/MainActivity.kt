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
                        initialUsername = state.savedUsername,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onLogin = { url, user, pass -> vm.login(url, user, pass) },
                        onOpenRegistration = { url -> vm.openRegistration(url) },
                    )
                } else {
                    GameScreen(
                        page = activePage,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        showStatsSheet = state.showStatsSheet,
                        onNavigate = { url -> vm.navigate(url) },
                        onSubmitForm = { form, values -> vm.submitForm(form, values) },
                        onRecoverBadNav = { vm.recoverBadNav() },
                        onOpenComms = { vm.openSyndicateComms() },
                        onToggleStatsSheet = { vm.setStatsSheetVisible(it) },
                        onLogout = {
                            if (state.isInCreateFlow) vm.returnToLogin() else vm.logout()
                        },
                    )
                }
            }
        }
    }
}
