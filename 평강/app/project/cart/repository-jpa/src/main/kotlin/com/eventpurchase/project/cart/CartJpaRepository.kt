package com.eventpurchase.project.cart

import org.springframework.data.jpa.repository.JpaRepository

interface CartJpaRepository : JpaRepository<CartEntity, Long> {
    fun findByUserId(userId: Long): CartEntity?
}
