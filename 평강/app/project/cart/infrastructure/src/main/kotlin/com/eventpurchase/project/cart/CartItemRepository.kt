package com.eventpurchase.project.cart

interface CartItemRepository {
    fun findById(id: Long): CartItem?
    fun findByCartIdAndProductId(cartId: Long, productId: Long): CartItem?
    fun findAllByCartId(cartId: Long): List<CartItem>
    fun save(item: CartItem): CartItem
    fun deleteById(id: Long)
    fun deleteAllByCartId(cartId: Long)
}
