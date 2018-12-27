package com.briatka.pavol.bookworm.models

import com.briatka.pavol.bookworm.customobjects.Book

class NetworkRequestResult(val book: Book, val status: Status = Status.NONE) {

    companion object {

        fun onSuccess(book: Book): NetworkRequestResult {
            return NetworkRequestResult(book, Status.NONE)
        }
        fun onServerError(): NetworkRequestResult {
            return NetworkRequestResult(Book(), Status.SERVER_ERROR)
        }

        fun onUnauthorizedError(): NetworkRequestResult {
            return NetworkRequestResult(Book(), Status.ERROR_401 )
        }

        fun onUnknownError(): NetworkRequestResult {
            return NetworkRequestResult(Book(),Status.UNKNOWN_ERROR)
        }

    }

    enum class Status {
        NONE,
        SERVER_ERROR,
        ERROR_401,
        UNKNOWN_ERROR
    }
}