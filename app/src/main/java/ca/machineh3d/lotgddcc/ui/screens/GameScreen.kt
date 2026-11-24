package ca.machineh3d.lotgddcc.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import ca.machineh3d.lotgddcc.audio.SoundFxManager
import ca.machineh3d.lotgddcc.data.AppThemeMode
import ca.machineh3d.lotgddcc.data.AppUpdateInfo
import ca.machineh3d.lotgddcc.model.CharStatSection
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.ui.SessionTelemetry
import ca.machineh3d.lotgddcc.ui.components.ActionDeck
import ca.machineh3d.lotgddcc.ui.components.InlineChoiceDeck
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
    pvpNotificationsEnabled: Boolean,
    notificationSoundsEnabled: Boolean,
    combatFlairEnabled: Boolean,
    combatFlairIntensity: Int,
    combatSoundsEnabled: Boolean,
    menuSoundsEnabled: Boolean,
    pulseTrainIndicator: Boolean,
    keepScreenAwake: Boolean,
    themeMode: AppThemeMode,
    narrativeFontSizeSp: Int,
    compactCombatLog: Boolean,
    crtScanlinesEnabled: Boolean,
    hudVignetteEnabled: Boolean,
    neonEdgeGlowEnabled: Boolean,
    autoCheckUpdatesEnabled: Boolean,
    availableUpdate: AppUpdateInfo?,
    isCheckingUpdate: Boolean,
    isDownloadingUpdate: Boolean,
    updateDownloadProgress: Float,
    updateCheckStatus: String?,
    sessionTelemetry: SessionTelemetry,
    friendsOnlineCount: Int,
    friendsPendingCount: Int,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSubmitForm: (GameForm, Map<String, String>) -> Unit,
    onRecoverBadNav: () -> Unit,
    onOpenComms: () -> Unit,
    onOpenFriends: () -> String?,
    onPollFriends: () -> Unit,
    onToggleStatsSheet: (Boolean) -> Unit,
    onToggleSettingsSheet: (Boolean) -> Unit,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    onToggleMailNotifications: (Boolean) -> Unit,
    onTogglePvpNotifications: (Boolean) -> Unit,
    onToggleNotificationSounds: (Boolean) -> Unit,
    onUpdateCombatFlair: (Boolean, Int) -> Unit,
    onToggleCombatSounds: (Boolean) -> Unit,
    onToggleMenuSounds: (Boolean) -> Unit,
    onTogglePulseTrain: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateNarrativeFontSize: (Int) -> Unit,
    onToggleCompactCombatLog: (Boolean) -> Unit,
    onToggleCrtScanlines: (Boolean) -> Unit,
    onToggleHudVignette: (Boolean) -> Unit,
    onToggleNeonEdgeGlow: (Boolean) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Scalable combat shake (X + Y) and hit/attack flash state
    val shakeOffsetX = remember { Animatable(0f) }
    val shakeOffsetY = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(DccColors.Crimson) }
    var previousHp by remember { mutableStateOf<Int?>(null) }

    var lastBackPressMs by remember { mutableLongStateOf(0L) }
    val allNavItems = remember(page.navGroups) {
        page.navGroups.flatMap { it.items }
    }
    val isAtSafeRoomHub = page.currentUrl.contains("village.php", ignoreCase = true) ||
        page.pageType == "village"

    BackHandler(enabled = showStatsSheet || showSettingsSheet || !isAtSafeRoomHub || (System.currentTimeMillis() - lastBackPressMs > 2000L)) {
        when {
            showStatsSheet -> onToggleStatsSheet(false)
            showSettingsSheet -> onToggleSettingsSheet(false)
            !isAtSafeRoomHub -> {
                val returnLink = allNavItems.firstOrNull {
                    it.url.contains("village.php", ignoreCase = true) ||
                        it.label.contains("return", ignoreCase = true) ||
                        it.label.contains("back", ignoreCase = true) ||
                        it.label.contains("exit", ignoreCase = true)
                }
                if (returnLink != null) {
                    onNavigate(returnLink.url)
                } else if (page.currentUrl.contains("mail.php", ignoreCase = true)) {
                    onNavigate("village.php")
                } else {
                    Toast.makeText(context, "Use the combat or room actions below.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                lastBackPressMs = System.currentTimeMillis()
                Toast.makeText(context, "Press back again to exit Dungeon Crawler World", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(page.currentUrl, page.title, page.paragraphs.size, page.tables.size) {
        scrollState.scrollTo(0)

        val currentHp = page.player?.hp
        val prev = previousHp
        previousHp = currentHp

        if (page.paragraphs.isNotEmpty()) {
            val plain = page.paragraphs.joinToString(" ") { line -> line.joinToString("") { it.text } }
            val isCorridorOrFight = page.pageType == "combat" ||
                page.currentUrl.contains("forest.php", ignoreCase = true) ||
                page.currentUrl.contains("battle", ignoreCase = true) ||
                page.currentUrl.contains("pvp.php", ignoreCase = true) ||
                page.currentUrl.contains("dragon.php", ignoreCase = true) ||
                page.currentUrl.contains("graveyard.php", ignoreCase = true) ||
                plain.contains("'s Hitpoints", ignoreCase = true)

            val gotAchievementOrLoot = plain.contains("NEW ACHIEVEMENT", ignoreCase = true) ||
                plain.contains("SECRET ACHIEVEMENT", ignoreCase = true) ||
                plain.contains("LOOT BOX", ignoreCase = true)
            val wonFight = plain.contains("You have defeated", ignoreCase = true) ||
                plain.contains("You have slain", ignoreCase = true) ||
                plain.contains("HOSTILE ELIMINATED", ignoreCase = true)
            val isExplosion = listOf(
                "dynamite", "hoblobber", "jug o' boom", "bomb", "explod",
                "detonat", "grenade", "blast", "shrapnel", "boom",
            ).any { plain.contains(it, ignoreCase = true) }
            val isMagicSpell = listOf(
                "cast ", "casts ", "spell", "arcane", "magic", "lightning",
                "fireball", "frost", "curse", "ritual", "summon", "aura",
            ).any { plain.contains(it, ignoreCase = true) }
            val isShieldBlock = listOf(
                "riposte", "block", "parry", "deflect", "absorbs",
            ).any { plain.contains(it, ignoreCase = true) }
            val isMissWhoosh = listOf(
                "tries to hit you but", "misses you", "you miss", "dodges", "evades",
            ).any { plain.contains(it, ignoreCase = true) }
            val tookDamage = (prev != null && currentHp != null && currentHp < prev) ||
                plain.contains("hits you for", ignoreCase = true) ||
                plain.contains("strikes you for", ignoreCase = true)
            val isCrit = plain.contains("CRITICAL", ignoreCase = true) ||
                plain.contains("POWER MOVE", ignoreCase = true)
            val landedHit = plain.contains("You hit ", ignoreCase = true) ||
                plain.contains("You strike ", ignoreCase = true) ||
                isCrit || isShieldBlock || wonFight

            // Ability-matched RPG sound effects (Corridors & Fights toggle)
            if (combatSoundsEnabled && (isCorridorOrFight || gotAchievementOrLoot || wonFight || tookDamage)) {
                when {
                    gotAchievementOrLoot -> SoundFxManager.playAchievementOrLoot()
                    isExplosion -> SoundFxManager.playExplosionBoom()
                    isMagicSpell -> SoundFxManager.playMagicSpell()
                    wonFight -> SoundFxManager.playVictoryFanfare()
                    isShieldBlock -> SoundFxManager.playShieldBlock()
                    isCrit -> SoundFxManager.playCritSlash()
                    tookDamage -> SoundFxManager.playDamageTaken()
                    landedHit -> SoundFxManager.playSwordSlash()
                    isMissWhoosh -> SoundFxManager.playMissWhoosh()
                }
            }

            // Intensity-scaled visual shake & flash (20% subtle -> 100% intense)
            val scale = (combatFlairIntensity.coerceIn(20, 100) / 50f) // 0.4x .. 2.0x
            val flashDuration = (200 + (combatFlairIntensity * 1.4f)).toInt()

            if (gotAchievementOrLoot) {
                if (combatFlairEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    flashColor = DccColors.Gold
                    launch {
                        flashAlpha.snapTo((0.18f * scale).coerceAtMost(0.36f))
                        flashAlpha.animateTo(0f, animationSpec = tween(flashDuration))
                    }
                }
            } else if (tookDamage) {
                if (combatFlairEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    flashColor = DccColors.Crimson
                    launch {
                        flashAlpha.snapTo((0.22f * scale).coerceAtMost(0.46f))
                        flashAlpha.animateTo(0f, animationSpec = tween(flashDuration))
                    }
                    launch {
                        val baseOffsets = listOf(-14f, 14f, -11f, 11f, -7f, 7f, -3f, 0f)
                        for (dx in baseOffsets) {
                            shakeOffsetX.animateTo(dx * scale, animationSpec = tween(30))
                        }
                    }
                    if (combatFlairIntensity >= 65) {
                        launch {
                            val yScale = (combatFlairIntensity - 50) / 50f
                            for (dy in listOf(7f, -7f, 4f, -4f, 0f)) {
                                shakeOffsetY.animateTo(dy * yScale, animationSpec = tween(36))
                            }
                        }
                    }
                }
            } else if (landedHit || isExplosion || isMagicSpell) {
                if (combatFlairEnabled) {
                    haptic.performHapticFeedback(
                        if (isExplosion || isCrit) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                    flashColor = when {
                        isExplosion -> DccColors.Gold
                        isMagicSpell -> DccColors.Violet
                        else -> DccColors.Cyan
                    }
                    launch {
                        val peak = if (isExplosion || isCrit) 0.24f else 0.14f
                        flashAlpha.snapTo((peak * scale).coerceAtMost(0.42f))
                        flashAlpha.animateTo(0f, animationSpec = tween(flashDuration))
                    }
                    if (isExplosion || isCrit || combatFlairIntensity >= 75) {
                        launch {
                            val amp = if (isExplosion || isCrit) 12f * scale else 6f * scale
                            for (dx in listOf(-amp, amp, -amp * 0.55f, amp * 0.55f, 0f)) {
                                shakeOffsetX.animateTo(dx, animationSpec = tween(30))
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeOffsetX.value
                translationY = shakeOffsetY.value
            }
    ) {
        Scaffold(
            containerColor = DccColors.Background,
            topBar = {
                Column {
                    TopHudBar(
                        title = page.title,
                        player = page.player,
                        mail = page.mail,
                        friendsOnline = friendsOnlineCount,
                        friendsPending = friendsPendingCount,
                        nextNewDaySeconds = remember(page.charStats, page.nextNewDaySeconds) {
                            val fromStats = page.charStats
                                .flatMap { it.items }
                                .firstOrNull { it.label.contains("next cycle", ignoreCase = true) }
                                ?.value
                                ?.let { v ->
                                    HMS_REGEX.find(v)?.let { m ->
                                        m.groupValues[1].toInt() * 3600 +
                                            m.groupValues[2].toInt() * 60 +
                                            m.groupValues[3].toInt()
                                    }
                                }
                            fromStats ?: page.nextNewDaySeconds
                        },
                        secondsPerGameDay = page.secondsPerGameDay,
                        pulseTrainIndicator = pulseTrainIndicator,
                        onOpenStats = { onToggleStatsSheet(true) },
                        onOpenComms = onOpenComms,
                        onOpenFriends = {
                            val hint = onOpenFriends()
                            if (!hint.isNullOrBlank()) {
                                Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            }
                        },
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
                            val formFieldLabels = page.forms
                                .flatMap { it.fields }
                                .mapNotNull { it.label.trim().trimEnd(':', '?').lowercase().takeIf { s -> s.length >= 3 } }
                                .toSet()
                            val formSelectBlobs = page.forms
                                .flatMap { it.fields }
                                .filter { it.type == "select" && it.options.size >= 2 }
                                .map { f -> f.options.joinToString("") { it.label.trim() }.lowercase() }
                                .filter { it.length >= 4 }
                                .toSet()

                            page.tables.filterNot { table ->
                                val noActions = table.rows.none { !it.actionUrl.isNullOrBlank() }
                                if (!noActions) return@filterNot false

                                val noHeaders = table.headers.isEmpty()
                                val isKnownFormPage = page.currentUrl.contains("create.php", ignoreCase = true) ||
                                    page.currentUrl.contains("prefs.php", ignoreCase = true) ||
                                    page.currentUrl.contains("configuration.php", ignoreCase = true)

                                val matchedFormRows = table.rows.count { row ->
                                    val first = row.cells.firstOrNull()?.trim()?.trimEnd(':', '?')?.lowercase().orEmpty()
                                    val second = row.cells.getOrNull(1)?.trim()?.lowercase().orEmpty()
                                    val labelHit = first.length >= 3 && formFieldLabels.any { lbl ->
                                        first == lbl || first.contains(lbl) || lbl.contains(first)
                                    }
                                    val selectHit = second.length >= 4 && formSelectBlobs.any { blob ->
                                        second.replace(" ", "") == blob.replace(" ", "")
                                    }
                                    labelHit || selectHit
                                }

                                val looksLikeFormLabels = table.rows.any { row ->
                                    val first = row.cells.firstOrNull()?.trim().orEmpty()
                                    first.endsWith(":") || first.endsWith("?")
                                }

                                matchedFormRows >= 2 ||
                                    (isKnownFormPage && (matchedFormRows >= 1 || table.rows.isNotEmpty())) ||
                                    (noHeaders && looksLikeFormLabels)
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

                    InlineChoiceDeck(
                        pageType = page.pageType,
                        badnav = page.badnav,
                        navGroups = page.navGroups,
                        enabled = !isLoading,
                        onNavigate = onNavigate,
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

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
                sessionTelemetry = sessionTelemetry,
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
                pvpNotificationsEnabled = pvpNotificationsEnabled,
                notificationSoundsEnabled = notificationSoundsEnabled,
                combatFlairEnabled = combatFlairEnabled,
                combatFlairIntensity = combatFlairIntensity,
                combatSoundsEnabled = combatSoundsEnabled,
                menuSoundsEnabled = menuSoundsEnabled,
                pulseTrainIndicator = pulseTrainIndicator,
                keepScreenAwake = keepScreenAwake,
                themeMode = themeMode,
                narrativeFontSizeSp = narrativeFontSizeSp,
                compactCombatLog = compactCombatLog,
                crtScanlinesEnabled = crtScanlinesEnabled,
                hudVignetteEnabled = hudVignetteEnabled,
                neonEdgeGlowEnabled = neonEdgeGlowEnabled,
                autoCheckUpdatesEnabled = autoCheckUpdatesEnabled,
                availableUpdate = availableUpdate,
                isCheckingUpdate = isCheckingUpdate,
                isDownloadingUpdate = isDownloadingUpdate,
                updateDownloadProgress = updateDownloadProgress,
                updateCheckStatus = updateCheckStatus,
                onUpdateAutoHeal = onUpdateAutoHeal,
                onToggleNewDayNotifications = onToggleNewDayNotifications,
                onToggleMailNotifications = onToggleMailNotifications,
                onTogglePvpNotifications = onTogglePvpNotifications,
                onToggleNotificationSounds = onToggleNotificationSounds,
                onUpdateCombatFlair = onUpdateCombatFlair,
                onToggleCombatSounds = onToggleCombatSounds,
                onToggleMenuSounds = onToggleMenuSounds,
                onTogglePulseTrain = onTogglePulseTrain,
                onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                onUpdateThemeMode = onUpdateThemeMode,
                onUpdateNarrativeFontSize = onUpdateNarrativeFontSize,
                onToggleCompactCombatLog = onToggleCompactCombatLog,
                onToggleCrtScanlines = onToggleCrtScanlines,
                onToggleHudVignette = onToggleHudVignette,
                onToggleNeonEdgeGlow = onToggleNeonEdgeGlow,
                onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                onCheckForUpdates = onCheckForUpdates,
                onInstallUpdate = onInstallUpdate,
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
    pvpNotificationsEnabled: Boolean,
    notificationSoundsEnabled: Boolean,
    combatFlairEnabled: Boolean,
    combatFlairIntensity: Int,
    combatSoundsEnabled: Boolean,
    menuSoundsEnabled: Boolean,
    pulseTrainIndicator: Boolean,
    keepScreenAwake: Boolean,
    themeMode: AppThemeMode,
    narrativeFontSizeSp: Int,
    compactCombatLog: Boolean,
    crtScanlinesEnabled: Boolean,
    hudVignetteEnabled: Boolean,
    neonEdgeGlowEnabled: Boolean,
    autoCheckUpdatesEnabled: Boolean,
    availableUpdate: AppUpdateInfo?,
    isCheckingUpdate: Boolean,
    isDownloadingUpdate: Boolean,
    updateDownloadProgress: Float,
    updateCheckStatus: String?,
    onUpdateAutoHeal: (Boolean, Int) -> Unit,
    onToggleNewDayNotifications: (Boolean) -> Unit,
    onToggleMailNotifications: (Boolean) -> Unit,
    onTogglePvpNotifications: (Boolean) -> Unit,
    onToggleNotificationSounds: (Boolean) -> Unit,
    onUpdateCombatFlair: (Boolean, Int) -> Unit,
    onToggleCombatSounds: (Boolean) -> Unit,
    onToggleMenuSounds: (Boolean) -> Unit,
    onTogglePulseTrain: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onUpdateThemeMode: (AppThemeMode) -> Unit,
    onUpdateNarrativeFontSize: (Int) -> Unit,
    onToggleCompactCombatLog: (Boolean) -> Unit,
    onToggleCrtScanlines: (Boolean) -> Unit,
    onToggleHudVignette: (Boolean) -> Unit,
    onToggleNeonEdgeGlow: (Boolean) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
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
            "pvp" -> onTogglePvpNotifications(granted)
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

        if (availableUpdate != null) {
            Surface(
                color = DccColors.Cyan.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UPDATE AVAILABLE: ${availableUpdate.latestVersion}",
                                color = DccColors.Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                            )
                            Text(
                                text = if (!availableUpdate.apkDownloadUrl.isNullOrBlank()) {
                                    "Tap Install to download and update directly inside the app."
                                } else {
                                    "A newer release is live on GitHub Releases."
                                },
                                color = DccColors.TextPrimary,
                                fontSize = 12.sp,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                enabled = !isDownloadingUpdate,
                                onClick = {
                                    runCatching {
                                        val link = availableUpdate.apkDownloadUrl ?: availableUpdate.releaseUrl
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                    }
                                },
                                border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text("Link", color = DccColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!availableUpdate.apkDownloadUrl.isNullOrBlank()) {
                                Button(
                                    enabled = !isDownloadingUpdate,
                                    onClick = onInstallUpdate,
                                    colors = ButtonDefaults.buttonColors(containerColor = DccColors.Cyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    val btnLbl = if (isDownloadingUpdate) {
                                        "${(updateDownloadProgress * 100).roundToInt()}%"
                                    } else {
                                        "Install"
                                    }
                                    Text(btnLbl, color = DccColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (isDownloadingUpdate) {
                        LinearProgressIndicator(
                            progress = { updateDownloadProgress.coerceIn(0f, 1f) },
                            color = DccColors.Cyan,
                            trackColor = DccColors.Surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Display & Theme (Auto / Dark / Light + Font Size + Keep Screen Awake + Pulse TRAIN!)
        Surface(
            color = DccColors.SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "App Theme",
                    color = DccColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (mode in AppThemeMode.entries) {
                        val selected = themeMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) DccColors.Cyan.copy(alpha = 0.18f) else DccColors.Surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) DccColors.Cyan else DccColors.Outline,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onUpdateThemeMode(mode) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (selected) DccColors.Cyan else DccColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
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

                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pulse XP Bar When Ready to Train",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Gently pulses the gold TRAIN! indicator when you have enough XP to level up.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = pulseTrainIndicator,
                        onCheckedChange = onTogglePulseTrain,
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
                            text = "Keep Screen Awake",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Prevents your screen from timing out while playing.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = keepScreenAwake,
                        onCheckedChange = onToggleKeepScreenAwake,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }
            }
        }

        // Visual Effects & CRT Monitor Look
        Surface(
            color = DccColors.SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "VISUAL EFFECTS & CRT MONITOR",
                    color = DccColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CRT Scanline Overlay",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Adds subtle retro horizontal broadcast scanlines across the HUD.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = crtScanlinesEnabled,
                        onCheckedChange = onToggleCrtScanlines,
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
                            text = "Monitor Edge Vignette",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Soft darkened corners for a Borant Syndicate terminal frame feel.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = hudVignetteEnabled,
                        onCheckedChange = onToggleHudVignette,
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
                            text = "Syndicate Neon Edge Glow",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Faint cyan top and crimson bottom ambient screen edge glow.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = neonEdgeGlowEnabled,
                        onCheckedChange = onToggleNeonEdgeGlow,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }
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

        // Combat Flair (with Intensity Slider), Split Sound Controls & Compact Combat Log
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
                            text = "Combat Shake, Flash & Animations",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Screen shake, hit flashes, haptics, and spinning slot reels.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = combatFlairEnabled,
                        onCheckedChange = { onUpdateCombatFlair(it, combatFlairIntensity) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }

                if (combatFlairEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Effect Intensity",
                            color = DccColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        val intensityTag = when {
                            combatFlairIntensity >= 85 -> "INTENSE ($combatFlairIntensity%)"
                            combatFlairIntensity >= 55 -> "PUNCHY ($combatFlairIntensity%)"
                            else -> "SUBTLE ($combatFlairIntensity%)"
                        }
                        Text(
                            text = intensityTag,
                            color = DccColors.Crimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = combatFlairIntensity.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 10f).roundToInt() * 10).coerceIn(20, 100)
                            onUpdateCombatFlair(true, snapped)
                        },
                        valueRange = 20f..100f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = DccColors.Crimson,
                            activeTrackColor = DccColors.Crimson,
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
                            text = "Corridor & Combat Sound Effects",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Sword slashes, shield clangs, miss whooshes, explosions, spells, and loot.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = combatSoundsEnabled,
                        onCheckedChange = onToggleCombatSounds,
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
                            text = "Menu & Navigation Sounds",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Subtle UI clicks when switching rooms, tabs, menus, and incoming mail.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = menuSoundsEnabled,
                        onCheckedChange = onToggleMenuSounds,
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

        // Notifications
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

                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PvP Attack Notifications",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "High-priority alert if another Crawler attacks or slays you while offline.",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = pvpNotificationsEnabled,
                        onCheckedChange = { want ->
                            requestNotifToggle("pvp", want, onTogglePvpNotifications)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Crimson,
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
                            text = "Notification Sounds",
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Play system alert tone for New Day, Mail, and PvP notifications (off = silent).",
                            color = DccColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = notificationSoundsEnabled,
                        onCheckedChange = onToggleNotificationSounds,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }
            }
        }

        // About App + Update Check with live status feedback
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ABOUT APP",
                        color = DccColors.Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (isCheckingUpdate) "Checking..." else "Check Updates",
                            color = DccColors.Cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (!updateCheckStatus.isNullOrBlank()) {
                    Text(
                        text = updateCheckStatus,
                        color = if (updateCheckStatus.startsWith("Update available")) DccColors.Cyan else DccColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Check Updates on Launch",
                            color = DccColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Checks GitHub on the Login screen so you can update before signing in.",
                            color = DccColors.TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = autoCheckUpdatesEnabled,
                        onCheckedChange = onToggleAutoCheckUpdates,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DccColors.Background,
                            checkedTrackColor = DccColors.Cyan,
                        )
                    )
                }
                HorizontalDivider(color = DccColors.Outline.copy(alpha = 0.5f))
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
    sessionTelemetry: SessionTelemetry,
    modifier: Modifier = Modifier,
) {
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

        Surface(
            color = DccColors.SurfaceVariant,
            border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "SESSION RECAP",
                    color = DccColors.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SessionStatCell(
                        label = "KILLS",
                        value = "${sessionTelemetry.enemiesDefeated}",
                        accent = DccColors.Crimson,
                        modifier = Modifier.weight(1f),
                    )
                    SessionStatCell(
                        label = "XP",
                        value = "+${sessionTelemetry.xpGained}",
                        accent = DccColors.Violet,
                        modifier = Modifier.weight(1f),
                    )
                    SessionStatCell(
                        label = "GOLD",
                        value = formatSigned(sessionTelemetry.goldNet),
                        accent = DccColors.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    SessionStatCell(
                        label = "TOKENS",
                        value = formatSigned(sessionTelemetry.tokensNet),
                        accent = DccColors.Emerald,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

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

@Composable
private fun SessionStatCell(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = DccColors.Surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                text = label,
                color = DccColors.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Text(
                text = value,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private fun formatSigned(n: Int): String = when {
    n > 0 -> "+$n"
    else -> n.toString()
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
