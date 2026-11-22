package com.example.memory

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MemoryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<UserProfileSettings?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfileSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeMemories(userId: String): Flow<List<MemoryEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/memories"
        emitAll(
            db.collection("users").document(uid).collection("memories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MemoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<MemoryEntry> { it.pinned }
                            .thenByDescending { it.updatedAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMessages(userId: String): Flow<List<ChatMessageEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/messages"
        emitAll(
            db.collection("users").document(uid).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessageEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(userId: String): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "preferredLanguage" to JaniLanguage.AUTO.name,
                    "autoSpeak" to true,
                    "speechRate" to 1.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        preferredLanguage: JaniLanguage,
        autoSpeak: Boolean,
        speechRate: Double
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val clampedRate = speechRate.coerceIn(0.5, 2.0)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "preferredLanguage" to preferredLanguage.name,
                        "autoSpeak" to autoSpeak,
                        "speechRate" to clampedRate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to preferredLanguage.name,
                        "autoSpeak" to autoSpeak,
                        "speechRate" to clampedRate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMemory(
        userId: String,
        category: MemoryCategory,
        title: String,
        content: String,
        source: String = "MANUAL",
        pinned: Boolean = false
    ): Result<String> {
        var docPath = "users/$userId/memories"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeTitle = title.trim().take(120)
            val safeContent = content.trim().take(2000)
            require(safeTitle.isNotEmpty()) { "Memory title cannot be empty." }
            require(safeContent.isNotEmpty()) { "Memory content cannot be empty." }
            val safeSource = if (source == "AUTO") "AUTO" else "MANUAL"

            val docRef = db.collection("users").document(uid).collection("memories").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "category" to category.name,
                "title" to safeTitle,
                "content" to safeContent,
                "source" to safeSource,
                "pinned" to pinned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMemory(
        userId: String,
        memoryId: String,
        category: MemoryCategory,
        title: String,
        content: String,
        pinned: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/memories/$memoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memoryId.isNotBlank()) { "Memory ID cannot be blank." }
            val safeTitle = title.trim().take(120)
            val safeContent = content.trim().take(2000)
            require(safeTitle.isNotEmpty()) { "Memory title cannot be empty." }
            require(safeContent.isNotEmpty()) { "Memory content cannot be empty." }

            val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "category" to category.name,
                    "title" to safeTitle,
                    "content" to safeContent,
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(userId: String, memoryId: String): Result<Unit> {
        var docPath = "users/$userId/memories/$memoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memoryId.isNotBlank()) { "Memory ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        userId: String,
        role: String,
        text: String,
        language: JaniLanguage,
        commandAction: String? = null
    ): Result<Unit> {
        var docPath = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeRole = if (role == "assistant") "assistant" else "user"
            val safeText = text.trim().take(8000)
            require(safeText.isNotEmpty()) { "Message cannot be empty." }
            val safeCommand = commandAction?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

            val docRef = db.collection("users").document(uid).collection("messages").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "role" to safeRole,
                "text" to safeText,
                "language" to language.name,
                "commandAction" to safeCommand,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearMessages(userId: String): Result<Unit> {
        val collectionPath = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val colRef = db.collection("users").document(uid).collection("messages")
            val snapshots = colRef.get().await()
            if (!snapshots.isEmpty) {
                val batch = db.batch()
                snapshots.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, collectionPath)
            Result.failure(e)
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
