package com.briatka.pavol.bookworm.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

object RetrofitInstance {

    private var retrofit: Retrofit? = null
    private val BASE_URL = "https://www.goodreads.com"

    val retrofitInstance: Retrofit
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(OkHttpClient())
                        .addConverterFactory(SimpleXmlConverterFactory.create())
                        .build()
            }

            return retrofit!!
        }
}