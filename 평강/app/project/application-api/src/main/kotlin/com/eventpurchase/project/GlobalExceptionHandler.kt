package com.eventpurchase.project

import com.eventpurchase.project.cart.CartItemNotFoundException
import com.eventpurchase.project.cart.ProductNotAvailableException
import com.eventpurchase.project.product.ProductNotFoundException
import com.eventpurchase.project.shared.response.ErrorResponse
import com.eventpurchase.project.user.UserNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404).body(ErrorResponse(404, "상품을 찾을 수 없습니다."))

    @ExceptionHandler(ProductNotAvailableException::class)
    fun handleProductNotAvailable(e: ProductNotAvailableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404).body(ErrorResponse(404, "상품을 찾을 수 없습니다."))

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404).body(ErrorResponse(404, "유저를 찾을 수 없습니다."))

    @ExceptionHandler(CartItemNotFoundException::class)
    fun handleCartItemNotFound(e: CartItemNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404).body(ErrorResponse(404, "장바구니 상품을 찾을 수 없습니다."))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(400).body(ErrorResponse(400, e.message ?: "잘못된 요청입니다."))

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(500).body(ErrorResponse(500, "서버 오류가 발생했습니다."))
}
