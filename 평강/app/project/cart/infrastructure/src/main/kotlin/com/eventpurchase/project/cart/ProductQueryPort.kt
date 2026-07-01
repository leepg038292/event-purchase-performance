package com.eventpurchase.project.cart

interface ProductQueryPort {
    fun findById(id: Long): ProductSummary?
}
