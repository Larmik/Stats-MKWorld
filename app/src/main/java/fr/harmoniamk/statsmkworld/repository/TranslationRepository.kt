package fr.harmoniamk.statsmkworld.repository

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.model.local.MarkdownSyntax
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Traduction sur l'appareil (ML Kit, sans clé ni backend) des textes Markdown anglais de MKCentral
 * (#152) vers la langue du téléphone.
 */
interface TranslationRepositoryInterface {
    /** Code ML Kit de la langue du téléphone ; `null` si anglais ou non supportée (texte original). */
    val targetLanguage: String?

    /**
     * Traduit [texts] (même ordre) en préservant la syntaxe Markdown et les URL ; `null` si le modèle
     * n'est pas disponible (téléchargement en Wi-Fi uniquement). Un segment en échec reste en anglais.
     */
    suspend fun translateMarkdown(texts: List<String>, targetLanguage: String): List<String>?
}

@Module
@InstallIn(SingletonComponent::class)
interface TranslationRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: TranslationRepository): TranslationRepositoryInterface
}

class TranslationRepository @Inject constructor() : TranslationRepositoryInterface {

    override val targetLanguage: String?
        get() = TranslateLanguage.fromLanguageTag(Locale.getDefault().language)
            ?.takeIf { it != TranslateLanguage.ENGLISH }

    override suspend fun translateMarkdown(texts: List<String>, targetLanguage: String): List<String>? {
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguage)
                .build()
        )
        return try {
            // ~30 Mo par langue, Wi-Fi requis. Borné : si les conditions ne sont pas réunies, la tâche
            // peut rester en attente ; le téléchargement se poursuit et servira à la synchro suivante.
            val downloaded = withTimeoutOrNull(60_000) {
                translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build()).awaitResult()
            }
            when (downloaded?.isSuccess) {
                true -> texts.map { text -> text.lines().map { translateLine(it, translator) }.joinToString("\n") }
                else -> null
            }
        } finally {
            translator.close()
        }
    }

    /** Ligne traduite segment par segment ; préfixes de bloc, emphases, liens, URL et images intacts. */
    private suspend fun translateLine(line: String, translator: Translator): String {
        val trimmed = line.trim()
        val content = MarkdownSyntax.heading.find(trimmed)?.groupValues?.get(2)
            ?: MarkdownSyntax.bullet.find(trimmed)?.groupValues?.get(1)
            ?: MarkdownSyntax.ordered.find(trimmed)?.groupValues?.get(2)
            ?: trimmed
        return when {
            trimmed.isEmpty() || MarkdownSyntax.rule.matches(trimmed) || MarkdownSyntax.image.matches(trimmed) -> trimmed
            else -> {
                val translated = StringBuilder(trimmed.removeSuffix(content))
                var cursor = 0
                protectedRegex.findAll(content).forEach { match ->
                    translated.append(translateSegment(content.substring(cursor, match.range.first), translator))
                    val label = match.groupValues[1]
                    when {
                        // Lien : seul le libellé est traduit, l'URL est restituée telle quelle.
                        label.isNotEmpty() -> translated.append("[${translateSegment(label, translator)}](${match.groupValues[2]})")
                        else -> translated.append(match.value)
                    }
                    cursor = match.range.last + 1
                }
                translated.append(translateSegment(content.substring(cursor), translator)).toString()
            }
        }
    }

    /** Segment sans lettre (ponctuation, espaces) ou en échec → inchangé ; espaces de bord conservés. */
    private suspend fun translateSegment(segment: String, translator: Translator): String {
        val text = segment.trim()
        return when {
            text.none { it.isLetter() } -> segment
            else -> segment.takeWhile { it.isWhitespace() } +
                (translator.translate(text).awaitResult().getOrNull() ?: text) +
                segment.takeLastWhile { it.isWhitespace() }
        }
    }

    /** Pont `Task` → coroutine (`kotlinx-coroutines-play-services` non déclaré). */
    private suspend fun <T> Task<T>.awaitResult(): Result<T> = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(Result.success(it)) }
        addOnFailureListener { continuation.resume(Result.failure(it)) }
    }

    private companion object {
        // Lien (libellé traduit à part), URL nue, marqueur d'emphase : jamais envoyés au traducteur.
        val protectedRegex = Regex("${MarkdownSyntax.LINK}|${MarkdownSyntax.BARE_URL}|\\*\\*|\\*")
    }
}
