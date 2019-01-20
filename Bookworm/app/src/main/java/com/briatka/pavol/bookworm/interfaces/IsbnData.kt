package com.briatka.pavol.bookworm.interfaces

import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.customobjects.BookObject
import com.briatka.pavol.bookworm.models.NetworkRequestResult
import io.reactivex.Observable


interface IsbnData {

    fun getIsbnData ( isbn: String): Observable<NetworkRequestResult>
}