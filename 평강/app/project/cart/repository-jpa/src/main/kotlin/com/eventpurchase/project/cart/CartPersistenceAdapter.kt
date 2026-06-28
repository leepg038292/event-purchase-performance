package com.eventpurchase.project.cart

import org.springframework.stereotype.Component

@Component
internal class CartPersistenceAdapter(
    private val cartJpaRepository: CartJpaRepository,
    private val cartItemJpaRepository: CartItemJpaRepository
) : CartRepository {
    override fun findByUserId(userId: Long): Cart? {
        val entity = cartJpaRepository.findByUserId(userId) ?: return null
        return entity.toDomain(cartItemJpaRepository.findAllByCartId(entity.id!!))
    }
    override fun create(userId: Long) = cartJpaRepository.save(CartEntity(userId = userId)).toDomain()
}
