package com.eventpurchase.project.cart

import jakarta.persistence.*

@Entity
@Table(name = "cart_items")
class CartItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @Column(nullable = false) val cartId: Long,
    @Column(nullable = false) val productId: Long,  // cross-module JPA 결합 방지
    @Column(nullable = false) val quantity: Int
) : BaseTimeEntity() {
    fun toDomain() = CartItem(id, cartId, productId, quantity)
    companion object { fun from(i: CartItem) = CartItemEntity(i.id, i.cartId, i.productId, i.quantity) }
}
