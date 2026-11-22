package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.features.CommandRouter
import com.example.features.CommandSuggestion
import com.example.memory.ChatMessageEntry
import com.example.memory.JaniLanguage
import com.example.memory.UiState
import com.example.memory.UserProfileSettings
import com.example.ui.components.HoloGlassCard
import com.example.ui.components.HolographicOrb
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.theme.HoloBorder
import com.example.ui.theme.HoloBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloGlass
import com.example.ui.theme.HoloGlassElevated
import com.example.ui.theme.HoloMint
import com.example.ui.theme.HoloViolet
import com.example.ui.theme.HoloVoid
import com.example.viewmodel.JaniViewModel

@Composable
fun CoreScreen(
    viewModel: JaniViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val activeCommand by viewModel.activeCommand.collectAsStateWithLifecycle()

    val isListening by viewModel.voiceManager.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by viewModel.voiceManager.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.voiceManager.partialTranscript.collectAsStateWithLifecycle()
    val voiceError by viewModel.voiceManager.voiceError.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: UserProfileSettings()
    val selectedLanguage = JaniLanguage.fromCode(profile.preferredLanguage)

    var draftText by rememberSaveable { mutableStateOf("") }
    var commandFeedback by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceCapture()
        } else {
            commandFeedback = "Microphone permission denied. Type below or grant mic permission in Settings."
        }
    }

    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + if (streamingReply.isNotEmpty() || isThinking) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Neural Core HUD Header Card
        HoloGlassCard(
            borderColor = if (isListening) HoloMint else if (isThinking) HoloViolet else HoloBorderGlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HolographicOrb(
                        isListening = isListening,
                        isThinking = isThinking,
                        isSpeaking = isSpeaking,
                        audioLevel = audioLevel,
                        size = 66.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isListening -> stringResource(R.string.core_status_listening)
                                isThinking -> stringResource(R.string.core_status_thinking)
                                isSpeaking -> stringResource(R.string.core_status_speaking)
                                else -> stringResource(R.string.core_status_ready)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isListening -> HoloMint
                                isThinking -> HoloViolet
                                else -> HoloCyan
                            }
                        )
                        Text(
                            text = "JANI AI • v1.0",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (partialTranscript.isNotBlank()) {
                            Text(
                                text = "“$partialTranscript”",
                                style = MaterialTheme.typography.bodySmall,
                                color = HoloMint,
                                maxLines = 2
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleAutoSpeak() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_auto_speak_button")
                    ) {
                        Icon(
                            imageVector = if (profile.autoSpeak) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = stringResource(R.string.core_tts_toggle_cd),
                            tint = if (profile.autoSpeak) HoloCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearConversation() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.core_clear_chat_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LanguageSelectorRow(
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { viewModel.updateLanguage(it) }
                )
            }
        }

        // Transient status / error / voice notice
        val bannerText = chatError ?: voiceError ?: statusBanner ?: commandFeedback
        AnimatedVisibility(visible = !bannerText.isNullOrBlank()) {
            Surface(
                color = if (chatError != null || voiceError != null) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = MaterialTheme.shapes.small,
                onClick = {
                    commandFeedback = null
                    viewModel.dismissBanner()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bannerText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chatError != null || voiceError != null) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Conversation Transcript
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HoloCyan)
                    }
                }
                is UiState.Error -> {
                    HoloGlassCard(
                        borderColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty() && !isThinking && streamingReply.isEmpty()) {
                        EmptyCoreStarterPrompts(
                            onSelectPrompt = { prompt -> viewModel.sendMessage(prompt) }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data, key = { it.id }) { msg ->
                                ChatBubbleItem(
                                    message = msg,
                                    onReplaySpeech = {
                                        viewModel.voiceManager.speak(
                                            text = msg.text,
                                            language = JaniLanguage.fromCode(msg.language),
                                            speechRate = profile.speechRate.toFloat()
                                        )
                                    }
                                )
                            }
                            if (isThinking || streamingReply.isNotEmpty()) {
                                item(key = "streaming_turn") {
                                    StreamingAssistantBubble(text = streamingReply)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Command Launch Action Chip
        AnimatedVisibility(visible = activeCommand != null) {
            activeCommand?.let { cmd ->
                CommandActionBanner(
                    command = cmd,
                    onExecute = {
                        val result = CommandRouter.execute(context, cmd)
                        commandFeedback = result.message
                    }
                )
            }
        }

        // Bottom Voice-First Composer
        HoloGlassCard(
            borderColor = HoloBorderGlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        when {
                            isSpeaking -> viewModel.stopSpeaking()
                            isListening -> viewModel.stopVoiceCapture()
                            else -> {
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                    viewModel.startVoiceCapture()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            isListening -> HoloMint
                            isSpeaking -> HoloViolet
                            else -> HoloCyan
                        },
                        contentColor = HoloVoid
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening || isSpeaking) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListening) {
                            stringResource(R.string.core_stop_mic_cd)
                        } else {
                            stringResource(R.string.core_mic_cd)
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.core_input_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draftText.isNotBlank() && !isThinking) {
                                viewModel.sendMessage(draftText)
                                draftText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HoloCyan,
                        unfocusedBorderColor = HoloBorder,
                        focusedContainerColor = HoloVoid.copy(alpha = 0.45f),
                        unfocusedContainerColor = HoloVoid.copy(alpha = 0.35f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (draftText.isNotBlank() && !isThinking) {
                            viewModel.sendMessage(draftText)
                            draftText = ""
                        }
                    },
                    enabled = draftText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.core_send_cd),
                        tint = if (draftText.isNotBlank() && !isThinking) HoloCyan else HoloBorder
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCoreStarterPrompts(
    onSelectPrompt: (String) -> Unit
) {
    val starters = remember {
        listOf(
            "Remember that my favorite drink is cardamom chai.",
            "Salam Jani! Mera naam yaad rakhna aur Roman Urdu mein baat karo.",
            "Namaste Jani, aaj ke liye 3 productivity goals suggest karo.",
            "Set a 5 minute focus timer."
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JANI MULTILINGUAL NEURAL LINK",
            style = MaterialTheme.typography.labelMedium,
            color = HoloCyan
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap the mic or pick a starter prompt in English, Urdu, or Hindi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        starters.forEach { prompt ->
            AssistChip(
                onClick = { onSelectPrompt(prompt) },
                label = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = HoloGlass.copy(alpha = 0.8f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = HoloBorder
                ),
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .minimumInteractiveComponentSize()
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntry,
    onReplaySpeech: () -> Unit
) {
    val isAssistant = message.role == "assistant"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
    ) {
        HoloGlassCard(
            borderColor = if (isAssistant) HoloCyan.copy(alpha = 0.5f) else HoloViolet.copy(alpha = 0.6f),
            containerColor = if (isAssistant) HoloGlass.copy(alpha = 0.85f) else HoloGlassElevated.copy(alpha = 0.9f),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAssistant) "JANI • ${message.language}" else "YOU • ${message.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAssistant) HoloCyan else HoloViolet
                    )
                    if (isAssistant) {
                        IconButton(
                            onClick = onReplaySpeech,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak reply aloud",
                                tint = HoloCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        HoloGlassCard(
            borderColor = HoloCyan,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = HoloCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JANI SYNTHESIZING…",
                        style = MaterialTheme.typography.labelSmall,
                        color = HoloCyan
                    )
                }
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandActionBanner(
    command: CommandSuggestion,
    onExecute: () -> Unit
) {
    HoloGlassCard(
        borderColor = HoloMint,
        containerColor = HoloGlassElevated.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = HoloMint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "READY COMMAND: ${command.type.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HoloMint
                    )
                    if (command.argument.isNotBlank()) {
                        Text(
                            text = command.argument,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HoloMint,
                    contentColor = HoloVoid
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("launch_command_button")
            ) {
                Text("Launch", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
