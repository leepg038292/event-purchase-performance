package com.eventpurchase.project.cart

import org.springframework.data.jpa.repository.JpaRepository

interface CartItemJpaRepository : JpaRepository<CartItemEntity, Long> {
    fun findByCartIdAndProductId(cartId: Long, productId: Long): CartItemEntity?
    fun findAllByCartId(cartId: Long): List<CartItemEntity>
    fun deleteAllByCartId(cartId: Long)
}
