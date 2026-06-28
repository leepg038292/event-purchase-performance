package com.eventpurchase.project

import com.eventpurchase.project.shared.exception.CustomException
import com.eventpurchase.project.shared.response.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        val errorCode = e.errorCode
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(status = errorCode.status, message = errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(500)
            .body(ErrorResponse(status = 500, message = "서버 오류가 발생했습니다."))
    }
}
