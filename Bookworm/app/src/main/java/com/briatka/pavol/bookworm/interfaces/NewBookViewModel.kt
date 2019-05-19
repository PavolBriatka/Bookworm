package com.briatka.pavol.bookworm.interfaces

import android.arch.lifecycle.LiveData
import com.briatka.pavol.bookworm.customobjects.Book
import io.reactivex.Observable

interface NewBookViewModel {

    fun getIsbnData(isbn: String)

    fun getTitleData(title: String): Observable<Book>

    fun returnBookData(): LiveData<Book>
}