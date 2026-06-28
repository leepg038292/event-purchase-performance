package com.eventpurchase.project.cart

import com.eventpurchase.project.product.ProductQueryPort
import com.eventpurchase.project.shared.exception.CustomException
import com.eventpurchase.project.shared.exception.ErrorCode
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
    private val cartItemRepository: CartItemRepository,
    private val productQueryPort: ProductQueryPort,
    private val userValidationPort: UserValidationPort
) : CartService {

    @Transactional(readOnly = true)
    override fun getCart(userId: Long): CartView {
        val cart = cartRepository.findByUserId(userId) ?: cartRepository.create(userId)
        return buildView(cart.id!!, cart.userId)
    }

    override fun addItem(userId: Long, productId: Long, quantity: Int): CartView {
        if (!userValidationPort.existsById(userId)) throw CustomException(ErrorCode.USER_NOT_FOUND)
        productQueryPort.findById(productId) ?: throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)

        val cart = cartRepository.findByUserId(userId) ?: cartRepository.create(userId)
        val cartId = cart.id!!
        val existing = cartItemRepository.findByCartIdAndProductId(cartId, productId)
        if (existing != null) cartItemRepository.save(existing.copy(quantity = existing.quantity + quantity))
        else cartItemRepository.save(CartItem(cartId = cartId, productId = productId, quantity = quantity))

        return buildView(cartId, cart.userId)
    }

    override fun updateItemQuantity(userId: Long, cartItemId: Long, quantity: Int): CartView {
        val item = cartItemRepository.findById(cartItemId) ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        cartItemRepository.save(item.copy(quantity = quantity))
        val cart = cartRepository.findByUserId(userId) ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        return buildView(cart.id!!, userId)
    }

    override fun deleteItem(userId: Long, cartItemId: Long): CartView {
        cartItemRepository.findById(cartItemId) ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        cartItemRepository.deleteById(cartItemId)
        val cart = cartRepository.findByUserId(userId) ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
        return buildView(cart.id!!, userId)
    }

    override fun clearCart(userId: Long) {
        val cart = cartRepository.findByUserId(userId) ?: return
        cartItemRepository.deleteAllByCartId(cart.id!!)
    }

    private fun buildView(cartId: Long, userId: Long): CartView {
        val items = cartItemRepository.findAllByCartId(cartId).mapNotNull { item ->
            val product = productQueryPort.findById(item.productId) ?: return@mapNotNull null
            CartItemView(cartItemId = item.id, quantity = item.quantity, product = product)
        }
        return CartView(cartId = cartId, userId = userId, items = items)
    }
}
