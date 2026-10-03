package fr.harmoniamk.statsmkworld.model.network.lorenzi

import com.squareup.moshi.JsonClass

/**
 * Corps JSON de `POST https://gb2.hlorenzi.com/table.png` (#105). Le style n'est appliqué
 * qu'en POST JSON ; `resolutionScale` n'accepte que 1 ou 2 (sinon HTTP 400).
 */
@JsonClass(generateAdapter = true)
data class LorenziTableRequest(
    val data: String,
    val style: LorenziTableStyle,
    val resolutionScale: Int = 2,
)

@JsonClass(generateAdapter = true)
data class LorenziTableFont(
    val name: String,
    val weight: Int,
)

/**
 * Miroir du `TableStyle` du générateur (`tableStyleSchema`, `src/tableRenderer.ts` des source
 * maps de gb2.hlorenzi.com). Les valeurs par défaut sont celles du style « Light ».
 */
@JsonClass(generateAdapter = true)
data class LorenziTableStyle(
    val name: String = "Light",
    val title: String = "",
    val showDate: Boolean = true,
    val useRankingIcons: Boolean = true,
    val showTeamRankings: Boolean = true,
    val baseTextColor: String = "#000000",
    val invertColors: Boolean = false,
    val autoGradient: Double = 0.2,
    val useForcedColors: Boolean = false,
    val forcedColor1: String = "#fd4d44",
    val forcedColor2: String = "#98fb56",
    val forcedGradient1: List<String> = emptyList(),
    val forcedGradient2: List<String> = emptyList(),
    val useRankingColors: Boolean = false,
    val rankingColor1: String = "#ffc800",
    val rankingColor2: String = "#d4d4d4",
    val rankingColor3: String = "#f77f2a",
    val rankingColor4: String = "#ffffff",
    val iconSrc: String = "",
    val iconPlacement: String = "header_left",
    val iconHeight: Int = 40,
    /** `/assets/…`, `https://…` ou `data:image…` ; le serveur (nginx) refuse un corps > ~1 Mo (HTTP 413). */
    val bkgSrc: String = "",
    val bkgColor: String = "#000000",
    val bkgOpacity: Double = 1.0,
    val playerBkgColor: String = "#000000",
    val playerBkgOpacity: Double = 0.15,
    val showBkgPolkaDots: Boolean = true,
    val hideTeamNames: Boolean = false,
    val hideTeamScores: Boolean = false,
    val hidePlayerScores: Boolean = false,
    val emblemTag1: String = "",
    val emblemSrc1: String = "",
    val emblemTag2: String = "",
    val emblemSrc2: String = "",
    val headerFont: LorenziTableFont = LorenziTableFont(name = "Roboto", weight = 900),
    val teamNameFont: LorenziTableFont = LorenziTableFont(name = "Roboto", weight = 900),
    val playerNameFont: LorenziTableFont = LorenziTableFont(name = "Roboto", weight = 900),
    val playerScoreFont: LorenziTableFont = LorenziTableFont(name = "Rubik Mono One", weight = 900),
    val teamScoreFont: LorenziTableFont = LorenziTableFont(name = "Rubik Mono One", weight = 900),
    val rankingFont: LorenziTableFont = LorenziTableFont(name = "Roboto", weight = 900),
) {
    /**
     * Texte d'une seule couleur. En style inversé (Dark), noms/scores/tags prennent la couleur
     * d'équipe et non `baseTextColor` (qui ne colore que l'écart et les rangs) : on force donc
     * la même couleur pour les deux équipes, sans dégradé (vérifié en live, #105).
     */
    fun withTextColor(hex: String) = copy(
        baseTextColor = hex,
        useForcedColors = true,
        forcedColor1 = hex,
        forcedColor2 = hex,
        forcedGradient1 = emptyList(),
        forcedGradient2 = emptyList(),
        autoGradient = 0.0,
    )
}

/**
 * Styles prédéfinis du générateur retenus pour l'app (objet `tableStyles` de `src/tableRenderer.ts`),
 * recopiés tels quels : l'API ne les connaît pas par nom, l'objet complet est envoyé.
 * Atlas League en premier (style proposé par défaut), base « Dark (Thin) » du générateur.
 *
 * @property circuitBackground fond = circuit au meilleur score ; jamais pour Atlas League.
 * @property textColorChoice palette de couleur de texte proposée ([LorenziTextColor]).
 */
enum class LorenziStylePreset(
    val style: LorenziTableStyle,
    val circuitBackground: Boolean,
    val textColorChoice: Boolean,
) {
    ATLAS_LEAGUE(
        LorenziTableStyle(
            name = "Atlas League",
            title = "Atlas League",
            iconSrc = "/assets/atlasleague.png",
            showTeamRankings = false,
            useRankingIcons = false,
            baseTextColor = "#ffffff",
            invertColors = true,
            useForcedColors = true,
            forcedColor1 = "#5eb6ea",
            forcedColor2 = "#ffffff",
            forcedGradient1 = listOf("#68c4eb", "#4d72e7"),
            forcedGradient2 = listOf("#b0dfe3", "#ffffff"),
            autoGradient = 0.0,
            headerFont = LorenziTableFont(name = "Roboto", weight = 400),
            teamNameFont = LorenziTableFont(name = "Roboto", weight = 300),
            playerNameFont = LorenziTableFont(name = "Roboto", weight = 400),
            playerScoreFont = LorenziTableFont(name = "Roboto", weight = 400),
            teamScoreFont = LorenziTableFont(name = "Roboto", weight = 300),
            rankingFont = LorenziTableFont(name = "Roboto", weight = 400),
        ),
        circuitBackground = false,
        textColorChoice = false,
    ),
    LIGHT(LorenziTableStyle(), circuitBackground = true, textColorChoice = false),
    DARK(
        LorenziTableStyle(
            name = "Dark",
            baseTextColor = "#ffffff",
            invertColors = true,
            playerBkgColor = "#888888",
            playerBkgOpacity = 0.05,
        ),
        circuitBackground = true,
        textColorChoice = true,
    ),
}

/** Palette de couleur de texte du style Dark ; [AUTO] = couleurs d'équipe du générateur (rendu Dark d'origine). */
enum class LorenziTextColor(val hex: String?) {
    AUTO(null),
    WHITE("#ffffff"),
    YELLOW("#ffd400"),
    CYAN("#4dd8f0"),
    GREEN("#7ee36b"),
    PINK("#ff7ac8"),
    ORANGE("#ff9a3c"),
}
