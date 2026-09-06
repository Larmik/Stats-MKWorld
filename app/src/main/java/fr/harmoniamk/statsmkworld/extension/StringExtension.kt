package fr.harmoniamk.statsmkworld.extension

// Nom de joueur normalisé pour l'affichage. MKCentral concatène les pseudos multiples
// d'un joueur dans un unique champ `name`, séparés par des slashs (ex. "A / B / C").
// On n'affiche que le PREMIER pseudo. Surcharge d'affichage NON destructive : la donnée
// brute reste stockée telle quelle (PlayerEntity.name / User.name). Aucun slash → inchangé.
val String.displayName: String
    get() = when {
        contains("/") -> substringBefore("/").trim()
        else -> this
    }

/**
 * Convertit un code pays ISO 3166-1 alpha-2 (ex. « FR ») en emoji drapeau correspondant,
 * en mappant les 2 lettres sur les Regional Indicator Symbols Unicode. Chaîne trop courte
 * (< 2 caractères) → chaîne vide.
 */
val String.countryFlag: String
    get() {
        if (this.length >= 2) {
            val flagOffset = 0x1F1E6
            val asciiOffset = 0x41
            val firstChar = Character.codePointAt(this, 0) - asciiOffset + flagOffset
            val secondChar = Character.codePointAt(this, 1) - asciiOffset + flagOffset
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
        return ""
    }