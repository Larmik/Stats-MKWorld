package fr.harmoniamk.statsmkworld.api

import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RetrofitUtils {

    // Réutilisés entre tous les appels (pool de connexions / DNS / threads partagés).
    private val baseClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    private val defaultFactory: Converter.Factory by lazy { MoshiConverterFactory.create() }
    private val callAdapterFactory by lazy { NetworkResponseCallAdapterFactory() }

    // Une instance Retrofit (et son client) par couple (url, timeout).
    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    fun <T> createRetrofit(
        apiClass: Class<T>,
        url: String,
        factory: Converter.Factory = defaultFactory,
        timeout: Long? = null
    ): T {
        val retrofit = retrofitCache.getOrPut("$url|${timeout ?: 0}|${System.identityHashCode(factory)}") {
            // newBuilder() partage le pool de connexions / dispatcher du client de base.
            val client = timeout?.let {
                baseClient.newBuilder()
                    .callTimeout(it, TimeUnit.SECONDS)
                    .connectTimeout(it, TimeUnit.SECONDS)
                    .writeTimeout(it, TimeUnit.SECONDS)
                    .readTimeout(it, TimeUnit.SECONDS)
                    .build()
            } ?: baseClient

            Retrofit.Builder()
                .baseUrl(url)
                .addCallAdapterFactory(callAdapterFactory)
                .addConverterFactory(factory)
                .client(client)
                .build()
        }
        return retrofit.create(apiClass)
    }

}