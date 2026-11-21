package ca.machineh3d.lotgddcc.ui.screens

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.CharStatSection
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.ui.components.ActionDeck
import ca.machineh3d.lotgddcc.ui.components.NarrativeView
import ca.machineh3d.lotgddcc.ui.components.NativeFormCard
import ca.machineh3d.lotgddcc.ui.components.NativeTableCards
import ca.machineh3d.lotgddcc.ui.components.TopHudBar
import ca.machineh3d.lotgddcc.ui.theme.DccColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    page: DccPageResponse,
    isLoading: Boolean,
    errorMessage: String?,
    showStatsSheet: Boolean,
    onNavigate: (String) -> Unit,
    onSubmitForm: (GameForm, Map<String, String>) -> Unit,
    onRecoverBadNav: () -> Unit,
    onOpenComms: () -> Unit,
    onToggleStatsSheet: (Boolean) -> Unit,
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
