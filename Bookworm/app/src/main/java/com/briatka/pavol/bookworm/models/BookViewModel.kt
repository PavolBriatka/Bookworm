package com.briatka.pavol.bookworm.models

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.briatka.pavol.bookworm.customobjects.Book
import com.briatka.pavol.bookworm.interfaces.IBookInteractor
import com.briatka.pavol.bookworm.interfaces.NewBookViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject

class BookViewModel(application: Application) : AndroidViewModel(application), NewBookViewModel {

    private var interactor = ReplaySubject.create<IBookInteractor>()


    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private val bookErrorSubject = BehaviorSubject.create<NetworkRequestResult.Status>()
    private val bookSubject = BehaviorSubject.create<Book>()
    private val subjectIsbn = BehaviorSubject.create<String>()
    private val subjectTitle = BehaviorSubject.create<String>()

    private val subscription = CompositeDisposable()

    init {
        subscription.addAll(//getBookIsbnUpdate(),
                getBookTitleUpdate())
    }

    override fun getIsbnData(isbn: String) {

        val interactor = BookInteractor()

        Log.e("QWER11", "3")
        val result = interactor.getIsbnData(isbn)
        _book.postValue(result.value?.book)
        /*return interactor.getIsbnData(isbn)
                    .flatMap { response ->
                        when (response.status) {
                            NetworkRequestResult.Status.SUCCESS -> {
                                bookErrorSubject.onNext(NetworkRequestResult.Status.SUCCESS)

                                Observable.just(response.book!!)
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
        }*/
    }

    override fun getTitleData(title: String): Observable<Book> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setIsbn(isbn: String) {
        getIsbnData(isbn)
    }

    fun setTitle(title: String) {
        subjectTitle.onNext(title)
    }

    /*private fun getBookIsbnUpdate(): Disposable {
        return subjectIsbn.hide()
                .flatMap { data ->
                    getIsbnData(data)
                }
                .subscribe { book ->
                    bookSubject.onNext(book)
                }
    }*/

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

    override fun returnBookData(): LiveData<Book> {
        return LiveDataReactiveStreams.fromPublisher(
                bookSubject.hide().toFlowable(BackpressureStrategy.LATEST)
        )
    }

    fun setInteractor(mInteractor: IBookInteractor) {
        if (!interactor.hasComplete()) {
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