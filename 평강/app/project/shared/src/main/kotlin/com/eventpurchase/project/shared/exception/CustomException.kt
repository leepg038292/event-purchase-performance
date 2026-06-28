package com.eventpurchase.project.shared.exception

class CustomException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
