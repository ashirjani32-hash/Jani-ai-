package com.example.memory

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class JaniLanguage(
    val code: String,
    val label: String,
    val speechLocaleTag: String
) {
    AUTO("AUTO", "Auto", "en-US"),
    EN("EN", "English", "en-US"),
    UR("UR", "اردو / Urdu", "ur-PK"),
    HI("HI", "हिन्दी / Hindi", "hi-IN");

    companion object {
        fun fromCode(raw: String?): JaniLanguage =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class MemoryCategory(val label: String) {
    FACT("Fact"),
    PREFERENCE("Preference"),
    GOAL("Goal"),
    TASK("Task"),
    NOTE("Note");

    companion object {
        fun fromCode(raw: String?): MemoryCategory =
            entries.firstOrNull { it.name == raw } ?: FACT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfileSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val preferredLanguage: String = JaniLanguage.AUTO.name,
    val autoSpeak: Boolean = true,
    val speechRate: Double = 1.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MemoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = MemoryCategory.FACT.name,
    val title: String = "",
    val content: String = "",
    val source: String = "MANUAL",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessageEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val language: String = JaniLanguage.AUTO.name,
    val commandAction: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
