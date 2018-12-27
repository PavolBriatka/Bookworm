package com.briatka.pavol.bookworm.models

import com.briatka.pavol.bookworm.clients.BookIsbnReviewClient
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.customobjects.BookObject
import com.briatka.pavol.bookworm.interfaces.IsbnData
import com.briatka.pavol.bookworm.retrofit.RetrofitInstance
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import retrofit2.Response

class IsbnRequestModel: IsbnData {

    private val API_KEY = "RrXbLty3WjyNPa58H93Rdw"

    override fun getIsbnData(isbn: String): Observable<NetworkRequestResult> {
        val client = RetrofitInstance.retrofitInstance.create(BookIsbnReviewClient::class.java)

        return client.getReviewsIsbn(isbn, API_KEY)
                .map {response ->
                    when (response.code()) {
                        200 -> NetworkRequestResult.onSuccess(response.body()?.book ?: Book())
                        401 -> NetworkRequestResult.onUnauthorizedError()
                        500 -> NetworkRequestResult.onServerError()
                        else -> NetworkRequestResult.onUnknownError()
                    }
                }
                .onErrorReturn {
                    NetworkRequestResult.onUnknownError()
                }
                .subscribeOn(Schedulers.io())

    }

}