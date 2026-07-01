package com.eventpurchase.project.cart

import com.eventpurchase.project.product.ProductLookupPort
import org.springframework.stereotype.Component

@Component
internal class ProductQueryAdapter(
    private val productLookupPort: ProductLookupPort
) : ProductQueryPort {
    override fun findById(id: Long): ProductSummary? {
        val p = productLookupPort.findSummaryById(id) ?: return null
        return ProductSummary(p.id, p.productName, p.brand, p.price, p.discountRate, p.imageUrl)
    }
}
