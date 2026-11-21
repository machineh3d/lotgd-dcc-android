package ca.machineh3d.lotgddcc.model

import kotlinx.serialization.Serializable

@Serializable
data class DccPageResponse(
    val apiVersion: Int = 1,
    val ok: Boolean = true,
    val currentUrl: String = "/",
    val pageType: String = "general",
    val title: String = "Dungeon Crawler World: Earth",
    val badnav: Boolean = false,
    val recoveryUrl: String = "badnav.php",
    val player: PlayerState? = null,
    val mail: MailState = MailState(),
    val navGroups: List<NavGroup> = emptyList(),
    val charStats: List<CharStatSection> = emptyList(),
    val paragraphs: List<List<TextSpan>> = emptyList(),
    val tables: List<GameTable> = emptyList(),
    val forms: List<GameForm> = emptyList(),
)

@Serializable
data class GameTable(
    val headers: List<String> = emptyList(),
    val rows: List<TableRowData> = emptyList(),
)

@Serializable
data class TableRowData(
    val cells: List<String> = emptyList(),
    val actionUrl: String? = null,
    val actionLabel: String? = null,
)

@Serializable
data class PlayerState(
    val loggedIn: Boolean = false,
    val acctId: Int = 0,
    val login: String = "",
    val title: String = "",
    val name: String = "Crawler",
    val level: Int = 1,
    val maxLevel: Int = 15,
    val floor: Int = 1,
    val dragonKills: Int = 0,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val turns: Int = 0,
    val gold: Int = 0,
    val goldInBank: Int = 0,
    val gems: Int = 0,
    val experience: Int = 0,
    val expCurrentLevel: Int = 0,
    val expNextLevel: Int = 100,
    val alive: Boolean = true,
    val location: String = "Safe Room",
    val race: String = "",
    val specialty: String = "",
    val attack: Int = 0,
    val defense: Int = 0,
    val isSuperuser: Boolean = false,
)

@Serializable
data class MailState(
    val unread: Int = 0,
    val read: Int = 0,
    val label: String = "Syndicate Comms",
    val url: String = "mail.php",
)

@Serializable
data class NavGroup(
    val header: String = "Actions",
    val items: List<NavItem> = emptyList(),
)

@Serializable
data class NavItem(
    val label: String = "",
    val url: String = "",
    val hotkey: String = "",
)

@Serializable
data class CharStatSection(
    val section: String = "Crawler HUD",
    val items: List<CharStatItem> = emptyList(),
)

@Serializable
data class CharStatItem(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class TextSpan(
    val text: String = "",
    val color: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val link: String? = null,
)

@Serializable
data class GameForm(
    val id: String = "form_0",
    val action: String = "",
    val method: String = "POST",
    val submitLabel: String = "Submit",
    val fields: List<FormField> = emptyList(),
)

@Serializable
data class FormField(
    val name: String = "",
    val type: String = "text",
    val value: String = "",
    val label: String = "",
    val placeholder: String = "",
    val checked: Boolean = false,
    val options: List<SelectOption> = emptyList(),
)

@Serializable
data class SelectOption(
    val value: String = "",
    val label: String = "",
    val selected: Boolean = false,
)
