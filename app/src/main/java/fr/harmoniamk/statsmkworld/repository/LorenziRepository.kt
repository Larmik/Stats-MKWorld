package fr.harmoniamk.statsmkworld.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.graphics.toColorInt
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.api.LorenziApi
import fr.harmoniamk.statsmkworld.api.RetrofitUtils
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziStylePreset
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziTableRequest
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziTableStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

interface LorenziRepositoryInterface {
    /**
     * Tab PNG d'une war 12p rendu par gb2.hlorenzi.com (#105). Les pseudos et tags sont envoyés
     * au service tiers. `null` si le service est injoignable, expire ou répond en erreur.
     */
    suspend fun generateTab(
        details: WarDetails,
        hostTeam: TeamEntity,
        opponentTeam: TeamEntity,
        hostScores: List<PlayerScoreForTab>,
        opponentScores: List<PlayerScoreForTab>,
        preset: LorenziStylePreset
    ): ByteArray?
}

@Module
@InstallIn(SingletonComponent::class)
interface LorenziRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: LorenziRepository): LorenziRepositoryInterface
}

class LorenziRepository @Inject constructor(@ApplicationContext private val context: Context) : LorenziRepositoryInterface {

    override suspend fun generateTab(
        details: WarDetails,
        hostTeam: TeamEntity,
        opponentTeam: TeamEntity,
        hostScores: List<PlayerScoreForTab>,
        opponentScores: List<PlayerScoreForTab>,
        preset: LorenziStylePreset
    ): ByteArray? {
        // Format `#date` attendu par le parseur JS (`new Date(...)`) : chiffres ASCII imposés.
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(details.war.id))
        // Totaux uniquement : les adversaires n'ont pas de score par course, des colonnes `|`
        // côté hôte seulement rendraient des lignes incohérentes.
        val data = (listOf("#date $date") +
                teamBlock(details, hostTeam, hostScores) +
                teamBlock(details, opponentTeam, opponentScores)).joinToString("\n")
        // Pas de fond pour Atlas League : ni décodage ni envoi (corps plus léger, réponse plus rapide).
        val style = when (preset.circuitBackground) {
            true -> withContext(Dispatchers.Default) {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, details.tabBackground, options)?.let { bitmap ->
                    val penaltyRows = listOf(hostTeam, opponentTeam).count { penaltyOf(details, it) > 0 }
                    val readableStyle = readableStyle(bitmap, preset, hostScores.size + opponentScores.size + penaltyRows)
                    // JPEG ~130 Ko (≈ 170 Ko en base64) : le serveur rejette en 413 un corps > ~1 Mo et
                    // la latence croît avec la taille (PNG brut 500 Ko → ~12 s contre ~6 s).
                    val jpeg = ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        stream.toByteArray()
                    }
                    bitmap.recycle()
                    readableStyle.copy(bkgSrc = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP))
                }
            } ?: preset.style
            else -> preset.style
        }
        val request = LorenziTableRequest(data = data, style = style)
        return RetrofitUtils.createRetrofit(LorenziApi::class.java, LorenziApi.baseUrl, timeout = 30)
            .getTable(request)
            .successResponse
            ?.use { it.bytes() }
    }

    /** Ligne d'équipe `TAG - Nom (-pénalité)` puis une ligne `Pseudo total` par joueur. */
    private fun teamBlock(details: WarDetails, team: TeamEntity, scores: List<PlayerScoreForTab>): List<String> {
        val penalty = penaltyOf(details, team)
        val teamLine = listOfNotNull(
            lorenziSafe(team.tag),
            team.name.takeIf { it.isNotBlank() }?.let { "- ${lorenziSafe(it)}" },
            "(-$penalty)".takeIf { penalty > 0 }
        ).joinToString(" ")
        return listOf(teamLine) + scores.map { "${lorenziSafe(it.displayedName)} ${it.score}" }
    }

    private fun penaltyOf(details: WarDetails, team: TeamEntity): Int =
        details.war.penalties.filter { it.teamId == team.id }.sumOf { it.amount }

    /**
     * Style lisible sur [background] : texte (noir/blanc si `autoTextColor`, sinon celui du style) et
     * voile (`bkgColor` + `bkgOpacity`) garantissant un contraste WCAG ≥ 4,5 sur 90 % du fond visible.
     * Reproduit `tableRenderer.ts` : image en « cover » sur un canevas de 818 × max(528, 32 + 38 × lignes
     * + 20 × équipes), en-tête noir de 40 px, image mélangée à `bkgColor` selon `bkgOpacity` (mélange
     * sRVB du canvas), bandeaux joueurs `playerBkgColor` pris en pire cas (avec ou sans). Les pois (#333,
     * opacité ≤ 25 %) sont ignorés. Retient la couleur qui demande le voile le plus léger.
     */
    private fun readableStyle(background: Bitmap, preset: LorenziStylePreset, rowCount: Int): LorenziTableStyle {
        val canvasWidth = 818f
        val canvasHeight = max(528f, 32f + 38f * rowCount + 20f * 2) // 2 équipes (12p)
        val scale = max(canvasWidth / background.width, canvasHeight / background.height)
        val cropLeft = ((background.width * scale - canvasWidth) / 2 / scale).toInt()
        val cropTop = ((background.height * scale - canvasHeight) / 2 / scale + 40 / scale).toInt()
        val cropWidth = (canvasWidth / scale).toInt().coerceAtMost(background.width - cropLeft)
        val cropHeight = ((canvasHeight - 40) / scale).toInt().coerceAtMost(background.height - cropTop)
        val visible = Bitmap.createBitmap(background, cropLeft, cropTop, cropWidth, cropHeight)
        // ~2 000 échantillons suffisent à l'échelle d'un glyphe et gardent le calcul instantané.
        val sample = Bitmap.createScaledBitmap(visible, 64, (64 * cropHeight / cropWidth).coerceAtLeast(1), true)
        val pixels = IntArray(sample.width * sample.height)
            .also { sample.getPixels(it, 0, sample.width, 0, 0, sample.width, sample.height) }
            .map { srgbChannels(it) }
        if (sample !== visible) sample.recycle()
        if (visible !== background) visible.recycle()

        val stripChannels = srgbChannels(preset.style.playerBkgColor.toColorInt())
        val stripOpacity = preset.style.playerBkgOpacity
        val textCandidates = when (preset.autoTextColor) {
            true -> listOf("#000000", "#ffffff")
            else -> listOf(preset.style.baseTextColor)
        }
        val (textColor, veilColor, opacity) = textCandidates.map { candidate ->
            val textLuminance = relativeLuminance(srgbChannels(candidate.toColorInt()))
            // Texte clair → voile noir ; texte sombre → voile blanc.
            val veil = if (textLuminance > 0.5) "#000000" else "#ffffff"
            val veilChannel = if (textLuminance > 0.5) 0.0 else 1.0
            val requiredOpacity = generateSequence(1.0) { it - 0.05 }
                .takeWhile { it >= 0.35 } // plancher : en deçà, le circuit ne se voit plus
                .firstOrNull { candidateOpacity ->
                    val contrasts = pixels.map { pixel ->
                        val blended = pixel.map { channel -> candidateOpacity * channel + (1 - candidateOpacity) * veilChannel }
                        val withStrip = blended.zip(stripChannels) { channel, stripChannel ->
                            (1 - stripOpacity) * channel + stripOpacity * stripChannel
                        }
                        minOf(
                            contrastRatio(textLuminance, relativeLuminance(blended)),
                            contrastRatio(textLuminance, relativeLuminance(withStrip))
                        )
                    }.sorted()
                    contrasts[contrasts.size / 10] >= 4.5
                } ?: 0.35
            Triple(candidate, veil, requiredOpacity)
        }.maxBy { it.third }
        return preset.style.withTextColor(textColor).copy(bkgColor = veilColor, bkgOpacity = opacity)
    }

    /** Canaux rouge, vert, bleu (0..1) d'une couleur ARGB. */
    private fun srgbChannels(color: Int): List<Double> =
        listOf(16, 8, 0).map { shift -> ((color shr shift) and 0xFF) / 255.0 }

    /** Luminance relative WCAG d'un triplet sRVB (0..1). */
    private fun relativeLuminance(rgb: List<Double>): Double {
        val (red, green, blue) = rgb.map { channel ->
            if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun contrastRatio(firstLuminance: Double, secondLuminance: Double): Double =
        (max(firstLuminance, secondLuminance) + 0.05) / (minOf(firstLuminance, secondLuminance) + 0.05)

    /**
     * Neutralise un texte libre (pseudo, tag, nom) que le parseur HLorenzi interpréterait :
     * `#` initial = directive (ligne ignorée), et en fin de texte `(n)` = bonus/pénalité,
     * `[xx]` = drapeau, ` 12` = score (ligne d'équipe lue comme joueur), ` #RRGGBB` = couleur.
     * Une espace sans chasse (U+200B, invisible au rendu, non retirée par `trim()` JS) casse
     * ces motifs ; les textes sans risque restent intacts (la couleur auto dépend du tag).
     */
    private fun lorenziSafe(text: String): String {
        val singleLine = text.replace(Regex("\\s+"), " ").trim()
        val prefix = if (singleLine.startsWith("#")) ZERO_WIDTH_SPACE else ""
        val suffix = if (lorenziTrailingSyntax.containsMatchIn(singleLine)) ZERO_WIDTH_SPACE else ""
        return prefix + singleLine + suffix
    }

    private companion object {
        const val ZERO_WIDTH_SPACE = "\u200B"
        val lorenziTrailingSyntax = Regex("""(?:[)）\]］」】]|\s[0-9０-９+|｜＋－ー-]+|\s#[0-9A-Fa-f]{6})$""")
    }
}
