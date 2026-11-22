package com.example.brain

import com.example.memory.JaniLanguage
import java.util.Locale

object LanguageEngine {

    private val urduArabicRegex = Regex("[\\u0600-\\u06FF]")
    private val devanagariRegex = Regex("[\\u0900-\\u097F]")

    private val romanUrduHints = setOf(
        "kya", "hai", "hain", "hoon", "main", "mera", "meri", "aap", "tum",
        "yaad", "rakhna", "rakho", "batao", "kaise", "kaisa", "acha", "theek",
        "shukriya", "madad", "kaam", "karna", "karo", "kal", "aaj", "subah",
        "raat", "zindagi", "dost", "salam", "assalam", "walaikum", "aur",
        "nahi", "haan", "mujhe", "hum", "woh", "yeh", "kyun", "kab", "kahan"
    )

    private val romanHindiHints = setOf(
        "namaste", "dhanyawad", "kaise", "bataiye", "kripya", "yaad", "rakhna",
        "samajh", "madad", "karna", "mera", "aapka", "kya", "hain", "nahin",
        "accha", "theek", "subah", "shaam", "dost", "kaam"
    )

    fun detectLanguage(text: String, preferred: JaniLanguage): JaniLanguage {
        if (preferred != JaniLanguage.AUTO) return preferred

        if (urduArabicRegex.containsMatchIn(text)) return JaniLanguage.UR
        if (devanagariRegex.containsMatchIn(text)) return JaniLanguage.HI

        val tokens = text.lowercase(Locale.ROOT)
            .split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }
            .toSet()

        val urduMatches = tokens.count { it in romanUrduHints }
        val hindiMatches = tokens.count { it in romanHindiHints }

        return when {
            urduMatches >= 2 || (urduMatches == 1 && tokens.contains("salam")) -> JaniLanguage.UR
            hindiMatches >= 2 || tokens.contains("namaste") -> JaniLanguage.HI
            else -> JaniLanguage.EN
        }
    }

    fun toTtsLocale(language: JaniLanguage): Locale {
        return when (language) {
            JaniLanguage.UR -> Locale("ur", "PK")
            JaniLanguage.HI -> Locale("hi", "IN")
            JaniLanguage.EN, JaniLanguage.AUTO -> Locale.US
        }
    }

    fun promptDirective(preferred: JaniLanguage): String {
        return when (preferred) {
            JaniLanguage.AUTO ->
                "Automatically detect the user's language (English, Urdu / Roman Urdu, or Hindi / Hinglish) and respond naturally in that exact same language and script."
            JaniLanguage.EN ->
                "Respond in clear, concise, intelligent English."
            JaniLanguage.UR ->
                "Respond naturally in Urdu (match Roman Urdu if the user writes in Roman Urdu, or Urdu script if they write in Urdu script)."
            JaniLanguage.HI ->
                "Respond naturally in Hindi (match Hinglish if the user writes in Latin script, or Devanagari Hindi if they write in Devanagari)."
        }
    }
}
