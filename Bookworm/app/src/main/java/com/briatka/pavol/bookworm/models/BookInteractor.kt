package com.briatka.pavol.bookworm.models

import com.briatka.pavol.bookworm.clients.RetrofitApiClient
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.interfaces.IBookInteractor
import com.briatka.pavol.bookworm.retrofit.RetrofitInstance
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class BookInteractor: IBookInteractor {

    private val API_KEY = "RrXbLty3WjyNPa58H93Rdw"

    override fun getIsbnData(isbn: String): Observable<NetworkRequestResult> {
        val client = RetrofitInstance.retrofitInstance.create(RetrofitApiClient::class.java)

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

    override fun getTitleData(title: String): Observable<NetworkRequestResult> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}