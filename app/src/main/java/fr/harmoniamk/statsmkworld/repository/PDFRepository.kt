package fr.harmoniamk.statsmkworld.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface PDFRepositoryInterface {
    fun write(pdfDocument: PdfDocument, fileName: String): Flow<Uri?>
    fun generatePdf(details: WarDetails, teamWin: TeamEntity?, teamLose: TeamEntity?, hostScores: List<PlayerScoreForTab>, opponentScores: List<PlayerScoreForTab>): PdfDocument
}


@Module
@InstallIn(SingletonComponent::class)
interface PDFRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: PDFRepository): PDFRepositoryInterface
}

class PDFRepository @Inject constructor(@ApplicationContext private val context: Context) : PDFRepositoryInterface {

    override fun write(pdfDocument: PdfDocument, fileName: String) = flow {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Dossier "Pictures/"
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                        pdfToJpg(pdfDocument, outputStream)
                        emit(uri)
                    }
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val jpegFile = File(picturesDir, fileName)
                FileOutputStream(jpegFile).use { out -> pdfToJpg(pdfDocument, out) }
                emit(FileProvider.getUriForFile(context, "${context.packageName}.provider", jpegFile))
            }
        } catch (_: IOException) {
            emit(value = null)
        } finally {
            pdfDocument.close()
        }
    }.flowOn(context = Dispatchers.IO)


    override fun generatePdf(details: WarDetails, teamWin: TeamEntity?, teamLose: TeamEntity?, hostScores: List<PlayerScoreForTab>, opponentScores: List<PlayerScoreForTab>): PdfDocument {
        val pdfView: View = View.inflate(context, R.layout.tab_pdf, null)
        val allScores = (hostScores.map { Pair(it, details.war.teamHost) } + opponentScores.map { Pair(it, details.war.teamOpponent) }).sortedByDescending { it.first.score }
        val playersWin: MutableList<Pair<PlayerScoreForTab, Int>> = mutableListOf()
        val playersLose: MutableList<Pair<PlayerScoreForTab, Int>> = mutableListOf()
        val doc = PdfDocument()
        var winHasPena = false
        var loseHasPena = false
        var height = when  {
            allScores.size == 13 -> 1040
            allScores.size == 14 -> 1110
            allScores.size == 15 -> 1180
            allScores.size == 16 -> 1250
            allScores.size == 17 -> 1320
            allScores.size == 18 -> 1390
            else -> 970
        }

        details.war.penalties.filter { it.teamId == teamWin?.id }.sumOf { it.amount }.takeIf { it > 0 }?.let {
            winHasPena = true
            pdfView.findViewById<TextView>(R.id.tab_winner_team_penalty_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_winner_team_penalty_score).text =  "-$it"
        }
        details.war.penalties.filter { it.teamId == teamLose?.id }.sumOf { it.amount }.takeIf { it > 0 }?.let {
            loseHasPena = true
            pdfView.findViewById<TextView>(R.id.tab_loser_team_penalty_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_loser_team_penalty_score).text =  "-$it"
        }
        when {
            winHasPena && loseHasPena -> height += 140
            winHasPena || loseHasPena -> height += 70
        }
        val pageInfo = PdfDocument.PageInfo.Builder(1630, height, 1).create()
        val page = doc.startPage(pageInfo)
        val pageCanvas: Canvas = page.canvas
        pageCanvas.scale(1f, 1f)
        val pageWidth: Int = pageCanvas.width
        val pageHeight: Int = pageCanvas.height
        val measureWidth: Int = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight: Int = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        allScores.forEachIndexed { index, pair ->
            val rank = when (pair.first.score == allScores.getOrNull(index-1)?.first?.score) {
                true -> index
                else -> index + 1
            }
            when {
                pair.second == teamWin?.id -> playersWin.add(Pair(pair.first, rank))
                pair.second == teamLose?.id -> playersLose.add(Pair(pair.first, rank))
            }
        }
        pdfView.findViewById<TextView>(R.id.tab_war_date).text = details.date
        pdfView.findViewById<TextView>(R.id.tab_war_diff).text = details.displayedDiff
        pdfView.findViewById<TextView>(R.id.tab_winner_team_tag).text = teamWin?.tag
        pdfView.findViewById<TextView>(R.id.tab_winner_team_name).text = teamWin?.name
        pdfView.findViewById<TextView>(R.id.tab_loser_team_tag).text = teamLose?.tag
        pdfView.findViewById<TextView>(R.id.tab_loser_team_name).text = teamLose?.name
        if (teamWin?.id == details.war.teamHost)
            pdfView.findViewById<LinearLayout>(R.id.tab_winner_team_layout).setBackgroundColor(context.getColor(R.color.blue_hr))
        if (teamLose?.id == details.war.teamHost)
            pdfView.findViewById<LinearLayout>(R.id.tab_loser_team_layout).setBackgroundColor(context.getColor(R.color.blue_hr))
        pdfView.findViewById<TextView>(R.id.tab_winner_team_score).text = when {
            teamWin?.id == details.war.teamHost -> details.scoreHostWithPenalties.toString()
            teamWin?.id == details.war.teamOpponent -> details.scoreOpponentWithPenalties.toString()
            else -> ""
        }
        pdfView.findViewById<TextView>(R.id.tab_loser_team_score).text  = when {
            teamLose?.id == details.war.teamHost -> details.scoreHostWithPenalties.toString()
            teamLose?.id == details.war.teamOpponent -> details.scoreOpponentWithPenalties.toString()
            else -> ""
        }

        pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_name).text = playersWin[0].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_score).text = playersWin[0].first.score.toString()
        when (playersWin[0].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_first_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_winner_team_first_player_rank).text = playersWin[0].second.toString() + "th"
            }
        }
        when (playersWin[1].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_second_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_rank).text = playersWin[1].second.toString() + "th"
            }
        }
        when (playersWin[2].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_winner_team_third_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_rank).text = playersWin[2].second.toString() + "th"
            }
        }
        pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_name).text =  playersWin[1].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_second_player_score).text = playersWin[1].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_name).text =  playersWin[2].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_third_player_score).text = playersWin[2].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fourth_player_name).text =  playersWin[3].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fourth_player_score).text = playersWin[3].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fourth_player_rank).text = playersWin[3].second.toString() + "th"
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fifth_player_name).text =  playersWin[4].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fifth_player_score).text = playersWin[4].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_winner_team_fifth_player_rank).text = playersWin[4].second.toString() + "th"
        pdfView.findViewById<TextView>(R.id.tab_winner_team_sixth_player_name).text =  playersWin[5].first.player
        pdfView.findViewById<TextView>(R.id.tab_winner_team_sixth_player_score).text = playersWin[5].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_winner_team_sixth_player_rank).text = playersWin[5].second.toString() + "th"
        playersWin.getOrNull(6)?.let {
            pdfView.findViewById<TextView>(R.id.tab_winner_team_seventh_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_winner_team_seventh_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_winner_team_seventh_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_winner_team_seventh_player_rank).text = it.second.toString() + "th"
        }
        playersWin.getOrNull(7)?.let {
            pdfView.findViewById<TextView>(R.id.tab_winner_team_eighth_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_winner_team_eighth_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_winner_team_eighth_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_winner_team_eighth_player_rank).text = it.second.toString() + "th"
        }
        playersWin.getOrNull(8)?.let {
            pdfView.findViewById<TextView>(R.id.tab_winner_team_ninth_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_winner_team_ninth_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_winner_team_ninth_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_winner_team_ninth_player_rank).text = it.second.toString() + "th"
        }
        when (playersLose[0].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_first_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_rank).text = playersLose[0].second.toString() + "th"
            }
        }
        when (playersLose[1].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_second_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_rank).text = playersLose[1].second.toString() + "th"
            }
        }
        when (playersLose[2].second) {
            1 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).setImageResource(R.drawable.couronne)
            }
            2 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).setImageResource(R.drawable.silver)
            }
            3 -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_rank).isVisible = false
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).setImageResource(R.drawable.bronze)
            }
            else -> {
                pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_rank).isVisible = true
                pdfView.findViewById<ImageView>(R.id.tab_loser_team_third_player_asset).isVisible = false
                pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_rank).text = playersLose[2].second.toString() + "th"
            }
        }
        pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_name).text = playersLose[0].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_first_player_score).text = playersLose[0].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_name).text =  playersLose[1].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_second_player_score).text = playersLose[1].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_name).text =  playersLose[2].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_third_player_score).text = playersLose[2].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fourth_player_name).text =  playersLose[3].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fourth_player_score).text = playersLose[3].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fourth_player_rank).text = playersLose[3].second.toString() + "th"
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fifth_player_name).text =  playersLose[4].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fifth_player_score).text = playersLose[4].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_fifth_player_rank).text = playersLose[4].second.toString() + "th"
        pdfView.findViewById<TextView>(R.id.tab_loser_team_sixth_player_name).text =  playersLose[5].first.player
        pdfView.findViewById<TextView>(R.id.tab_loser_team_sixth_player_score).text = playersLose[5].first.score.toString()
        pdfView.findViewById<TextView>(R.id.tab_loser_team_sixth_player_rank).text = playersLose[5].second.toString() + "th"
        playersLose.getOrNull(6)?.let {
            pdfView.findViewById<TextView>(R.id.tab_loser_team_seventh_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_loser_team_seventh_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_loser_team_seventh_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_loser_team_seventh_player_rank).text = it.second.toString() + "th"
        }
        playersLose.getOrNull(7)?.let {
            pdfView.findViewById<TextView>(R.id.tab_loser_team_eighth_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_loser_team_eighth_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_loser_team_eighth_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_loser_team_eighth_player_rank).text = it.second.toString() + "th"
        }
        playersLose.getOrNull(8)?.let {
            pdfView.findViewById<TextView>(R.id.tab_loser_team_ninth_player_layout).isVisible = true
            pdfView.findViewById<TextView>(R.id.tab_loser_team_ninth_player_name).text =  it.first.player
            pdfView.findViewById<TextView>(R.id.tab_loser_team_ninth_player_score).text = it.first.score.toString()
            pdfView.findViewById<TextView>(R.id.tab_loser_team_ninth_player_rank).text = it.second.toString() + "th"
        }
        pdfView.measure(measureWidth, measuredHeight)
        pdfView.layout(0, 0, pageWidth, pageHeight)
        pdfView.draw(pageCanvas)
        doc.finishPage(page)
        return doc
    }

    private fun pdfToJpg(pdfDocument: PdfDocument, outputStream: OutputStream) {
        val tempFile = File(context.cacheDir, "temp_doc.pdf")
        FileOutputStream(tempFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        val page = pdfRenderer.openPage(0)
        val bitmap = createBitmap(page.width, page.height)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        page.close()
        pdfRenderer.close()
        fileDescriptor.close()
        tempFile.delete()
    }

}