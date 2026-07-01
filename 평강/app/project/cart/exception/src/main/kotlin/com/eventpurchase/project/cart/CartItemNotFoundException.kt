package com.eventpurchase.project.cart

class CartItemNotFoundException(cartItemId: Long) : RuntimeException("CartItem not found: $cartItemId")
