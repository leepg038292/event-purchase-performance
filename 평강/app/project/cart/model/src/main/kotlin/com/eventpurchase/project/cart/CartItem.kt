package com.eventpurchase.project.cart

data class CartItem(
    val id: Long? = null,
    val cartId: Long,
    val productId: Long,
    val quantity: Int
) {
    init {
        require(quantity > 0) { "quantity must be positive" }
        require(productId > 0){"productId must be positive"}
        require(cartId > 0){"cartId must be positive"}
    }

    fun changeQuantity(quantity: Int): CartItem =
        copy(quantity = quantity)

    fun increase(quantity: Int): CartItem =
        copy(quantity = this.quantity + quantity)
}