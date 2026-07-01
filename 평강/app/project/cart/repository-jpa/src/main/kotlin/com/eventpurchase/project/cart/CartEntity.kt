package com.eventpurchase.project.cart

import com.eventpurchase.project.shared.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "carts")
class CartEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @Column(nullable = false) val userId: Long
) : BaseTimeEntity() {
    fun toDomain(items: List<CartItemEntity> = emptyList()) = Cart(id, userId, items.map { it.toDomain() })
}
