package ca.machineh3d.lotgddcc.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ca.machineh3d.lotgddcc.BuildConfig
import ca.machineh3d.lotgddcc.model.CharStatSection
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.ui.components.ActionDeck
import ca.machineh3d.lotgddcc.ui.components.BattleHudCard
import ca.machineh3d.lotgddcc.ui.components.CasinoVisualPanel
import ca.machineh3d.lotgddcc.ui.components.NarrativeView
import ca.machineh3d.lotgddcc.ui.components.NativeFormCard
import ca.machineh3d.lotgddcc.ui.components.NativeTableCards
import ca.machineh3d.lotgddcc.ui.components.TopHudBar
import ca.machineh3d.lotgddcc.ui.components.filterRedundantWebBattleHudParagraphs
import ca.machineh3d.lotgddcc.ui.theme.DccColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    serverUrl: String,
    page: DccPageResponse,
    isLoading: Boolean,
    errorMessage: String?,
    canRetry: Boolean,
    showStatsSheet: Boolean,
    showSettingsSheet: Boolean,
    autoHealEnabled: Boolean,
    autoHealThresholdPercent: Int,
    newDayNotificationsEnabled: Boolean,
    mailNotificationsEnabled: Boolean,
    combatFlairEnabled: Boolean,
    useLightTheme: Boolean,
    narrativeFontSizeSp: Int,
    compactCombatLog: Boolean,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSubmitForm: (GameForm, Map<String, String>) -> Unit,
    onRecoverBadNav: () -> Unit,
    onOpenComms: () -> Unit,
    onToggleStatsSheet: (Boolean) -> Unit,
    onToggleSettingsSheet: (Boolean) -> Unit,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    onToggleMailNotifications: (Boolean) -> Unit,
    onToggleCombatFlair: (Boolean) -> Unit,
    onToggleLightTheme: (Boolean) -> Unit,
    onUpdateNarrativeFontSize: (Int) -> Unit,
    onToggleCompactCombatLog: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Combat micro-shake + subtle hit/attack flash state
    val shakeOffset = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(DccColors.Crimson) }
    var previousHp by remember { mutableStateOf<Int?>(null) }

    val allNavItems = remember(page.navGroups) {
        page.navGroups.flatMap { it.items }
    }

    LaunchedEffect(page.currentUrl, page.title, page.paragraphs.size, page.tables.size) {
        scrollState.scrollTo(0)

        val currentHp = page.player?.hp
        val prev = previousHp
        previousHp = currentHp

        if (combatFlairEnabled && page.paragraphs.isNotEmpty()) {
            val plain = page.paragraphs.joinToString(" ") { line -> line.joinToString("") { it.text } }
            val tookDamage = (prev != null && currentHp != null && currentHp < prev) ||
                plain.contains("hits you for", ignoreCase = true) ||
                plain.contains("strikes you for", ignoreCase = true)
            val landedHit = plain.contains("You hit ", ignoreCase = true) ||
                plain.contains("CRITICAL", ignoreCase = true) ||
                plain.contains("You have defeated", ignoreCase = true) ||
                plain.contains("You have slain", ignoreCase = true)

            if (tookDamage) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                flashColor = DccColors.Crimson
                launch {
                    flashAlpha.snapTo(0.24f)
                    flashAlpha.animateTo(0f, animationSpec = tween(260))
                }
                launch {
                    val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
                    for (dx in offsets) {
                        shakeOffset.animateTo(dx, animationSpec = tween(32))
                    }
                }
            } else if (landedHit) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                flashColor = DccColors.Cyan
                launch {
                    flashAlpha.snapTo(0.14f)
                    flashAlpha.animateTo(0f, animationSpec = tween(220))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shakeOffset.value }
    ) {
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
                        onTapHpBar = {
                            val player = page.player
                            val healerLink = allNavItems.firstOrNull {
                                it.url.contains("healer", ignoreCase = true) ||
                                    it.label.contains("healer", ignoreCase = true) ||
                                    it.label.contains("medbay", ignoreCase = true)
                            }
                            if (player != null && player.hp < player.maxHp && healerLink != null) {
                                onNavigate(healerLink.url)
                            } else {
                                onToggleStatsSheet(true)
                            }
                        },
                        onTapXpBar = {
                            val player = page.player
                            val trainerLink = allNavItems.firstOrNull {
                                it.url.contains("train", ignoreCase = true) ||
                                    it.label.contains("train", ignoreCase = true) ||
                                    it.label.contains("master", ignoreCase = true)
                            }
                            val ready = player != null &&
                                player.level < player.maxLevel &&
                                player.experience >= player.expNextLevel
                            if (ready && trainerLink != null) {
                                onNavigate(trainerLink.url)
                            } else {
                                onToggleStatsSheet(true)
                            }
                        },
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
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = DccColors.TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (canRetry) {
                                    Button(
                                        onClick = onRetry,
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Dedicated Syndicate Comms (Mail) header bar with 1-tap Return to Game
                    if (page.currentUrl.contains("mail.php", ignoreCase = true)) {
                        Surface(
                            color = DccColors.Cyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SYNDICATE COMMS",
                                        color = DccColors.Cyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.1.sp,
                                    )
                                    Text(
                                        text = if (page.mail.unread > 0) {
                                            "${page.mail.unread} unread message(s)"
                                        } else {
                                            "Inbox synced"
                                        },
                                        color = DccColors.TextSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onNavigate("village.php") },
                                    enabled = !isLoading,
                                    border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "Return to Game",
                                        color = DccColors.Cyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    BattleHudCard(
                        page = page,
                    )

                    CasinoVisualPanel(
                        page = page,
                        animate = combatFlairEnabled,
                    )

                    val isCombatScreen = remember(page.pageType, page.paragraphs) {
                        page.pageType == "combat" ||
                            page.paragraphs.any { line ->
                                val t = line.joinToString("") { it.text }
                                t.contains("'s Hitpoints", ignoreCase = true)
                            }
                    }

                    NarrativeView(
                        paragraphs = remember(page.paragraphs) {
                            filterRedundantWebBattleHudParagraphs(page.paragraphs)
                        },
                        onLinkClick = onNavigate,
                        fontSizeSp = narrativeFontSizeSp,
                        compactCombatLog = compactCombatLog,
                        isCombatPage = isCombatScreen,
                    )

                    val displayTables = remember(page.tables, page.forms, page.currentUrl) {
                        if (page.forms.isEmpty()) {
                            page.tables
                        } else {
                            page.tables.filterNot { table ->
                                val noHeaders = table.headers.isEmpty()
                                val noActions = table.rows.none { !it.actionUrl.isNullOrBlank() }
                                val isCreatePage = page.currentUrl.contains("create.php", ignoreCase = true)
                                val looksLikeFormLabels = table.rows.any { row ->
                                    val first = row.cells.firstOrNull()?.trim().orEmpty()
                                    first.endsWith(":") || first.endsWith("?")
                                }
                                noHeaders && noActions && (isCreatePage || looksLikeFormLabels)
                            }
                        }
                    }

                    if (displayTables.isNotEmpty()) {
                        NativeTableCards(
                            tables = displayTables,
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

        // Subtle screen flash overlay for combat hits
        if (flashAlpha.value > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flashColor.copy(alpha = flashAlpha.value))
            )
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
                secondsPerGameDay = page.secondsPerGameDay,
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
                mailNotificationsEnabled = mailNotificationsEnabled,
                combatFlairEnabled = combatFlairEnabled,
                useLightTheme = useLightTheme,
                narrativeFontSizeSp = narrativeFontSizeSp,
                compactCombatLog = compactCombatLog,
                onUpdateAutoHeal = onUpdateAutoHeal,
                onToggleNewDayNotifications = onToggleNewDayNotifications,
                onToggleMailNotifications = onToggleMailNotifications,
                onToggleCombatFlair = onToggleCombatFlair,
                onToggleLightTheme = onToggleLightTheme,
                onUpdateNarrativeFontSize = onUpdateNarrativeFontSize,
                onToggleCompactCombatLog = onToggleCompactCombatLog,
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
    mailNotificationsEnabled: Boolean,
    combatFlairEnabled: Boolean,
    useLightTheme: Boolean,
    narrativeFontSizeSp: Int,
    compactCombatLog: Boolean,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    onToggleMailNotifications: (Boolean) -> Unit,
    onToggleCombatFlair: (Boolean) -> Unit,
    onToggleLightTheme: (Boolean) -> Unit,
    onUpdateNarrativeFontSize: (Int) -> Unit,
    onToggleCompactCombatLog: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingPermTarget by remember { mutableStateOf<String?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        when (pendingPermTarget) {
            "newday" -> onToggleNewDayNotifications(granted)
            "mail" -> onToggleMailNotifications(granted)
        }
        pendingPermTarget = null
    }

    fun requestNotifToggle(target: String, wantEnabled: Boolean, onApply: (Boolean) -> Unit) {
        if (!wantEnabled) {
            onApply(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onApply(true)
            } else {
                pendingPermTarget = target
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onApply(true)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

        // Appearance: Light / Dark Theme + Narrative Font Size
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
                            text = "Syndicate Light Theme",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Clean daylight palette with automatic LotGD text contrast.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = useLightTheme,
                        onCheckedChange = onToggleLightTheme,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }

                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Narrative Font Size",
                        color = DccColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "${narrativeFontSizeSp}sp",
                        color = DccColors.Cyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = narrativeFontSizeSp.toFloat(),
                    onValueChange = { raw ->
                        onUpdateNarrativeFontSize(raw.roundToInt().coerceIn(13, 20))
                    },
                    valueRange = 13f..20f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = DccColors.Cyan,
                        activeTrackColor = DccColors.Cyan,
                    )
                )
            }
        }

        // Auto-Heal Threshold
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

        // Combat & Casino Flair + Compact Combat Log
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
                            text = "Combat Shake, Flash & Casino Animations",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Adds subtle screen shake, hit flashes, haptics, and spinning slot reels.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = combatFlairEnabled,
                        onCheckedChange = onToggleCombatFlair,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }

                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compact Combat Log",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Hides redundant HP lines below the Battle HUD for cleaner combat rounds.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = compactCombatLog,
                        onCheckedChange = onToggleCompactCombatLog,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }
            }
        }

        // Notifications: New Game Day + In-Game Mail
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
                        onCheckedChange = { want ->
                            requestNotifToggle("newday", want, onToggleNewDayNotifications)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Gold,
                        )
                    )
                }

                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In-Game Mail Notifications",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Alerts when new Syndicate Comms arrive (checks every 15 min in background).",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = mailNotificationsEnabled,
                        onCheckedChange = { want ->
                            requestNotifToggle("mail", want, onToggleMailNotifications)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Gold,
                        )
                    )
                }
            }
        }

        // About App
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
    secondsPerGameDay: Int,
    modifier: Modifier = Modifier,
) {
    // Live 1-second ticker so Earth Time, Dungeon Clock, and Next Cycle tick in real time
    var elapsedSec by remember(sections) { mutableIntStateOf(0) }
    LaunchedEffect(sections) {
        elapsedSec = 0
        while (true) {
            delay(1000L)
            elapsedSec += 1
        }
    }

    val dungeonSpeedMultiplier = if (secondsPerGameDay > 0) {
        (86400.0 / secondsPerGameDay.toDouble()).roundToInt().coerceAtLeast(1)
    } else {
        4
    }

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
                val cleanItems = sec.items.filterNot { item ->
                    item.value.contains("PT (Vancouver)MT (Edmonton)") ||
                        (item.label.isBlank() && item.value.isBlank())
                }
                if (cleanItems.isEmpty()) continue

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
                        Spacer(Modifier.height(8.dp))
                        cleanItems.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = DccColors.Outline.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }

                            val displayValue = formatLiveClockValue(
                                label = item.label,
                                rawValue = item.value,
                                elapsedSec = elapsedSec,
                                dungeonMultiplier = dungeonSpeedMultiplier,
                                secondsPerGameDay = secondsPerGameDay,
                            )

                            val isLongValue = displayValue.length > 22
                            if (isLongValue) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (item.label.isNotBlank()) {
                                        Text(
                                            text = item.label,
                                            color = DccColors.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Text(
                                        text = displayValue,
                                        color = DccColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 18.sp,
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.label,
                                        color = DccColors.TextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = displayValue,
                                        color = DccColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val EARTH_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US)
private val HMS_REGEX = Regex("""(\d{1,2}):(\d{2}):(\d{2})""")

private fun formatLiveClockValue(
    label: String,
    rawValue: String,
    elapsedSec: Int,
    dungeonMultiplier: Int,
    secondsPerGameDay: Int,
): String {
    val lower = label.lowercase()
    return when {
        lower.contains("earth time") -> {
            LocalTime.now().format(EARTH_TIME_FORMATTER)
        }
        lower.contains("dungeon clock") -> {
            val m = HMS_REGEX.find(rawValue) ?: return rawValue
            val baseSec = (m.groupValues[1].toInt() * 3600) +
                (m.groupValues[2].toInt() * 60) +
                m.groupValues[3].toInt()
            val liveSec = (baseSec + elapsedSec * dungeonMultiplier) % 86400
            val h = liveSec / 3600
            val min = (liveSec % 3600) / 60
            val s = liveSec % 60
            String.format(Locale.US, "%02d:%02d:%02d", h, min, s)
        }
        lower.contains("next cycle") -> {
            val m = HMS_REGEX.find(rawValue) ?: return rawValue
            val baseRem = (m.groupValues[1].toInt() * 3600) +
                (m.groupValues[2].toInt() * 60) +
                m.groupValues[3].toInt()
            val cycleLen = if (secondsPerGameDay > 0) secondsPerGameDay else 21600
            val rem = (baseRem - elapsedSec).let { if (it < 0) (cycleLen + (it % cycleLen)) % cycleLen else it }
            val h = rem / 3600
            val min = (rem % 3600) / 60
            val s = rem % 60
            val pct = ((cycleLen - rem).toDouble() / cycleLen.toDouble() * 100.0).coerceIn(0.0, 100.0)
            String.format(Locale.US, "%02d:%02d:%02d (%.1f%%)", h, min, s, pct)
        }
        else -> rawValue
    }
}
