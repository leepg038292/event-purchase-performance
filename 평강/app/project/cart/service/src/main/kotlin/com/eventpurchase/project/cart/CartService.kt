package com.eventpurchase.project.cart

import com.eventpurchase.project.cart.ProductQueryPort
import com.eventpurchase.project.user.UserNotFoundException
import com.eventpurchase.project.user.UserValidationPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CartService {
    fun getCart(userId: Long): CartView
    fun addItem(userId: Long, productId: Long, quantity: Int): CartView
    fun updateItemQuantity(userId: Long, cartItemId: Long, quantity: Int): CartView
    fun deleteItem(userId: Long, cartItemId: Long): CartView
    fun clearCart(userId: Long)
}

@Service
@Transactional
internal class CartServiceImpl(
    private val cartRepository: CartRepository,
    private val productQueryPort: ProductQueryPort,
    private val userValidationPort: UserValidationPort
) : CartService {

    @Transactional(readOnly = true)
    override fun getCart(userId: Long): CartView {
        val cart = cartRepository.findByUserId(userId) ?: cartRepository.create(userId)
        return buildView(cart)
    }

    override fun addItem(userId: Long, productId: Long, quantity: Int): CartView {
        if (!userValidationPort.existsById(userId)) throw UserNotFoundException(userId)
        productQueryPort.findById(productId) ?: throw ProductNotAvailableException(productId)

        val cart = cartRepository.findByUserId(userId) ?: cartRepository.create(userId)
        val updated = cartRepository.save(cart.addItem(productId, quantity))
        return buildView(updated)
    }

    override fun updateItemQuantity(userId: Long, cartItemId: Long, quantity: Int): CartView {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CartItemNotFoundException(cartItemId)
        val updated = cartRepository.save(cart.changeItemQuantity(cartItemId, quantity))
        return buildView(updated)
    }

    override fun deleteItem(userId: Long, cartItemId: Long): CartView {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CartItemNotFoundException(cartItemId)
        val updated = cartRepository.save(cart.removeItem(cartItemId))
        return buildView(updated)
    }

    override fun clearCart(userId: Long) {
        val cart = cartRepository.findByUserId(userId) ?: return
        cartRepository.save(cart.clear())
    }

    private fun buildView(cart: Cart): CartView {
        val items = cart.cartItems.mapNotNull { item ->
            val product = productQueryPort.findById(item.productId) ?: return@mapNotNull null
            CartItemView(cartItemId = item.id, quantity = item.quantity, product = product)
        }
        return CartView(cartId = cart.id!!, userId = cart.userId, items = items)
    }
}
