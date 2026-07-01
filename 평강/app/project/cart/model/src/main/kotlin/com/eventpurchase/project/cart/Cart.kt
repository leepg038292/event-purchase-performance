package com.eventpurchase.project.cart

data class Cart(
    val id: Long? = null,
    val userId: Long,
    val cartItems: List<CartItem> = emptyList()
) {
    init {
        require(userId > 0) { "userId must be positive" }
    }

    fun addItem(productId: Long, quantity: Int): Cart {
        val existing = cartItems.find { it.productId == productId }
        return if (existing != null) {
            copy(cartItems = cartItems.map {
                if (it.productId == productId) it.increase(quantity) else it
            })
        } else {
            copy(cartItems = cartItems + CartItem(cartId = id!!, productId = productId, quantity = quantity))
        }
    }

    fun removeItem(cartItemId: Long): Cart {
        require(cartItems.any { it.id == cartItemId }) { "CartItem not found: $cartItemId" }
        return copy(cartItems = cartItems.filter { it.id != cartItemId })
    }

    fun changeItemQuantity(cartItemId: Long, quantity: Int): Cart {
        require(cartItems.any { it.id == cartItemId }) { "CartItem not found: $cartItemId" }
        return copy(cartItems = cartItems.map {
            if (it.id == cartItemId) it.changeQuantity(quantity) else it
        })
    }

    fun clear(): Cart = copy(cartItems = emptyList())
}
