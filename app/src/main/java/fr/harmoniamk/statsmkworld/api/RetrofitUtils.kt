package fr.harmoniamk.statsmkworld.api

import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtils {

    fun <T> createRetrofit(
        apiClass: Class<T>,
        url: String,
        factory: Converter.Factory = MoshiConverterFactory.create(),
        timeout: Long? = null
    ): T {

        val client = OkHttpClient.Builder()

        timeout?.let {
            client.callTimeout(it, TimeUnit.SECONDS)
                .connectTimeout(it, TimeUnit.SECONDS)
                .writeTimeout(it, TimeUnit.SECONDS)
                .readTimeout(it, TimeUnit.SECONDS)
        }

        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(factory)
            .client(client.build())
            .build()
            .create(apiClass)
    }

}