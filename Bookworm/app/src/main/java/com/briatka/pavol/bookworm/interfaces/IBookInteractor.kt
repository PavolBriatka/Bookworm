package com.briatka.pavol.bookworm.interfaces

import com.briatka.pavol.bookworm.models.NetworkRequestResult
import io.reactivex.Observable


interface IBookInteractor {

    fun getIsbnData ( isbn: String): Observable<NetworkRequestResult>

    fun getTitleData(title: String): Observable<NetworkRequestResult>
}