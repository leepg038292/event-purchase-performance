package com.eventpurchase.project.user

interface UserRepository {
    fun findById(id: Long): User?
    fun existsById(id: Long): Boolean
    fun save(user: User): User
}
