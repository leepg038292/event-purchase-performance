package com.eventpurchase.project.product

import java.math.BigDecimal

data class Product(
    val id: Long? = null,
    val productName: String,
    val brand: String,
    val category: String,
    val subCategory: String? = null,
    val price: BigDecimal,
    val discountRate: BigDecimal? = null,
    val rating: BigDecimal? = null,
    val reviewCount: Int? = null,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val source: String? = null
)
