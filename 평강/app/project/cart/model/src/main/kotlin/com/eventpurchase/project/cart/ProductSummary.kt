package com.eventpurchase.project.cart

import java.math.BigDecimal

// cart가 자체 정의한 상품 뷰 - product 모듈 타입 없음
data class ProductSummary(
    val id: Long, val productName: String, val brand: String,
    val price: BigDecimal, val discountRate: BigDecimal?, val imageUrl: String?
)
