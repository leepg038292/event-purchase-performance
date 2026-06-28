package com.eventpurchase.project.cart

data class CartView(val cartId: Long?, val userId: Long, val items: List<CartItemView>)

data class CartItemView(val cartItemId: Long?, val quantity: Int, val product: ProductSummary)
