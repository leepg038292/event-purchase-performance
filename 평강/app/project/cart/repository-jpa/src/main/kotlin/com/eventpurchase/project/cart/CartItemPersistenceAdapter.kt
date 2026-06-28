package com.eventpurchase.project.cart

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class CartItemPersistenceAdapter(
    private val cartItemJpaRepository: CartItemJpaRepository
) : CartItemRepository {
    override fun findById(id: Long) = cartItemJpaRepository.findByIdOrNull(id)?.toDomain()
    override fun findByCartIdAndProductId(cartId: Long, productId: Long) = cartItemJpaRepository.findByCartIdAndProductId(cartId, productId)?.toDomain()
    override fun findAllByCartId(cartId: Long) = cartItemJpaRepository.findAllByCartId(cartId).map { it.toDomain() }
    override fun save(item: CartItem) = cartItemJpaRepository.save(CartItemEntity.from(item)).toDomain()
    override fun deleteById(id: Long) = cartItemJpaRepository.deleteById(id)
    override fun deleteAllByCartId(cartId: Long) = cartItemJpaRepository.deleteAllByCartId(cartId)
}
