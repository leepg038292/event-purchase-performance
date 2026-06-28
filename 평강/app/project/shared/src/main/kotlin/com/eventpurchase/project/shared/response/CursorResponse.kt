package com.eventpurchase.project.shared.response

data class CursorResponse<T>(
    val content: List<T>,
    val nextLastId: Long?,
    val hasNext: Boolean
)
