package com.eventpurchase.project.cart

data class Cart(val id: Long? = null, val userId: Long, val cartItems: List<CartItem> = emptyList())
