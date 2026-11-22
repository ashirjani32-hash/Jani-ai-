package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.brain.GeminiBrainService
import com.example.brain.LanguageEngine
import com.example.features.CommandSuggestion
import com.example.features.JaniCommandType
import com.example.memory.ChatMessageEntry
import com.example.memory.JaniLanguage
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.example.memory.MemoryRepository
import com.example.memory.UiState
import com.example.memory.UserProfileSettings
import com.example.voice.VoiceManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JaniViewModel(
    private val repository: MemoryRepository,
    private val brainService: GeminiBrainService,
    val voiceManager: VoiceManager,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "JaniViewModel"
        private const val STOP_TIMEOUT_MS = 5000L

        fun provideFactory(
            repository: MemoryRepository,
            brainService: GeminiBrainService,
            voiceManager: VoiceManager,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JaniViewModel(
                    repository = repository,
                    brainService = brainService,
                    voiceManager = voiceManager,
                    currentUserId = currentUserId
                )
            }
        }
    }

    val profileState: StateFlow<UiState<UserProfileSettings>> = repository.observeProfile(currentUserId)
        .map<UserProfileSettings?, UiState<UserProfileSettings>> { profile ->
            UiState.Success(profile ?: UserProfileSettings(userId = currentUserId))
        }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Failed to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val memoriesState: StateFlow<UiState<List<MemoryEntry>>> = repository.observeMemories(currentUserId)
        .map<List<MemoryEntry>, UiState<List<MemoryEntry>>> { UiState.Success(it) }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Failed to load saved memories"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val messagesState: StateFlow<UiState<List<ChatMessageEntry>>> = repository.observeMessages(currentUserId)
        .map<List<ChatMessageEntry>, UiState<List<ChatMessageEntry>>> { UiState.Success(it) }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Failed to load conversation history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _streamingReply = MutableStateFlow("")
    val streamingReply: StateFlow<String> = _streamingReply.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _activeCommand = MutableStateFlow<CommandSuggestion?>(null)
    val activeCommand: StateFlow<CommandSuggestion?> = _activeCommand.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileExists(currentUserId).onFailure { err ->
                Log.w(TAG, "Profile initialization warning: ${err.message}")
            }
        }
    }

    private fun currentProfile(): UserProfileSettings {
        return (profileState.value as? UiState.Success)?.data
            ?: UserProfileSettings(userId = currentUserId)
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _isThinking.value) return

        val profile = currentProfile()
        val preferredLang = JaniLanguage.fromCode(profile.preferredLanguage)
        val memories = (memoriesState.value as? UiState.Success)?.data.orEmpty()
        val history = (messagesState.value as? UiState.Success)?.data.orEmpty()

        _chatError.value = null
        _activeCommand.value = null
        _isThinking.value = true
        _streamingReply.value = ""
        voiceManager.stopSpeaking()

        viewModelScope.launch {
            try {
                val detectedQuickLang = LanguageEngine.detectLanguage(text, preferredLang)
                val saveUserMsg = repository.addMessage(
                    userId = currentUserId,
                    role = "user",
                    text = text,
                    language = detectedQuickLang
                )
                saveUserMsg.onFailure { err ->
                    _chatError.value = err.localizedMessage ?: "Failed to save message."
                }

                val analysisDeferred = async {
                    brainService.analyzeTurn(text, preferredLang)
                }

                val replyBuilder = StringBuilder()
                brainService.streamAssistantReply(
                    userMessage = text,
                    preferredLanguage = preferredLang,
                    memories = memories,
                    recentHistory = history
                ).collect { partial ->
                    replyBuilder.clear()
                    replyBuilder.append(partial)
                    _streamingReply.value = partial
                }

                val finalReply = replyBuilder.toString().trim()
                val analysis = analysisDeferred.await()

                val commandType = JaniCommandType.fromCode(analysis.commandType)
                val commandSuggestion = if (commandType != null) {
                    CommandSuggestion(commandType, analysis.commandArg.orEmpty())
                } else {
                    null
                }
                _activeCommand.value = commandSuggestion

                if (analysis.shouldSaveMemory) {
                    repository.addMemory(
                        userId = currentUserId,
                        category = analysis.memoryCategory,
                        title = analysis.memoryTitle,
                        content = analysis.memoryContent,
                        source = "AUTO",
                        pinned = false
                    ).onSuccess {
                        _statusBanner.value = "Memory saved: ${analysis.memoryTitle}"
                    }
                }

                val commandLabel = commandSuggestion?.let {
                    "${it.type.name}:${it.argument}"
                }

                if (finalReply.isNotEmpty()) {
                    repository.addMessage(
                        userId = currentUserId,
                        role = "assistant",
                        text = finalReply,
                        language = analysis.detectedLanguage,
                        commandAction = commandLabel
                    ).onFailure { err ->
                        _chatError.value = err.localizedMessage ?: "Failed to persist reply."
                    }

                    if (profile.autoSpeak) {
                        voiceManager.speak(
                            text = finalReply,
                            language = analysis.detectedLanguage,
                            speechRate = profile.speechRate.toFloat()
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error during Jani turn", e)
                _chatError.value = e.localizedMessage ?: "Jani encountered an error generating a response."
            } finally {
                _isThinking.value = false
                _streamingReply.value = ""
            }
        }
    }

    fun startVoiceCapture() {
        val profile = currentProfile()
        val preferred = JaniLanguage.fromCode(profile.preferredLanguage)
        voiceManager.startListening(preferred) { transcript ->
            sendMessage(transcript)
        }
    }

    fun stopVoiceCapture() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun saveManualMemory(
        category: MemoryCategory,
        title: String,
        content: String,
        pinned: Boolean
    ) {
        viewModelScope.launch {
            repository.addMemory(
                userId = currentUserId,
                category = category,
                title = title,
                content = content,
                source = "MANUAL",
                pinned = pinned
            ).onSuccess {
                _statusBanner.value = "Stored memory: ${title.trim()}"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not save memory."
            }
        }
    }

    fun updateMemory(
        memoryId: String,
        category: MemoryCategory,
        title: String,
        content: String,
        pinned: Boolean
    ) {
        viewModelScope.launch {
            repository.updateMemory(
                userId = currentUserId,
                memoryId = memoryId,
                category = category,
                title = title,
                content = content,
                pinned = pinned
            ).onSuccess {
                _statusBanner.value = "Updated memory."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not update memory."
            }
        }
    }

    fun togglePinMemory(entry: MemoryEntry) {
        viewModelScope.launch {
            repository.updateMemory(
                userId = currentUserId,
                memoryId = entry.id,
                category = MemoryCategory.fromCode(entry.category),
                title = entry.title,
                content = entry.content,
                pinned = !entry.pinned
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not update pin state."
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteMemory(currentUserId, memoryId)
                .onSuccess { _statusBanner.value = "Memory removed." }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to delete memory."
                }
        }
    }

    fun updateLanguage(language: JaniLanguage) {
        val profile = currentProfile()
        viewModelScope.launch {
            repository.updateProfile(
                userId = currentUserId,
                preferredLanguage = language,
                autoSpeak = profile.autoSpeak,
                speechRate = profile.speechRate
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to save language preference."
            }
        }
    }

    fun toggleAutoSpeak() {
        val profile = currentProfile()
        val next = !profile.autoSpeak
        if (!next) voiceManager.stopSpeaking()
        viewModelScope.launch {
            repository.updateProfile(
                userId = currentUserId,
                preferredLanguage = JaniLanguage.fromCode(profile.preferredLanguage),
                autoSpeak = next,
                speechRate = profile.speechRate
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to update voice preference."
            }
        }
    }

    fun updateSpeechRate(rate: Double) {
        val profile = currentProfile()
        viewModelScope.launch {
            repository.updateProfile(
                userId = currentUserId,
                preferredLanguage = JaniLanguage.fromCode(profile.preferredLanguage),
                autoSpeak = profile.autoSpeak,
                speechRate = rate
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to update speech rate."
            }
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            voiceManager.stopSpeaking()
            repository.clearMessages(currentUserId)
                .onSuccess { _statusBanner.value = "Conversation cleared. Permanent memories remain intact." }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to clear conversation."
                }
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
        _chatError.value = null
        voiceManager.clearError()
    }

    private fun unwrapFirestoreError(error: Throwable, fallback: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) {
            throw error
        }
        val rootError = firestoreError ?: error
        Log.w(TAG, fallback, rootError)
        return UiState.Error(rootError.localizedMessage ?: fallback)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
