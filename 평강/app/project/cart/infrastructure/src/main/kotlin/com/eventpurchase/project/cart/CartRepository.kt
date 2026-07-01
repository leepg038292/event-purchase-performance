package com.eventpurchase.project.cart

interface CartRepository {
    fun findByUserId(userId: Long): Cart?
    fun create(userId: Long): Cart
    fun save(cart: Cart): Cart
}
