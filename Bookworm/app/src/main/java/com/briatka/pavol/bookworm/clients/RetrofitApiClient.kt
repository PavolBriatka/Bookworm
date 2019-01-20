package com.briatka.pavol.bookworm.clients

import com.briatka.pavol.bookworm.customobjects.BookObject
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitApiClient {

    @GET("/book/isbn/{isbn}")
    fun getReviewsIsbn(@Path("isbn") isbn: String, @Query("key") key: String): Observable<Response<BookObject>>

    @GET("/book/title.xml")
    fun getReviewsTitle(@Query("key") key: String, @Query("title") title: String): Observable<Response<BookObject>>
}