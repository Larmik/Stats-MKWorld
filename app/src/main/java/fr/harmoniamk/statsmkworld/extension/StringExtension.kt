package fr.harmoniamk.statsmkworld.extension

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