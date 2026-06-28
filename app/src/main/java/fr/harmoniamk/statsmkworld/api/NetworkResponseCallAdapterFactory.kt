package fr.harmoniamk.statsmkworld.api

import com.google.firebase.crashlytics.FirebaseCrashlytics
import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class NetworkResponseCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        val callType = getParameterUpperBound(0, returnType as ParameterizedType)
        if (getRawType(callType) != NetworkResponse::class.java) return null
        val responseType = getParameterUpperBound(0, callType as ParameterizedType)
        return NetworkResponseCallAdapter<Any>(responseType)
    }
}

private class NetworkResponseCallAdapter<T>(
    private val responseType: Type
) : CallAdapter<T, Call<NetworkResponse<T>>> {

    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Call<NetworkResponse<T>> = NetworkResponseCall(call)
}

private class NetworkResponseCall<T>(
    private val delegate: Call<T>
) : Call<NetworkResponse<T>> {

    override fun enqueue(callback: Callback<NetworkResponse<T>>) {
        delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                callback.onResponse(this@NetworkResponseCall, Response.success(response.toNetworkResponse()))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback.onResponse(this@NetworkResponseCall, Response.success(t.toNetworkError()))
            }
        })
    }

    override fun execute(): Response<NetworkResponse<T>> = try {
        Response.success(delegate.execute().toNetworkResponse())
    } catch (t: Throwable) {
        Response.success(t.toNetworkError())
    }

    override fun clone(): Call<NetworkResponse<T>> = NetworkResponseCall(delegate.clone())
    override fun isExecuted(): Boolean = delegate.isExecuted
    override fun cancel() = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}

/** Conversion centralisée Retrofit → NetworkResponse, avec journalisation Crashlytics des erreurs. */
private fun <T> Response<T>.toNetworkResponse(): NetworkResponse<T> = when {
    isSuccessful -> body()?.let { NetworkResponse.Success(it) } ?: NetworkResponse.Error("Unknown")
    else -> {
        val message = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: message()
        FirebaseCrashlytics.getInstance().log("HTTP ${code()} error: $message")
        NetworkResponse.Error(message)
    }
}

private fun <T> Throwable.toNetworkError(): NetworkResponse<T> {
    FirebaseCrashlytics.getInstance().recordException(this)
    return NetworkResponse.Error(message ?: "Unknown")
}