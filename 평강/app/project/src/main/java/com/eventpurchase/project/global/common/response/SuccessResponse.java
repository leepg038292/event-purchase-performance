package com.eventpurchase.project.global.common.response;

import org.springframework.http.HttpStatus;

public record SuccessResponse<T> (
        int status,
        String message,
        T data
)
    {

        public static <T> SuccessResponse<T> of(HttpStatus status, String message, T data) {
            return new SuccessResponse<T>(status.value(), message, data);
        }
}
