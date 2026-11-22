package com.example.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

enum class JaniCommandType(val label: String) {
    WEB_SEARCH("Web Search"),
    SET_TIMER("Set Timer"),
    DIAL_NUMBER("Phone Dialer"),
    COMPOSE_EMAIL("Compose Email"),
    OPEN_SETTINGS("Device Settings");

    companion object {
        fun fromCode(raw: String?): JaniCommandType? =
            entries.firstOrNull { it.name == raw }
    }
}

data class CommandSuggestion(
    val type: JaniCommandType,
    val argument: String
)

data class CommandExecutionResult(
    val executed: Boolean,
    val message: String
)

object CommandRouter {

    fun execute(context: Context, command: CommandSuggestion): CommandExecutionResult {
        val intent = when (command.type) {
            JaniCommandType.WEB_SEARCH -> {
                val query = Uri.encode(command.argument.ifBlank { "Jani AI" })
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
            }
            JaniCommandType.SET_TIMER -> {
                val seconds = command.argument.toIntOrNull()?.coerceIn(5, 86_400) ?: 60
                Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Jani AI Timer")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
            }
            JaniCommandType.DIAL_NUMBER -> {
                val digits = command.argument.filter { it.isDigit() || it == '+' }
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            }
            JaniCommandType.COMPOSE_EMAIL -> {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_SUBJECT, command.argument.ifBlank { "Note from Jani AI" })
                }
            }
            JaniCommandType.OPEN_SETTINGS -> {
                Intent(Settings.ACTION_SETTINGS)
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                CommandExecutionResult(
                    executed = true,
                    message = "Opened ${command.type.label}."
                )
            } else {
                CommandExecutionResult(
                    executed = false,
                    message = "No compatible app is installed on this device for ${command.type.label}."
                )
            }
        } catch (e: Exception) {
            CommandExecutionResult(
                executed = false,
                message = "Could not launch ${command.type.label}: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
