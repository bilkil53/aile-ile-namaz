package com.example.util

import java.util.Locale

object FamilyRoleHelper {

    private val TR_LOCALE = Locale.forLanguageTag("tr")

    /**
     * Normalizes first-person possessive input ("Oğlum", "Kızım", "Eşim", etc.)
     * to 3rd-person relative title to Aile Reisi ("Oğlu", "Kızı", "Eşi", etc.).
     */
    fun normalizeRoleTitle(roleOrName: String): String {
        val trimmed = roleOrName.trim()
        val lower = trimmed.lowercase(TR_LOCALE)
        return when {
            lower.contains("oğul") || lower.contains("ogul") || lower == "oğlu" || lower == "oğlum" || lower == "oglu" || lower == "oglum" -> "Oğlu"
            lower.contains("kız") || lower.contains("kiz") || lower == "kızı" || lower == "kızım" || lower == "kizi" || lower == "kizim" -> "Kızı"
            lower.contains("eşim") || lower.contains("esim") || lower == "eş" || lower == "es" || lower == "eşi" || lower == "esi" -> "Eşi"
            lower.contains("baba") || lower == "babası" || lower == "babam" || lower == "babasi" -> "Babası"
            lower.contains("anne") || lower == "annesi" || lower == "annem" -> "Annesi"
            lower.contains("kardeş") || lower.contains("kardes") || lower == "kardeşi" || lower == "kardeşim" -> "Kardeşi"
            lower.contains("torun") || lower == "torunu" || lower == "torunum" -> "Torunu"
            lower.contains("dede") || lower == "dedesi" || lower == "dedem" -> "Dede"
            lower.contains("nine") || lower == "ninesi" || lower == "ninem" -> "Nine"
            else -> trimmed
        }
    }

    /**
     * Converts a role or name into 2nd-person possessive address for the Aile Reisi:
     * - "Oğlu" / "Oğlum" -> "Oğlun"
     * - "Kızı" / "Kızım" -> "Kızın"
     * - "Eşi" / "Eşim" -> "Eşin"
     * - "Babası" / "Babam" / "Baba" -> "Baban"
     * - "Annesi" / "Annem" / "Anne" -> "Annen"
     * - "Kardeşi" / "Kardeşim" -> "Kardeşin"
     * - "Torunu" / "Torunum" -> "Torunun"
     * - "Dede" / "Dedesi" -> "Deden"
     * - "Nine" / "Ninesi" -> "Ninen"
     * - Custom name (e.g. "Ahmet") -> "Ahmet"
     */
    fun formatAddressForReis(roleOrName: String): String {
        val trimmed = roleOrName.trim()
        val lower = trimmed.lowercase(TR_LOCALE)
        return when {
            lower.contains("oğul") || lower.contains("ogul") || lower == "oğlu" || lower == "oğlum" || lower == "oglu" || lower == "oglum" -> "Oğlun"
            lower.contains("kız") || lower.contains("kiz") || lower == "kızı" || lower == "kızım" || lower == "kizi" || lower == "kizim" -> "Kızın"
            lower.contains("eş") || lower.contains("es") || lower == "eşi" || lower == "eşim" || lower == "esim" || lower == "esi" -> "Eşin"
            lower.contains("baba") || lower == "babası" || lower == "babam" || lower == "babasi" -> "Baban"
            lower.contains("anne") || lower == "annesi" || lower == "annem" -> "Annen"
            lower.contains("kardeş") || lower.contains("kardes") || lower == "kardeşi" || lower == "kardeşim" -> "Kardeşin"
            lower.contains("torun") || lower == "torunu" || lower == "torunum" -> "Torunun"
            lower.contains("dede") || lower == "dedesi" || lower == "dedem" -> "Deden"
            lower.contains("nine") || lower == "ninesi" || lower == "ninem" -> "Ninen"
            trimmed.isNotBlank() && !trimmed.contains("@") && trimmed != "Mümin" && trimmed != "Aile Ferdi" -> trimmed
            else -> "Aile ferdin"
        }
    }

    /**
     * Formats the pool joined message specifically addressed to the Aile Reisi.
     * e.g. "Oğlun aile havuzuna katıldı 🤝", "Kızın aile havuzuna katıldı 🤝"
     */
    fun formatJoinMessageForReis(roleOrName: String): String {
        val address = formatAddressForReis(roleOrName)
        return "$address aile havuzuna katıldı 🤝"
    }

    /**
     * Formats the pool left message specifically addressed to the Aile Reisi.
     * e.g. "Oğlun aile havuzundan ayrıldı 🔴", "Kızın aile havuzundan ayrıldı 🔴"
     */
    fun formatLeftMessageForReis(roleOrName: String): String {
        val address = formatAddressForReis(roleOrName)
        return "$address aile havuzundan ayrıldı 🔴"
    }

    /**
     * Resolves the appropriate default emoji icon based on family role.
     * e.g. "Oğlu" -> "👦", "Kızı" -> "👧", "Eşi" -> "🧕", "Babası" -> "🧔"
     */
    fun getDefaultIconForRole(roleOrName: String): String {
        val lower = roleOrName.trim().lowercase(TR_LOCALE)
        return when {
            lower.contains("oğul") || lower.contains("ogul") || lower == "oğlu" || lower == "oğlum" || lower == "oglu" || lower == "oglum" -> "👦"
            lower.contains("kız") || lower.contains("kiz") || lower == "kızı" || lower == "kızım" || lower == "kizi" || lower == "kizim" -> "👧"
            lower == "eşi" || lower == "eşim" || lower == "esim" || lower == "esi" || lower == "anne" || lower == "annesi" || lower == "annem" -> "🧕"
            lower == "baba" || lower == "babası" || lower == "babam" || lower == "babasi" -> "🧔"
            lower.contains("kardeş") || lower.contains("kardes") -> "🧒"
            lower.contains("torun") -> "👶"
            lower.contains("dede") -> "👴"
            lower.contains("nine") -> "👵"
            else -> "🧔"
        }
    }

    /**
     * Checks if a cached or provided icon directly conflicts with the gender/nature of the role.
     * E.g. A male role ("Oğlu", "Babası") with a girl/woman icon ("👧", "🧕", etc.) is conflicting.
     */
    fun isIconConflictingWithRole(icon: String, roleOrName: String): Boolean {
        if (icon.isBlank() || roleOrName.isBlank()) return false
        val lower = roleOrName.trim().lowercase(TR_LOCALE)
        val isMale = lower.contains("oğul") || lower.contains("ogul") || lower == "oğlu" || lower == "oğlum" || lower == "oglu" || lower == "oglum" ||
                lower == "baba" || lower == "babası" || lower == "babam" || lower == "babasi" || lower == "dede" || lower == "dedesi" || lower == "dedem"
        val isFemale = lower.contains("kız") || lower.contains("kiz") || lower == "kızı" || lower == "kızım" || lower == "kizi" || lower == "kizim" ||
                lower == "eşi" || lower == "eşim" || lower == "esim" || lower == "esi" || lower == "anne" || lower == "annesi" || lower == "annem" ||
                lower == "nine" || lower == "ninesi" || lower == "ninem"

        val femaleIcons = setOf("👧", "🧕", "👩", "👵", "👱‍♀️")
        val maleIcons = setOf("👦", "🧔", "👨", "🧓", "👱‍♂️")

        if (isMale && femaleIcons.contains(icon)) return true
        if (isFemale && maleIcons.contains(icon)) return true
        return false
    }
}
