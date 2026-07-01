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

    override fun create(userId: Long) =
        cartJpaRepository.save(CartEntity(userId = userId)).toDomain()

    override fun save(cart: Cart): Cart {
        val cartId = cart.id!!
        val existingIds = cartItemJpaRepository.findAllByCartId(cartId).mapNotNull { it.id }.toSet()
        val newIds = cart.cartItems.mapNotNull { it.id }.toSet()

        existingIds.subtract(newIds).forEach { cartItemJpaRepository.deleteById(it) }

        val savedItems = cart.cartItems.map { cartItemJpaRepository.save(CartItemEntity.from(it)).toDomain() }
        return cart.copy(cartItems = savedItems)
    }
}
