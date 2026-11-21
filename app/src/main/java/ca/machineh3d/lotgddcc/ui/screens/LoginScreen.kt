package ca.machineh3d.lotgddcc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.BuildConfig
import ca.machineh3d.lotgddcc.ui.theme.DccColors

@Composable
fun LoginScreen(
    initialServerUrl: String,
    recentServerUrls: List<String>,
    initialRememberCredentials: Boolean,
    initialUsername: String,
    initialPassword: String,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (serverUrl: String, username: String, password: String, rememberCredentials: Boolean) -> Unit,
    onOpenRegistration: (serverUrl: String) -> Unit,
    onRemoveRecentUrl: (String) -> Unit,
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var rememberCredentials by remember { mutableStateOf(initialRememberCredentials) }
    var passwordVisible by remember { mutableStateOf(false) }
    var recentMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialServerUrl) {
        if (serverUrl.isBlank() && initialServerUrl.isNotBlank()) {
            serverUrl = initialServerUrl
        }
    }
    LaunchedEffect(initialUsername) {
        if (username.isBlank() && initialUsername.isNotBlank()) {
            username = initialUsername
        }
    }
    LaunchedEffect(initialPassword, initialRememberCredentials) {
        rememberCredentials = initialRememberCredentials
        if (password.isBlank() && initialRememberCredentials && initialPassword.isNotBlank()) {
            password = initialPassword
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DccColors.Background)
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BORANT SYNDICATE BROADCAST",
                color = DccColors.Crimson,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Dungeon Crawler World",
                color = DccColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Native Crawler HUD · lotgd-dcc",
                color = DccColors.Cyan,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(24.dp))

            Surface(
                color = DccColors.Surface,
                border = BorderStroke(1.dp, DccColors.Outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://dcc.yourdomain.com") },
                            leadingIcon = {
                                Icon(Icons.Filled.Public, contentDescription = null, tint = DccColors.Cyan)
                            },
                            trailingIcon = if (recentServerUrls.isNotEmpty()) {
                                {
                                    IconButton(onClick = { recentMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Recent server URLs",
                                            tint = DccColors.Cyan,
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = recentMenuExpanded && recentServerUrls.isNotEmpty(),
                            onDismissRequest = { recentMenuExpanded = false },
                            containerColor = DccColors.SurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            recentServerUrls.forEach { recent ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = recent,
                                                color = DccColors.TextPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { onRemoveRecentUrl(recent) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove URL",
                                                    tint = DccColors.TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        serverUrl = recent
                                        recentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Crawler Login") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberCredentials = !rememberCredentials }
                    ) {
                        Checkbox(
                            checked = rememberCredentials,
                            onCheckedChange = { rememberCredentials = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DccColors.Cyan,
                                checkmarkColor = DccColors.Background,
                            )
                        )
                        Text(
                            text = "Save login credentials on this device",
                            color = DccColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = DccColors.Crimson.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, DccColors.Crimson.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = DccColors.TextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        enabled = !isLoading,
                        onClick = { onLogin(serverUrl, username, password, rememberCredentials) },
                        colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = DccColors.TextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Enter the World Dungeon", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    OutlinedButton(
                        enabled = !isLoading,
                        onClick = { onOpenRegistration(serverUrl) },
                        border = BorderStroke(1.dp, DccColors.Outline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register New Crawler", color = DccColors.Cyan)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "App Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                color = DccColors.TextSecondary.copy(alpha = 0.75f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
