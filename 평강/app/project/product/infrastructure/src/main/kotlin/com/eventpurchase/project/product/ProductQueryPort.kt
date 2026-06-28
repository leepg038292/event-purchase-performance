package com.eventpurchase.project.product

import com.eventpurchase.project.cart.ProductSummary

interface ProductQueryPort {
    fun findById(id: Long): ProductSummary?
}
