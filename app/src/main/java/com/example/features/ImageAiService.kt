package com.example.features

import android.graphics.Bitmap
import android.util.Log
import com.example.brain.LanguageEngine
import com.example.memory.JaniLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class ImageAiService {

    companion object {
        private const val TAG = "ImageAiService"
        private const val IMAGE_TIMEOUT_MS = 60_000L
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun generateImage(prompt: String): Bitmap {
        try {
            val response = withTimeout(IMAGE_TIMEOUT_MS) {
                imageGenModel.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            return imagePart?.image
                ?: throw IllegalStateException("No image was returned by Gemini Nano Banana.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Image generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    fun analyzeImage(
        bitmap: Bitmap,
        question: String,
        language: JaniLanguage
    ): Flow<String> = flow {
        val directive = LanguageEngine.promptDirective(language)
        val prompt = """
            $directive
            Analyze the provided image carefully and answer the user's question accurately.
            User question: ${question.ifBlank { "Describe what is visible in this image." }}
        """.trimIndent()

        try {
            withTimeout(IMAGE_TIMEOUT_MS) {
                val accumulated = StringBuilder()
                visionModel.generateContentStream(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                ).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
                if (accumulated.isBlank()) {
                    throw IllegalStateException("Vision model returned an empty analysis.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image analysis timed out", e)
            throw IllegalStateException("Image analysis timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image analysis failed", e)
            throw e
        }
    }
}
