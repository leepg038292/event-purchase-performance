package com.eventpurchase.project.global.common.response;

import com.eventpurchase.project.global.common.exception.ErrorCode;

public record ErrorResponse (
        int status,
        String message



)
{
    public static ErrorResponse of(ErrorCode errorcode) {
        return new ErrorResponse(errorcode.getStatus().value(), errorcode.getMessage());
    }


}

