package com.example.brain

import android.util.Log
import com.example.memory.ChatMessageEntry
import com.example.memory.JaniLanguage
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class BrainTurnAnalysis(
    val detectedLanguage: JaniLanguage,
    val shouldSaveMemory: Boolean,
    val memoryCategory: MemoryCategory,
    val memoryTitle: String,
    val memoryContent: String,
    val commandType: String?,
    val commandArg: String?
)

class GeminiBrainService {

    companion object {
        private const val TAG = "GeminiBrainService"
        private const val CHAT_TIMEOUT_MS = 60_000L
        private const val ANALYSIS_TIMEOUT_MS = 30_000L
    }

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.enumeration(
                values = listOf("EN", "UR", "HI"),
                description = "Primary language of the user message: EN, UR (Urdu/Roman Urdu), or HI (Hindi/Hinglish)."
            ),
            "shouldSaveMemory" to Schema.boolean(
                description = "True if the user shared a durable personal fact, preference, goal, task, or explicitly asked Jani to remember something."
            ),
            "memoryCategory" to Schema.enumeration(
                values = listOf("FACT", "PREFERENCE", "GOAL", "TASK", "NOTE"),
                description = "Category for the extracted memory."
            ),
            "memoryTitle" to Schema.string(
                description = "Concise 2 to 6 word title for the memory if shouldSaveMemory is true, else empty string."
            ),
            "memoryContent" to Schema.string(
                description = "Self-contained sentence summarizing what to remember if shouldSaveMemory is true, else empty string."
            ),
            "commandType" to Schema.enumeration(
                values = listOf("NONE", "WEB_SEARCH", "SET_TIMER", "DIAL_NUMBER", "COMPOSE_EMAIL", "OPEN_SETTINGS"),
                description = "Device intent command requested by the user, or NONE."
            ),
            "commandArg" to Schema.string(
                description = "Argument for the command (e.g. search query, timer seconds, phone number, email subject), or empty string."
            )
        )
    )

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    private val analyzerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = analysisSchema
                temperature = 0.1f
            }
        )
    }

    fun streamAssistantReply(
        userMessage: String,
        preferredLanguage: JaniLanguage,
        memories: List<MemoryEntry>,
        recentHistory: List<ChatMessageEntry>
    ): Flow<String> = flow {
        val memoryContext = if (memories.isEmpty()) {
            "No saved memories yet."
        } else {
            memories.take(25).joinToString("\n") { mem ->
                "- [${mem.category}] ${mem.title}: ${mem.content}"
            }
        }

        val systemInstruction = """
            You are JANI, a personal, voice-first intelligent AI assistant with a futuristic holographic interface.
            ${LanguageEngine.promptDirective(preferredLanguage)}

            PERMANENT SAVED MEMORIES FOR THIS USER:
            $memoryContext

            HONEST CAPABILITY RULES (STRICT):
            1. Never make false claims about capabilities you do not have.
            2. You CAN: converse naturally in English, Urdu, and Hindi; recall and reference the user's saved memories above; analyze or generate images inside the Vision Lab tab; and prepare Android device command intents (Web Search, Set Timer, Open Phone Dialer, Compose Email, Open Settings) for the user to launch with one tap.
            3. You CANNOT: silently browse live websites on your own, read private files outside the app, run Termux/shell commands directly, or place calls/send messages without opening the Android system app. If asked for real-time live web info (news/weather/prices), clearly state you cannot fetch live web pages yourself and offer a Web Search command.
            4. Keep spoken-friendly replies clear, warm, concise, and helpful.
        """.trimIndent()

        val historyContents = recentHistory.takeLast(8).map { entry ->
            content(role = if (entry.role == "assistant") "model" else "user") {
                text(entry.text)
            }
        }

        val promptContent = content("user") {
            text("$systemInstruction\n\nUser message: $userMessage")
        }

        try {
            withTimeout(CHAT_TIMEOUT_MS) {
                val chat = chatModel.startChat(history = historyContents)
                val fullText = StringBuilder()
                chat.sendMessageStream(promptContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        fullText.append(piece)
                        emit(fullText.toString())
                    }
                }
                if (fullText.isBlank()) {
                    throw IllegalStateException("Jani returned an empty response.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat generation timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat generation failed", e)
            throw e
        }
    }

    suspend fun analyzeTurn(
        userMessage: String,
        preferredLanguage: JaniLanguage
    ): BrainTurnAnalysis {
        val fallbackLang = LanguageEngine.detectLanguage(userMessage, preferredLanguage)
        return try {
            val prompt = """
                Analyze this user message sent to JANI AI assistant:
                "$userMessage"

                1. Detect language: EN, UR (Urdu or Roman Urdu), or HI (Hindi or Hinglish).
                2. Decide if this contains a durable personal fact, preference, goal, task, or explicit "remember / yaad rakhna" request worth saving permanently. Do not save transient greetings or generic questions.
                3. Detect if the user wants a supported Android device command:
                   - WEB_SEARCH (commandArg = search query)
                   - SET_TIMER (commandArg = duration in seconds as integer string, e.g. "300")
                   - DIAL_NUMBER (commandArg = phone number digits if provided, else "")
                   - COMPOSE_EMAIL (commandArg = subject line)
                   - OPEN_SETTINGS (commandArg = "")
                   - NONE (commandArg = "")
            """.trimIndent()

            val response = withTimeout(ANALYSIS_TIMEOUT_MS) {
                analyzerModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty analysis JSON")
            val json = JSONObject(rawJson)

            val lang = JaniLanguage.fromCode(json.optString("detectedLanguage", fallbackLang.name))
            val shouldSave = json.optBoolean("shouldSaveMemory", false)
            val category = MemoryCategory.fromCode(json.optString("memoryCategory", "FACT"))
            val title = json.optString("memoryTitle", "").trim()
            val contentText = json.optString("memoryContent", "").trim()
            val cmdType = json.optString("commandType", "NONE").takeIf { it != "NONE" && it.isNotBlank() }
            val cmdArg = json.optString("commandArg", "").trim().takeIf { it.isNotEmpty() }

            BrainTurnAnalysis(
                detectedLanguage = if (preferredLanguage == JaniLanguage.AUTO) lang else preferredLanguage,
                shouldSaveMemory = shouldSave && title.isNotEmpty() && contentText.isNotEmpty(),
                memoryCategory = category,
                memoryTitle = title,
                memoryContent = contentText,
                commandType = cmdType,
                commandArg = cmdArg
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Structured turn analysis skipped: ${e.message}")
            BrainTurnAnalysis(
                detectedLanguage = fallbackLang,
                shouldSaveMemory = false,
                memoryCategory = MemoryCategory.FACT,
                memoryTitle = "",
                memoryContent = "",
                commandType = null,
                commandArg = null
            )
        }
    }
}
