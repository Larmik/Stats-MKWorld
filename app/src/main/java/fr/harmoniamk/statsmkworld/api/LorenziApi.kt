package fr.harmoniamk.statsmkworld.api

import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziTableRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

/** Générateur de tab « GameBoards v2 » de HLorenzi (service tiers, sans authentification). */
interface LorenziApi {
    companion object {
        const val baseUrl: String = "https://gb2.hlorenzi.com/"
    }

    /** Image PNG du tab (`image/png`). */
    @POST("table.png")
    suspend fun getTable(@Body request: LorenziTableRequest): NetworkResponse<ResponseBody>
}
