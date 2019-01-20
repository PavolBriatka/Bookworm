package com.briatka.pavol.bookworm.interfaces

import com.briatka.pavol.bookworm.customobjects.Book
import io.reactivex.Observable

interface BookViewModel {

    fun getIsbnData(isbn: String): Observable<Book>

    fun getTitleData(title: String): Observable<Book>

}