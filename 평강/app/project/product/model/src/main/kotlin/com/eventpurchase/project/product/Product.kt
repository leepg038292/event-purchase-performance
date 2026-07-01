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
) {
    init {
        require(productName.isNotBlank()) { "제품명은 공백이 불가합니다." }
        require(brand.isNotBlank()) { "브랜드명은 공백이 불가합니다" }
        require(category.isNotBlank()) { "카테고리 지정이 필요합니다" }
        require(price >= BigDecimal.ZERO) { "가격은 0 이상이어야 합니다" }
        require(discountRate == null || (discountRate >= BigDecimal.ZERO && discountRate <= BigDecimal("100"))) {
            "discountRate는 0과 100 사이만 가능합니다"
        }
        require(rating == null || (rating >= BigDecimal.ZERO && rating <= BigDecimal("5"))) {
            "rating은 0과 5 사이만 가능합니다"
        }
        require(reviewCount == null || reviewCount >= 0) {
            "reviewCount는 0 이상부터 가능합니다"
        }
    }
}
