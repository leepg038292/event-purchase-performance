package com.eventpurchase.project.product

import com.eventpurchase.project.cart.ProductSummary
import org.springframework.stereotype.Component

@Component
internal class ProductQueryAdapter(
    private val productRepository: ProductRepository
) : ProductQueryPort {
    override fun findById(id: Long): ProductSummary? {
        val p = productRepository.findById(id) ?: return null
        return ProductSummary(p.id!!, p.productName, p.brand, p.price, p.discountRate, p.imageUrl)
    }
}
