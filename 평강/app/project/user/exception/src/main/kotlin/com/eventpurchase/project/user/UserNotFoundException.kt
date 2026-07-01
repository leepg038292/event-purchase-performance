package com.eventpurchase.project.user

class UserNotFoundException(userId: Long) : RuntimeException("User not found: $userId")
