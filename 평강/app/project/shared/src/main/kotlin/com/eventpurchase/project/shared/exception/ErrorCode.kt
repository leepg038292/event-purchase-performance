package com.eventpurchase.project.shared.exception

enum class ErrorCode(val status: Int, val message: String) {
    PRODUCT_NOT_FOUND(404, "상품을 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "유저를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(404, "장바구니 상품을 찾을 수 없습니다.")
}
