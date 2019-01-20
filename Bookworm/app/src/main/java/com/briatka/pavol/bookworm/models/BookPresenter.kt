package com.briatka.pavol.bookworm.models

import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.interfaces.BookViewModel
import com.briatka.pavol.bookworm.interfaces.IBookInteractor
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject

class BookPresenter : BookViewModel {


    private var interactor = ReplaySubject.create<IBookInteractor>()

    private val bookErrorSubject = BehaviorSubject.create<NetworkRequestResult.Status>()

    override fun getIsbnData(isbn: String): Observable<Book> {
        return interactor.flatMap { interactor ->
            interactor.getIsbnData(isbn)
                    .flatMap { response ->
                        when (response.status) {
                            NetworkRequestResult.Status.NONE -> {
                                bookErrorSubject.onNext(NetworkRequestResult.Status.SUCCESS)

                                Observable.just(response.book)
                            }
                            NetworkRequestResult.Status.SERVER_ERROR -> {
                                bookErrorSubject.onNext(NetworkRequestResult.Status.SERVER_ERROR)
                                Observable.empty<Book>()
                            }
                            NetworkRequestResult.Status.ERROR_401 -> {
                                bookErrorSubject.onNext(NetworkRequestResult.Status.ERROR_401)
                                Observable.empty<Book>()
                            }
                            else -> {
                                bookErrorSubject.onNext(NetworkRequestResult.Status.UNKNOWN_ERROR)
                                Observable.empty<Book>()
                            }
                        }
                    }
        }
    }

    override fun getTitleData(title: String): Observable<Book> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun finish() {
        if (!interactor.hasComplete()) interactor.onComplete()
        bookErrorSubject.onComplete()
    }
}