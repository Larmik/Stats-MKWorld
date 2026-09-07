package fr.harmoniamk.statsmkworld.model.network

sealed class NetworkResponse<T> {

    data class Success<T>(val response: T): NetworkResponse<T>()
    data class Error<T>(val message: String): NetworkResponse<T>()

    val successResponse: T?
        get() = (this as? Success)?.response

}