package com.eventpurchase.project.cart

import org.springframework.data.jpa.repository.JpaRepository

interface CartItemJpaRepository : JpaRepository<CartItemEntity, Long> {
    fun findAllByCartId(cartId: Long): List<CartItemEntity>
}
