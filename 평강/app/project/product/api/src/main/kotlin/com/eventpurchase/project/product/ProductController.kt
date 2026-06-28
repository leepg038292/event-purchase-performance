package com.eventpurchase.project.product

import com.eventpurchase.project.shared.response.CursorResponse
import com.eventpurchase.project.shared.response.SuccessResponse
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {

    @GetMapping
    fun getProducts(
        @RequestParam(required = false) lastId: Long?,
        @RequestParam(defaultValue = "20") size: Int
    ): SuccessResponse<CursorResponse<ProductResponse>> {
        val result = productService.getProducts(lastId, size)
        return SuccessResponse.of(200, "상품 목록 조회 성공",
            CursorResponse(result.content.map { it.toResponse() }, result.nextLastId, result.hasNext))
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): SuccessResponse<ProductResponse> =
        SuccessResponse.of(200, "상품 상세 조회 성공", productService.getProductById(id).toResponse())
}

data class ProductResponse(
    val id: Long?, val productName: String, val brand: String, val category: String,
    val subCategory: String?, val price: BigDecimal, val discountRate: BigDecimal?,
    val rating: BigDecimal?, val reviewCount: Int?, val imageUrl: String?, val source: String?
)

private fun Product.toResponse() = ProductResponse(id, productName, brand, category, subCategory, price, discountRate, rating, reviewCount, imageUrl, source)
