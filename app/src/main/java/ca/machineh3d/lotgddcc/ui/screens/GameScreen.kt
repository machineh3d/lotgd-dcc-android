package ca.machineh3d.lotgddcc.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ca.machineh3d.lotgddcc.BuildConfig
import ca.machineh3d.lotgddcc.model.CharStatSection
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.ui.components.ActionDeck
import ca.machineh3d.lotgddcc.ui.components.NarrativeView
import ca.machineh3d.lotgddcc.ui.components.NativeFormCard
import ca.machineh3d.lotgddcc.ui.components.NativeTableCards
import ca.machineh3d.lotgddcc.ui.components.TopHudBar
import ca.machineh3d.lotgddcc.ui.theme.DccColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    serverUrl: String,
    page: DccPageResponse,
    isLoading: Boolean,
    errorMessage: String?,
    showStatsSheet: Boolean,
    showSettingsSheet: Boolean,
    autoHealEnabled: Boolean,
    autoHealThresholdPercent: Int,
    newDayNotificationsEnabled: Boolean,
    onNavigate: (String) -> Unit,
    onSubmitForm: (GameForm, Map<String, String>) -> Unit,
    onRecoverBadNav: () -> Unit,
    onOpenComms: () -> Unit,
    onToggleStatsSheet: (Boolean) -> Unit,
    onToggleSettingsSheet: (Boolean) -> Unit,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(page.currentUrl, page.title, page.paragraphs.size, page.tables.size) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        containerColor = DccColors.Background,
        topBar = {
            Column {
                TopHudBar(
                    title = page.title,
                    player = page.player,
                    mail = page.mail,
                    onOpenStats = { onToggleStatsSheet(true) },
                    onOpenComms = onOpenComms,
                    onOpenSettings = { onToggleSettingsSheet(true) },
                    onLogout = onLogout,
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        color = DccColors.Cyan,
                        trackColor = DccColors.SurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }
            }
        },
        bottomBar = {
            ActionDeck(
                pageType = page.pageType,
                badnav = page.badnav,
                navGroups = page.navGroups,
                player = page.player,
                autoHealEnabled = autoHealEnabled,
                autoHealThresholdPercent = autoHealThresholdPercent,
                enabled = !isLoading,
                onNavigate = onNavigate,
                onRecoverBadNav = onRecoverBadNav,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DccColors.Background)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = DccColors.Crimson.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, DccColors.Crimson.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = DccColors.TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                NarrativeView(
                    paragraphs = page.paragraphs,
                    onLinkClick = onNavigate,
                )

                if (page.tables.isNotEmpty()) {
                    NativeTableCards(
                        tables = page.tables,
                        enabled = !isLoading,
                        onActionClick = onNavigate,
                    )
                }

                for (form in page.forms) {
                    NativeFormCard(
                        form = form,
                        enabled = !isLoading,
                        onSubmit = { values -> onSubmitForm(form, values) },
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showStatsSheet) {
        ModalBottomSheet(
            onDismissRequest = { onToggleStatsSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DccColors.Surface,
        ) {
            CrawlerStatsSheetContent(
                sections = page.charStats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { onToggleSettingsSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DccColors.Surface,
        ) {
            ClientSettingsSheetContent(
                serverUrl = serverUrl,
                bridgeApiVersion = page.apiVersion,
                nextNewDaySeconds = page.nextNewDaySeconds,
                autoHealEnabled = autoHealEnabled,
                autoHealThresholdPercent = autoHealThresholdPercent,
                newDayNotificationsEnabled = newDayNotificationsEnabled,
                onUpdateAutoHeal = onUpdateAutoHeal,
                onToggleNewDayNotifications = onToggleNewDayNotifications,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ClientSettingsSheetContent(
    serverUrl: String,
    bridgeApiVersion: Int,
    nextNewDaySeconds: Int,
    autoHealEnabled: Boolean,
    autoHealThresholdPercent: Int,
    newDayNotificationsEnabled: Boolean,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNewDayNotifications(granted)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Crawler HUD Settings",
            color = DccColors.Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        // 1. Auto-Heal Threshold Card
        Surface(
            color = DccColors.SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Switch Corridors Button to Healer",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Replaces 'Look for a Fight' with Healer's Hut when HP drops below threshold.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = autoHealEnabled,
                        onCheckedChange = { onUpdateAutoHeal(it, autoHealThresholdPercent) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Emerald,
                        )
                    )
                }

                if (autoHealEnabled) {
                    HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Low-HP Heal Threshold",
                            color = DccColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "$autoHealThresholdPercent% HP",
                            color = DccColors.Emerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = autoHealThresholdPercent.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(10, 90)
                            onUpdateAutoHeal(true, snapped)
                        },
                        valueRange = 10f..90f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = DccColors.Emerald,
                            activeTrackColor = DccColors.Emerald,
                        )
                    )
                }
            }
        }

        // 2. New Game Day Notifications Card
        Surface(
            color = DccColors.SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Game Day Notifications",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val countdownHint = if (nextNewDaySeconds > 0) {
                            "Alerts when Stamina resets (next day in ${formatDuration(nextNewDaySeconds)})."
                        } else {
                            "Alerts when a new in-game day begins (requires Bridge v4+)."
                        }
                        Text(
                            text = countdownHint,
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = newDayNotificationsEnabled,
                        onCheckedChange = { wantEnabled ->
                            if (!wantEnabled) {
                                onToggleNewDayNotifications(false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    onToggleNewDayNotifications(true)
                                } else {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                onToggleNewDayNotifications(true)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Gold,
                        )
                    )
                }
            }
        }

        // 3. About App Card
        Surface(
            color = DccColors.SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "ABOUT APP",
                    color = DccColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                AboutRow("App", "Dungeon Crawler World (lotgd-dcc)")
                AboutRow("Version", "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                AboutRow("Server", serverUrl.ifBlank { "Not connected" })
                AboutRow("Bridge API", "v$bridgeApiVersion")
                AboutRow("Author", "Devin Falconer (machineh3d)")
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = DccColors.TextSecondary,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = DccColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "${totalSeconds}s"
    }
}

@Composable
private fun CrawlerStatsSheetContent(
    sections: List<CharStatSection>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Crawler HUD Dossier",
            color = DccColors.Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        if (sections.isEmpty()) {
            Text(
                text = "No additional stats reported on this screen.",
                color = DccColors.TextSecondary,
                fontSize = 14.sp,
            )
        } else {
            for (sec in sections) {
                Surface(
                    color = DccColors.SurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = sec.section.uppercase(),
                            color = DccColors.Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        sec.items.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = DccColors.Outline.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.label,
                                    color = DccColors.TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = item.value,
                                    color = DccColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
