package com.eventpurchase.project.product

import java.math.BigDecimal

interface ProductLookupPort {
    fun findSummaryById(id: Long): ProductLookupResult?
}

data class ProductLookupResult(
    val id: Long,
    val productName: String,
    val brand: String,
    val price: BigDecimal,
    val discountRate: BigDecimal?,
    val imageUrl: String?
)
