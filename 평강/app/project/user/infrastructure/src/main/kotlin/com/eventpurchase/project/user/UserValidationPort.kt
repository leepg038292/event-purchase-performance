package com.eventpurchase.project.user

interface UserValidationPort {
    fun existsById(id: Long): Boolean
}
