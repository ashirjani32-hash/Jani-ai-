package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.memory.JaniLanguage
import com.example.memory.UiState
import com.example.memory.UserProfileSettings
import com.example.ui.components.HoloGlassCard
import com.example.ui.theme.HoloBorder
import com.example.ui.theme.HoloBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloMint
import com.example.ui.theme.HoloRose
import com.example.ui.theme.HoloViolet
import com.example.ui.theme.HoloVoid
import com.example.viewmodel.ImageGenUiState
import com.example.viewmodel.JaniViewModel
import com.example.viewmodel.VisionAnalysisUiState
import com.example.viewmodel.VisionLabViewModel

@Composable
fun VisionLabScreen(
    visionViewModel: VisionLabViewModel,
    janiViewModel: JaniViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileState by janiViewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data ?: UserProfileSettings()
    val preferredLanguage = JaniLanguage.fromCode(profile.preferredLanguage)

    val imageGenState by visionViewModel.imageGenState.collectAsStateWithLifecycle()
    val visionAnalysisState by visionViewModel.visionAnalysisState.collectAsStateWithLifecycle()

    var isGenerateMode by rememberSaveable { mutableStateOf(true) }
    var imagePrompt by rememberSaveable { mutableStateOf("") }
    var analysisQuestion by rememberSaveable { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedBitmap = decodeScaledBitmap(context, uri, maxSize = 1280)
            visionViewModel.resetAnalysis()
        }
    }

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
                    text = stringResource(R.string.vision_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = HoloCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by Gemini Nano Banana 2.1 & Gemini 3.8 Flash",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isGenerateMode,
                        onClick = { isGenerateMode = true },
                        leadingIcon = {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(stringResource(R.string.vision_mode_generate)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = HoloCyan
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vision_mode_generate_chip")
                    )
                    FilterChip(
                        selected = !isGenerateMode,
                        onClick = { isGenerateMode = false },
                        leadingIcon = {
                            Icon(Icons.Filled.ImageSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(stringResource(R.string.vision_mode_analyze)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = HoloViolet
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vision_mode_analyze_chip")
                    )
                }
            }
        }

        if (isGenerateMode) {
            HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        placeholder = { Text(stringResource(R.string.vision_prompt_hint)) },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HoloCyan,
                            unfocusedBorderColor = HoloBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_gen_prompt_input")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "Futuristic holographic Islamabad skyline at night",
                            "Cybernetic glowing AI neural crystal on dark obsidian",
                            "Minimalist sci-fi workspace with neon cyan HUD"
                        )
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = { imagePrompt = preset },
                                label = { Text(preset.take(26) + "…", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }

                    Button(
                        onClick = { visionViewModel.generateImage(imagePrompt) },
                        enabled = imagePrompt.isNotBlank() && imageGenState !is ImageGenUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloCyan,
                            contentColor = HoloVoid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_image_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.vision_generate_btn))
                    }
                }
            }

            HoloGlassCard(
                borderColor = if (imageGenState is ImageGenUiState.Error) HoloRose else HoloBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = imageGenState) {
                        is ImageGenUiState.Idle -> {
                            Text(
                                text = "Enter a visual concept above to synthesize artwork with Gemini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is ImageGenUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 24.dp)
                            ) {
                                CircularProgressIndicator(color = HoloCyan)
                                Text(
                                    text = "Synthesizing image (typically 30–60s)…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HoloCyan
                                )
                            }
                        }
                        is ImageGenUiState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Image(
                                    bitmap = state.bitmap.asImageBitmap(),
                                    contentDescription = state.prompt,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Text(
                                    text = state.prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is ImageGenUiState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = HoloRose
                            )
                        }
                    }
                }
            }
        } else {
            HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_photo_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = HoloViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedBitmap == null) {
                                stringResource(R.string.vision_pick_photo)
                            } else {
                                "Change Photo"
                            },
                            color = HoloViolet
                        )
                    }

                    selectedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected photo for Jani analysis",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }

                    OutlinedTextField(
                        value = analysisQuestion,
                        onValueChange = { analysisQuestion = it },
                        placeholder = { Text(stringResource(R.string.vision_analyze_hint)) },
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HoloViolet,
                            unfocusedBorderColor = HoloBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vision_question_input")
                    )

                    Button(
                        onClick = {
                            selectedBitmap?.let { bmp ->
                                visionViewModel.analyzeImage(bmp, analysisQuestion, preferredLanguage)
                            }
                        },
                        enabled = selectedBitmap != null && visionAnalysisState !is VisionAnalysisUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloViolet,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_image_button")
                    ) {
                        Icon(Icons.Filled.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.vision_analyze_btn))
                    }
                }
            }

            when (val state = visionAnalysisState) {
                is VisionAnalysisUiState.Idle -> Unit
                is VisionAnalysisUiState.Loading -> {
                    HoloGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HoloViolet)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing visual data…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is VisionAnalysisUiState.Streaming -> {
                    HoloGlassCard(borderColor = HoloViolet, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is VisionAnalysisUiState.Success -> {
                    HoloGlassCard(borderColor = HoloMint, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "JANI VISION REPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = HoloMint
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is VisionAnalysisUiState.Error -> {
                    HoloGlassCard(borderColor = HoloRose, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.message,
                            color = HoloRose,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val width = raw.width
        val height = raw.height
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height, 1f)
        if (scale < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            raw
        }
    } catch (_: Exception) {
        null
    }
}
