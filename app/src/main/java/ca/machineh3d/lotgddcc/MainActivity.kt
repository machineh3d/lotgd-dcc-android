package ca.machineh3d.lotgddcc

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.machineh3d.lotgddcc.data.AppThemeMode
import ca.machineh3d.lotgddcc.notifications.MailNotificationScheduler
import ca.machineh3d.lotgddcc.ui.GameViewModel
import ca.machineh3d.lotgddcc.ui.components.CrawlerShieldIcon
import ca.machineh3d.lotgddcc.ui.components.CrtHudOverlay
import ca.machineh3d.lotgddcc.ui.screens.GameScreen
import ca.machineh3d.lotgddcc.ui.screens.LoginScreen
import ca.machineh3d.lotgddcc.ui.theme.DccColors
import ca.machineh3d.lotgddcc.ui.theme.LotgdDccTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openMailOnStart = intent?.getBooleanExtra(
            MailNotificationScheduler.EXTRA_OPEN_MAIL,
            false
        ) == true

        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.uiState.collectAsState()
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1150L)
                showSplash = false
            }

            LaunchedEffect(openMailOnStart) {
                if (openMailOnStart) {
                    vm.requestOpenMailFromNotification()
                }
            }

            // Keep screen awake while enabled in Settings
            DisposableEffect(state.keepScreenAwake) {
                if (state.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val systemDark = isSystemInDarkTheme()
            LotgdDccTheme(themeMode = state.themeMode, systemDark = systemDark) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                            availableUpdate = state.availableUpdate,
                            isCheckingUpdate = state.isCheckingUpdate,
                            isDownloadingUpdate = state.isDownloadingUpdate,
                            updateDownloadProgress = state.updateDownloadProgress,
                            updateCheckStatus = state.updateCheckStatus,
                            onCheckForUpdates = { vm.checkForUpdates(silent = false) },
                            onInstallUpdate = { vm.downloadAndInstallUpdate() },
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
                            canRetry = state.canRetry,
                            showStatsSheet = state.showStatsSheet,
                            showSettingsSheet = state.showSettingsSheet,
                            autoHealEnabled = state.autoHealEnabled,
                            autoHealThresholdPercent = state.autoHealThresholdPercent,
                            newDayNotificationsEnabled = state.newDayNotificationsEnabled,
                            mailNotificationsEnabled = state.mailNotificationsEnabled,
                            pvpNotificationsEnabled = state.pvpNotificationsEnabled,
                            notificationSoundsEnabled = state.notificationSoundsEnabled,
                            combatFlairEnabled = state.combatFlairEnabled,
                            combatFlairIntensity = state.combatFlairIntensity,
                            combatSoundsEnabled = state.combatSoundsEnabled,
                            menuSoundsEnabled = state.menuSoundsEnabled,
                            pulseTrainIndicator = state.pulseTrainIndicator,
                            keepScreenAwake = state.keepScreenAwake,
                            themeMode = state.themeMode,
                            narrativeFontSizeSp = state.narrativeFontSizeSp,
                            compactCombatLog = state.compactCombatLog,
                            crtScanlinesEnabled = state.crtScanlinesEnabled,
                            hudVignetteEnabled = state.hudVignetteEnabled,
                            neonEdgeGlowEnabled = state.neonEdgeGlowEnabled,
                            autoCheckUpdatesEnabled = state.autoCheckUpdatesEnabled,
                            availableUpdate = state.availableUpdate,
                            isCheckingUpdate = state.isCheckingUpdate,
                            isDownloadingUpdate = state.isDownloadingUpdate,
                            updateDownloadProgress = state.updateDownloadProgress,
                            updateCheckStatus = state.updateCheckStatus,
                            sessionTelemetry = state.sessionTelemetry,
                            friendsOnlineCount = state.friendsOnlineCount,
                            friendsPendingCount = state.friendsPendingCount,
                            onNavigate = { url -> vm.navigate(url) },
                            onRefresh = { vm.refreshCurrentPage() },
                            onRetry = { vm.retryLastAction() },
                            onSubmitForm = { form, values -> vm.submitForm(form, values) },
                            onRecoverBadNav = { vm.recoverBadNav() },
                            onOpenComms = { vm.openSyndicateComms() },
                            onOpenFriends = { vm.openFriendsList() },
                            onPollFriends = { vm.pollFriendsStatus() },
                            onToggleStatsSheet = { vm.setStatsSheetVisible(it) },
                            onToggleSettingsSheet = { vm.setSettingsSheetVisible(it) },
                            onUpdateAutoHeal = { enabled, pct -> vm.updateAutoHealSettings(enabled, pct) },
                            onToggleNewDayNotifications = { enabled -> vm.updateNewDayNotifications(enabled) },
                            onToggleMailNotifications = { enabled -> vm.updateMailNotifications(enabled) },
                            onTogglePvpNotifications = { enabled -> vm.updatePvpNotifications(enabled) },
                            onToggleNotificationSounds = { enabled -> vm.updateNotificationSounds(enabled) },
                            onUpdateCombatFlair = { enabled, intensity -> vm.updateCombatFlair(enabled, intensity) },
                            onToggleCombatSounds = { enabled -> vm.updateCombatSounds(enabled) },
                            onToggleMenuSounds = { enabled -> vm.updateMenuSounds(enabled) },
                            onTogglePulseTrain = { enabled -> vm.updatePulseTrainIndicator(enabled) },
                            onToggleKeepScreenAwake = { enabled -> vm.updateKeepScreenAwake(enabled) },
                            onUpdateThemeMode = { mode -> vm.updateThemeMode(mode) },
                            onUpdateNarrativeFontSize = { sp -> vm.updateNarrativeFontSize(sp) },
                            onToggleCompactCombatLog = { enabled -> vm.updateCompactCombatLog(enabled) },
                            onToggleCrtScanlines = { enabled -> vm.updateCrtScanlines(enabled) },
                            onToggleHudVignette = { enabled -> vm.updateHudVignette(enabled) },
                            onToggleNeonEdgeGlow = { enabled -> vm.updateNeonEdgeGlow(enabled) },
                            onToggleAutoCheckUpdates = { enabled -> vm.updateAutoCheckUpdates(enabled) },
                            onCheckForUpdates = { vm.checkForUpdates(silent = false) },
                            onInstallUpdate = { vm.downloadAndInstallUpdate() },
                            onLogout = {
                                if (state.isInCreateFlow) vm.returnToLogin() else vm.logout()
                            },
                        )
                    }

                    CrtHudOverlay(
                        scanlinesEnabled = state.crtScanlinesEnabled,
                        vignetteEnabled = state.hudVignetteEnabled,
                        neonEdgeGlowEnabled = state.neonEdgeGlowEnabled,
                    )

                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(),
                    ) {
                        SyndicateSplashOverlay()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun SyndicateSplashOverlay() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DccColors.Background)
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CrawlerShieldIcon(size = 76.dp, accentColor = DccColors.Cyan)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BORANT SYNDICATE BROADCAST",
                color = DccColors.Crimson,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "Dungeon Crawler World",
                color = DccColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "INITIALIZING CRAWLER HUD · v${BuildConfig.VERSION_NAME}",
                color = DccColors.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                color = DccColors.Cyan,
                trackColor = DccColors.SurfaceVariant,
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
