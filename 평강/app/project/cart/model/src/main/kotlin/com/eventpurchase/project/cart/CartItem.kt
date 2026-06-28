package com.eventpurchase.project.cart

data class CartItem(val id: Long? = null, val cartId: Long, val productId: Long, val quantity: Int)
