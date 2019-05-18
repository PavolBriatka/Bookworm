package com.briatka.pavol.bookworm.models

import android.util.Log
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.interfaces.BookViewModel
import com.briatka.pavol.bookworm.interfaces.IBookInteractor
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject

class BookPresenter : BookViewModel {


    private var interactor = ReplaySubject.create<IBookInteractor>()

    private val bookErrorSubject = BehaviorSubject.create<NetworkRequestResult.Status>()
    private val bookSubject = BehaviorSubject.create<Book>()
    private val subjectIsbn = BehaviorSubject.create<String>()
    private val subjectTitle = BehaviorSubject.create<String>()

    private val subscription = CompositeDisposable()

    init {
        subscription.addAll(getBookIsbnUpdate(),
                getBookTitleUpdate())
    }

    override fun getIsbnData(isbn: String): Observable<Book> {
        Log.e("QWER","getisbn")

        return interactor.flatMap { interactor ->
            interactor.getIsbnData(isbn)
                    .flatMap { response ->
                        Log.d("QWER","status " + response.status)
                        when (response.status) {
                            NetworkRequestResult.Status.SUCCESS -> {
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

    fun setIsbn(isbn: String) {
        Log.e("QWER","setisbn")
        subjectIsbn.onNext(isbn)
    }

    fun setTitle(title: String) {
        subjectTitle.onNext(title)
    }

    private fun getBookIsbnUpdate(): Disposable {
        return subjectIsbn.hide()
                .flatMap { data ->
                    getIsbnData(data)
                }
                .subscribe { book ->
                    bookSubject.onNext(book)
                }
    }

    private fun getBookTitleUpdate(): Disposable {
        return subjectTitle.hide()
                .skip(1)
                .flatMap { data ->
                    getTitleData(data)
                }
                .subscribe { book ->
                    bookSubject.onNext(book)
                }
    }

    override fun returnBookData(): Observable<Book> {
        return bookSubject.hide()
    }

    fun setInteractor(mInteractor: IBookInteractor) {
        if (!interactor.hasComplete()) {
            Log.d("QWER","initinteractor")
            interactor.onNext(mInteractor)
            interactor.onComplete()
        }
    }

    fun isInteractorReady(): Boolean {
        return interactor.hasValue()
    }


    fun finish() {
        if (!interactor.hasComplete()) interactor.onComplete()
        bookErrorSubject.onComplete()
        subscription.dispose()
    }
}