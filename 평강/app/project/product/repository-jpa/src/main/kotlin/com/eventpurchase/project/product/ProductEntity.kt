package com.eventpurchase.project.product

import com.eventpurchase.project.shared.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false) val productName: String,
    @Column(nullable = false) val brand: String,
    @Column(nullable = false) val category: String,
    val subCategory: String? = null,
    @Column(nullable = false) val price: BigDecimal,
    val discountRate: BigDecimal? = null,
    val rating: BigDecimal? = null,
    val reviewCount: Int? = null,
    @Column(length = 1000) val imageUrl: String? = null,
    @Column(length = 1000) val sourceUrl: String? = null,
    val source: String? = null
) : BaseTimeEntity() {

    fun toDomain() = Product(id, productName, brand, category, subCategory, price, discountRate, rating, reviewCount, imageUrl, sourceUrl, source)

    companion object {
        fun from(p: Product) = ProductEntity(p.id, p.productName, p.brand, p.category, p.subCategory, p.price, p.discountRate, p.rating, p.reviewCount, p.imageUrl, p.sourceUrl, p.source)
    }
}
