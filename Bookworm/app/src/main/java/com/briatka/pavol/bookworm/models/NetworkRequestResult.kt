package com.briatka.pavol.bookworm.models

import com.briatka.pavol.bookworm.customobjects.Book

class NetworkRequestResult(val book: Book?, val status: Status = Status.NONE) {

    companion object {

        fun onSuccess(book: Book): NetworkRequestResult {
            return NetworkRequestResult(book, Status.SUCCESS)
        }
        fun onServerError(): NetworkRequestResult {
            return NetworkRequestResult(null, Status.SERVER_ERROR)
        }

        fun onUnauthorizedError(): NetworkRequestResult {
            return NetworkRequestResult(null, Status.ERROR_401 )
        }

        fun onUnknownError(): NetworkRequestResult {
            return NetworkRequestResult(null,Status.UNKNOWN_ERROR)
        }

    }

    enum class Status {
        NONE,
        SUCCESS,
        SERVER_ERROR,
        ERROR_401,
        UNKNOWN_ERROR
    }
}