package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.features.CommandRouter
import com.example.features.CommandSuggestion
import com.example.features.JaniCommandType
import com.example.memory.JaniLanguage
import com.example.memory.UiState
import com.example.memory.UserProfileSettings
import com.example.ui.components.HoloGlassCard
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.theme.HoloAmber
import com.example.ui.theme.HoloBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloMint
import com.example.ui.theme.HoloRose
import com.example.ui.theme.HoloViolet
import com.example.viewmodel.JaniViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemsScreen(
    viewModel: JaniViewModel,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data ?: UserProfileSettings()
    val selectedLang = JaniLanguage.fromCode(profile.preferredLanguage)
    var commandResultMsg by remember { mutableStateOf<String?>(null) }

    val user = Firebase.auth.currentUser

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HoloGlassCard(
            borderColor = HoloBorderGlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.systems_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = HoloCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modular Command Console, Voice Calibration & Honest Capabilities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Command Console
        HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "VERIFIED ANDROID COMMANDS",
                    style = MaterialTheme.typography.labelMedium,
                    color = HoloMint
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommandButton(
                        icon = Icons.Filled.Search,
                        label = "Web Search",
                        onClick = {
                            val res = CommandRouter.execute(
                                context,
                                CommandSuggestion(JaniCommandType.WEB_SEARCH, "Jani AI news")
                            )
                            commandResultMsg = res.message
                        }
                    )
                    CommandButton(
                        icon = Icons.Filled.Alarm,
                        label = "5m Timer",
                        onClick = {
                            val res = CommandRouter.execute(
                                context,
                                CommandSuggestion(JaniCommandType.SET_TIMER, "300")
                            )
                            commandResultMsg = res.message
                        }
                    )
                    CommandButton(
                        icon = Icons.Filled.Phone,
                        label = "Dialer",
                        onClick = {
                            val res = CommandRouter.execute(
                                context,
                                CommandSuggestion(JaniCommandType.DIAL_NUMBER, "")
                            )
                            commandResultMsg = res.message
                        }
                    )
                    CommandButton(
                        icon = Icons.Filled.Email,
                        label = "Email",
                        onClick = {
                            val res = CommandRouter.execute(
                                context,
                                CommandSuggestion(JaniCommandType.COMPOSE_EMAIL, "Note from Jani AI")
                            )
                            commandResultMsg = res.message
                        }
                    )
                    CommandButton(
                        icon = Icons.Filled.Settings,
                        label = "Settings",
                        onClick = {
                            val res = CommandRouter.execute(
                                context,
                                CommandSuggestion(JaniCommandType.OPEN_SETTINGS, "")
                            )
                            commandResultMsg = res.message
                        }
                    )
                }

                if (!commandResultMsg.isNullOrBlank()) {
                    Text(
                        text = commandResultMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = HoloCyan
                    )
                }
            }
        }

        // Voice & Language Engine
        HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "VOICE & LANGUAGE ENGINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = HoloCyan
                )
                LanguageSelectorRow(
                    selectedLanguage = selectedLang,
                    onSelectLanguage = { viewModel.updateLanguage(it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Speak Replies", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Speak Jani's responses aloud via Android TTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.autoSpeak,
                        onCheckedChange = { viewModel.toggleAutoSpeak() }
                    )
                }

                Column {
                    Text(
                        text = "Speech Rate: ${String.format("%.2fx", profile.speechRate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = profile.speechRate.toFloat().coerceIn(0.5f, 2.0f),
                        onValueChange = { viewModel.updateSpeechRate(it.toDouble()) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = HoloCyan,
                            activeTrackColor = HoloCyan
                        )
                    )
                }
            }
        }

        // Honest Capability Boundaries (Anti-Hallucination Transparency)
        HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TRANSPARENT CAPABILITY CONTRACT",
                    style = MaterialTheme.typography.labelMedium,
                    color = HoloAmber
                )
                CapabilityRow(
                    supported = true,
                    text = "Gemini 3.8 Flash multilingual chat (English, Urdu, Hindi)"
                )
                CapabilityRow(
                    supported = true,
                    text = "Permanent per-user Firestore memory across restarts"
                )
                CapabilityRow(
                    supported = true,
                    text = "Voice input & TTS playback + Gemini Nano Banana 2.1 Image AI"
                )
                CapabilityRow(
                    supported = false,
                    text = "No silent background phone calls, OS shell access, or live autonomous web scraping (launches Android Intents with your confirmation instead)"
                )
            }
        }

        // Account & Security Card
        HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = HoloMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE CLOUD IDENTITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = HoloMint
                    )
                }

                Text(
                    text = user?.email ?: user?.displayName ?: "Authenticated Google User",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Firebase App Check & Zero-Trust Firestore Isolation Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = onSignedOut
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HoloRose.copy(alpha = 0.2f),
                        contentColor = HoloRose
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.systems_sign_out))
                }
            }
        }
    }
}

@Composable
private fun CommandButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Icon(icon, contentDescription = null, tint = HoloCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CapabilityRow(supported: Boolean, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = if (supported) Icons.Filled.CheckCircle else Icons.Filled.Info,
            contentDescription = null,
            tint = if (supported) HoloMint else HoloAmber,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
