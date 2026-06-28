package com.eventpurchase.project.shared.response

data class SuccessResponse<T>(
    val status: Int,
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> of(status: Int, message: String, data: T?) = SuccessResponse(status, message, data)
    }
}
