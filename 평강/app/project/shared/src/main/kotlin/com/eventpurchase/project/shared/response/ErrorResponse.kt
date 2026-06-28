package com.eventpurchase.project.shared.response

import com.eventpurchase.project.shared.exception.ErrorCode

data class ErrorResponse(
    val status: Int,
    val message: String
) {
    companion object {
        fun of(errorCode: ErrorCode) = ErrorResponse(errorCode.status, errorCode.message)
    }
}
