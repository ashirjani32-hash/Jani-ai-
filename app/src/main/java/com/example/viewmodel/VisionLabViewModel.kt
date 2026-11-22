package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.features.ImageAiService
import com.example.memory.JaniLanguage
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap, val prompt: String) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

sealed interface VisionAnalysisUiState {
    data object Idle : VisionAnalysisUiState
    data object Loading : VisionAnalysisUiState
    data class Streaming(val text: String) : VisionAnalysisUiState
    data class Success(val text: String) : VisionAnalysisUiState
    data class Error(val message: String) : VisionAnalysisUiState
}

class VisionLabViewModel(
    private val imageAiService: ImageAiService
) : ViewModel() {

    companion object {
        private const val TAG = "VisionLabVM"

        fun provideFactory(imageAiService: ImageAiService): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { VisionLabViewModel(imageAiService) }
            }
    }

    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    private val _visionAnalysisState = MutableStateFlow<VisionAnalysisUiState>(VisionAnalysisUiState.Idle)
    val visionAnalysisState: StateFlow<VisionAnalysisUiState> = _visionAnalysisState.asStateFlow()

    fun generateImage(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return
        viewModelScope.launch {
            _imageGenState.value = ImageGenUiState.Loading
            try {
                val bitmap = imageAiService.generateImage(cleanPrompt)
                _imageGenState.value = ImageGenUiState.Success(bitmap, cleanPrompt)
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Image generation quota exceeded", e)
                _imageGenState.value = ImageGenUiState.Error(
                    "Image synthesis rate limit exceeded. Ensure Firebase Blaze plan or try again later."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageGenState.value = ImageGenUiState.Error(
                    e.localizedMessage ?: "Image generation failed."
                )
            }
        }
    }

    fun analyzeImage(bitmap: Bitmap, question: String, language: JaniLanguage) {
        viewModelScope.launch {
            _visionAnalysisState.value = VisionAnalysisUiState.Loading
            var finalOutput = ""
            try {
                imageAiService.analyzeImage(bitmap, question, language).collect { partial ->
                    finalOutput = partial
                    _visionAnalysisState.value = VisionAnalysisUiState.Streaming(partial)
                }
                if (finalOutput.isNotBlank()) {
                    _visionAnalysisState.value = VisionAnalysisUiState.Success(finalOutput)
                } else {
                    _visionAnalysisState.value = VisionAnalysisUiState.Error("No analysis returned.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision analysis failed", e)
                _visionAnalysisState.value = VisionAnalysisUiState.Error(
                    e.localizedMessage ?: "Failed to analyze image."
                )
            }
        }
    }

    fun resetAnalysis() {
        _visionAnalysisState.value = VisionAnalysisUiState.Idle
    }
}
