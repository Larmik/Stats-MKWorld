package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.model.network.RecordDto
import fr.harmoniamk.statsmkworld.model.network.SplitsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


interface WorldRecordsRepositoryInterface {
    suspend fun getCurrentWRs(): List<RecordDto>
}

@Module
@InstallIn(SingletonComponent::class)
interface WorldRecordsRepositoryModule {
    @Binds
    @Singleton
    fun bind(impl: WorldRecordsRepository): WorldRecordsRepositoryInterface
}


class WorldRecordsRepository @Inject constructor(): WorldRecordsRepositoryInterface {

    private val baseUrl = "https://mkwrs.com/mkworld/"
    private val ua = "Mozilla/5.0 (Android) MKWorldFetcher/1.0"

    // cache mapping trackUrl -> HeaderInfo (détecté une fois)
    private val headerCache = ConcurrentHashMap<String, HeaderInfo>()

    private data class HeaderInfo(
        val dateIdx: Int,
        val timeIdx: Int,
        val playerIdx: Int,
        val nationIdx: Int,
        val durationIdx: Int,
        val lapStartIdx: Int,    // inclusive
        val coinsIdx: Int,
        val shroomsIdx: Int
    )

    override suspend fun getCurrentWRs(): List<RecordDto> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(baseUrl).userAgent(ua).get()
        val rows = doc.select("tr:has(a[href*='display.php?track='])")
        val records = mutableListOf<RecordDto>()

        for (row in rows) {
            val tds = row.select("td")
            if (tds.size < 8) continue

            val trackEl = row.selectFirst("a[href*='display.php?track=']") ?: continue
            val track = trackEl.text().trim()
            val trackUrl = trackEl.absUrl("href")

            val timeText = row.selectFirst("a[href*='youtu']")?.text()?.trim()
                ?: tds[1].text().trim()

            val player = tds[2].text().trim()
            val nation = tds[3].selectFirst("img")?.attr("alt")
                ?.ifBlank { tds[3].text().trim() } ?: tds[3].text().trim()
            val dateStr = tds[4].text().trim()
            val durationDays = tds[5].text().filter { it.isDigit() }.toIntOrNull()
            val character = tds[6].text().trim()
            val vehicle = tds[7].text().trim()

            val splits = fetchSplits(trackUrl, dateStr, timeText)

            val dto = RecordDto(
                date = dateStr,
                track = track,
                time = timeText,
                player = player,
                nation = nation,
                durationDays = durationDays,
                character = character,
                vehicle = vehicle,
                splits = splits
            )
            records.add(dto)
        }
        records
    }

    private fun fetchSplits(trackUrl: String, dateStr: String, timeText: String): SplitsDto? {
        val cached = headerCache[trackUrl]
        val doc = Jsoup.connect(trackUrl).userAgent(ua).get()

        val headerInfo = cached ?: detectHeaderInfo(doc)?.also { headerCache[trackUrl] = it }

        val rows: List<Element> = if (headerInfo != null) {
            val candidateTable = findTableWithHeader(doc, headerInfo)
            candidateTable?.select("tr:has(td)") ?: doc.select("tr:has(td)")
        } else {
            doc.select("tr:has(td)")
        }

        for (tr in rows) {
            val cols = tr.select("td")
            if (headerInfo != null) {
                if (cols.size <= headerInfo.timeIdx) continue
                val dateMatches = cols.getOrNull(headerInfo.dateIdx)?.text()?.trim() == dateStr
                val timeMatches = timeMatches(cols.getOrNull(headerInfo.timeIdx)?.text().orEmpty(), timeText)
                if (!dateMatches || !timeMatches) continue

                val lapStart = headerInfo.lapStartIdx.coerceAtMost(cols.size)
                val coinsIdx = headerInfo.coinsIdx.coerceAtMost(cols.size - 1)
                val shroomsIdx = headerInfo.shroomsIdx.coerceAtMost(cols.size - 1)

                val lapsSliceEnd = coinsIdx.coerceAtMost(cols.size)
                val laps = if (lapStart < lapsSliceEnd) {
                    cols.subList(lapStart, lapsSliceEnd).map { it.text().trim() }
                } else emptyList()

                val coinsRaw = cols.getOrNull(coinsIdx)?.text()?.trim().orEmpty()
                val shroomsRaw = cols.getOrNull(shroomsIdx)?.text()?.trim().orEmpty()

                return buildSplitsFromRaw(laps, coinsRaw, shroomsRaw)
            } else {

                if (cols.size < 6) continue
                val dateCell = cols.getOrNull(0)?.text()?.trim().orEmpty()
                val timeCell = cols.getOrNull(1)?.text()?.trim().orEmpty()
                if (dateCell != dateStr) continue
                if (!timeMatches(timeCell, timeText)) continue

                val lapStart = 5
                val coinsIdx = cols.size - 2
                val shroomsIdx = cols.size - 1
                val laps = if (lapStart < coinsIdx) {
                    cols.subList(lapStart, coinsIdx).map { it.text().trim() }
                } else emptyList()
                val coinsRaw = cols.getOrNull(coinsIdx)?.text()?.trim().orEmpty()
                val shroomsRaw = cols.getOrNull(shroomsIdx)?.text()?.trim().orEmpty()

                return buildSplitsFromRaw(laps, coinsRaw, shroomsRaw)
            }
        }
        return null
    }

    /**
     * Try to detect header indices by scanning tables' <th> or first row <td> if no <th>.
     */
    private fun detectHeaderInfo(doc: org.jsoup.nodes.Document): HeaderInfo? {
        val tables = doc.select("table")
        for (table in tables) {
            // Prefer explicit <th> header row if present
            val headerCells = table.select("thead th").ifEmpty { table.select("tr").first()?.select("th") ?: listOf() }
            val headerTexts = if (headerCells.isNotEmpty()) {
                headerCells.map { it.text().trim() }
            } else {
                // No <th>, try to use the first row's <td> as a pseudo-header (risky but helps)
                val firstRow = table.select("tr").first()
                firstRow?.select("td")?.map { it.text().trim() } ?: listOf()
            }

            if (headerTexts.isEmpty()) continue

            // heuristics to find indices
            val dateIdx = indexOfRegex(headerTexts, ".*date.*") ?: indexOfRegex(headerTexts, ".*datum.*") ?: 0
            val timeIdx = indexOfRegex(headerTexts, ".*time.*") ?: 1
            val playerIdx = indexOfRegex(headerTexts, ".*player.*") ?: 2
            val nationIdx = indexOfRegex(headerTexts, ".*nation.*|.*country.*|.*flag.*") ?: 3
            val durationIdx = indexOfRegex(headerTexts, ".*duration.*|.*days.*") ?: 4

            val coinsIdx = indexOfRegex(headerTexts, ".*coin(s)?.*") ?: (headerTexts.size - 2).coerceAtLeast(5)
            val shroomsIdx = indexOfRegex(headerTexts, ".*shroom(s)?.*") ?: (headerTexts.size - 1).coerceAtLeast(6)

            // find first "Lap" column index
            val lapStartIdx = indexOfRegex(headerTexts, ".*lap.*") ?: run {
                // if "Lap" not in headers, heuristically assume laps start after fixed columns (durationIdx + 1)
                (durationIdx + 1).coerceAtLeast(5)
            }

            // sanity check: coinsIdx should be > lapStartIdx
            if (coinsIdx <= lapStartIdx) {
                // skip this table — layout not as expected
                continue
            }

            return HeaderInfo(
                dateIdx = dateIdx,
                timeIdx = timeIdx,
                playerIdx = playerIdx,
                nationIdx = nationIdx,
                durationIdx = durationIdx,
                lapStartIdx = lapStartIdx,
                coinsIdx = coinsIdx,
                shroomsIdx = shroomsIdx
            )
        }
        return null
    }

    private fun findTableWithHeader(doc: org.jsoup.nodes.Document, headerInfo: HeaderInfo): Element? {
        val tables = doc.select("table")
        for (table in tables) {
            val rows = table.select("tr:has(td)")
            if (rows.isEmpty()) continue
            // quick check: does first data row have enough columns?
            val first = rows.first()
            if (first != null) {
                val cols = first.select("td")
                if (cols.size > headerInfo.shroomsIdx) return table
            }
        }
        return null
    }

    private fun indexOfRegex(list: List<String>, pattern: String): Int? {
        val r = Regex(pattern, RegexOption.IGNORE_CASE)
        val idx = list.indexOfFirst { r.containsMatchIn(it) }
        return if (idx >= 0) idx else null
    }

    private fun normalizeTimeDigits(s: String): String = s.filter { it.isDigit() }

    private fun timeMatches(a: String, b: String): Boolean {
        // Normalize and compare digits only (robust to different separators ' : " etc)
        val na = normalizeTimeDigits(a)
        val nb = normalizeTimeDigits(b)
        if (na.isEmpty() || nb.isEmpty()) return a.trim() == b.trim()
        // accept equality or one containing the other (some pages may omit ms or separators)
        return na == nb || na.contains(nb) || nb.contains(na)
    }

    private fun buildSplitsFromRaw(lapsRaw: List<String>, coinsRaw: String, shroomsRaw: String): SplitsDto {
        val laps = lapsRaw.map { it }.filter { it.isNotBlank() && it != "-" }

        fun parseDashInts(s: String): List<Int> {
            val cleaned = s.trim()
            if (cleaned.isEmpty()) return emptyList()
            return cleaned.split("-").mapNotNull { it.trim().toIntOrNull() }
        }

        var coins = parseDashInts(coinsRaw)
        var shrooms = parseDashInts(shroomsRaw)

        // Ensure coins/shrooms lengths match laps length
        val lapCount = laps.size
        if (coins.size < lapCount) {
            coins = coins + List(lapCount - coins.size) { 0 }
        } else if (coins.size > lapCount) {
            coins = coins.take(lapCount)
        }

        if (shrooms.size < lapCount) {
            shrooms = shrooms + List(lapCount - shrooms.size) { 0 }
        } else if (shrooms.size > lapCount) {
            shrooms = shrooms.take(lapCount)
        }

        return SplitsDto(
            laps = laps,
            coinsPerLap = coins,
            shroomsPerLap = shrooms
        )
    }



}
