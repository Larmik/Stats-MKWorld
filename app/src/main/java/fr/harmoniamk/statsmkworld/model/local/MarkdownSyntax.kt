package fr.harmoniamk.statsmkworld.model.local

/**
 * Sous-ensemble Markdown des textes officiels MKCentral (#152), partagé par le rendu
 * (`MKMarkdownText`) et la traduction (`TranslationRepository`) pour qu'ils reconnaissent les mêmes
 * blocs. Les regex de bloc s'appliquent à une ligne déjà `trim`.
 */
object MarkdownSyntax {
    val heading = Regex("""^(#{1,6})\s+(.*)$""")
    val bullet = Regex("""^[-*+]\s+(.*)$""")
    val ordered = Regex("""^(\d+)\.\s+(.*)$""")
    val rule = Regex("""^(\*{3,}|-{3,}|_{3,})$""")
    val image = Regex("""^!\[[^\]]*]\((\S+)\)$""")

    /** Lien `[libellé](url)` : groupes 1 = libellé, 2 = url. */
    const val LINK = """\[([^\]]+)]\((\S+?)\)"""
    const val BARE_URL = """https?://[^\s)]+"""
}
