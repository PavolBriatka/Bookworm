package com.briatka.pavol.bookworm.interfaces

import android.arch.lifecycle.LiveData
import com.briatka.pavol.bookworm.models.NetworkRequestResult
import io.reactivex.Observable


interface IBookInteractor {

    fun getIsbnData ( isbn: String): LiveData<NetworkRequestResult>

    fun getTitleData(title: String): Observable<NetworkRequestResult>
}