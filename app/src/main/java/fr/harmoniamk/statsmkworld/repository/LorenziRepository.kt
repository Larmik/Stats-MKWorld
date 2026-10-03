package fr.harmoniamk.statsmkworld.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface LorenziRepositoryInterface {
    /**
     * Tab PNG d'une war 12p rendu par gb2.hlorenzi.com (#105). Les pseudos et tags sont envoyés
     * au service tiers. [textColor] n'est appliqué que si le style propose la palette.
     * `null` si le service est injoignable, expire ou répond en erreur.
     */
    suspend fun generateTab(
        details: WarDetails,
        hostTeam: TeamEntity,
        opponentTeam: TeamEntity,
        hostScores: List<PlayerScoreForTab>,
        opponentScores: List<PlayerScoreForTab>,
        preset: LorenziStylePreset,
        textColor: LorenziTextColor
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
        preset: LorenziStylePreset,
        textColor: LorenziTextColor
    ): ByteArray? {
        // Format `#date` attendu par le parseur JS (`new Date(...)`) : chiffres ASCII imposés.
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(details.war.id))
        // Totaux uniquement : les adversaires n'ont pas de score par course, des colonnes `|`
        // côté hôte seulement rendraient des lignes incohérentes.
        val data = (listOf("#date $date") +
                teamBlock(details, hostTeam, hostScores) +
                teamBlock(details, opponentTeam, opponentScores)).joinToString("\n")
        // Pas de fond pour Atlas League : ni décodage ni envoi (corps plus léger, réponse plus rapide).
        val background = when (preset.circuitBackground) {
            true -> withContext(Dispatchers.Default) {
                // JPEG ~130 Ko (≈ 170 Ko en base64) : le serveur rejette en 413 un corps > ~1 Mo et
                // la latence croît avec la taille (PNG brut 500 Ko → ~12 s contre ~6 s).
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, details.tabBackground, options)?.let { bitmap ->
                    val jpeg = ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        stream.toByteArray()
                    }
                    bitmap.recycle()
                    "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
                }.orEmpty()
            }
            else -> ""
        }
        val style = textColor.hex
            ?.takeIf { preset.textColorChoice }
            ?.let { preset.style.withTextColor(it) }
            ?: preset.style
        val request = LorenziTableRequest(data = data, style = style.copy(bkgSrc = background))
        return RetrofitUtils.createRetrofit(LorenziApi::class.java, LorenziApi.baseUrl, timeout = 30)
            .getTable(request)
            .successResponse
            ?.use { it.bytes() }
    }

    /** Ligne d'équipe `TAG - Nom (-pénalité)` puis une ligne `Pseudo total` par joueur. */
    private fun teamBlock(details: WarDetails, team: TeamEntity, scores: List<PlayerScoreForTab>): List<String> {
        val penalty = details.war.penalties.filter { it.teamId == team.id }.sumOf { it.amount }
        val teamLine = listOfNotNull(
            lorenziSafe(team.tag),
            team.name.takeIf { it.isNotBlank() }?.let { "- ${lorenziSafe(it)}" },
            "(-$penalty)".takeIf { penalty > 0 }
        ).joinToString(" ")
        return listOf(teamLine) + scores.map { "${lorenziSafe(it.displayedName)} ${it.score}" }
    }

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
