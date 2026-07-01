package com.eventpurchase.project.product

import com.eventpurchase.project.shared.response.CursorResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface ProductService {
    fun getProducts(lastId: Long?, size: Int): CursorResponse<Product>
    fun getProductById(id: Long): Product
}

@Service
@Transactional(readOnly = true)
internal class ProductServiceImpl(
    private val productRepository: ProductRepository
) : ProductService {

    override fun getProducts(lastId: Long?, size: Int): CursorResponse<Product> {
        val products = if (lastId == null) productRepository.findAll(size + 1)
                       else productRepository.findByIdLessThan(lastId, size + 1)
        val hasNext = products.size > size
        val content = if (hasNext) products.dropLast(1) else products
        return CursorResponse(content, if (hasNext) content.last().id else null, hasNext)
    }

    override fun getProductById(id: Long): Product =
        productRepository.findById(id) ?: throw ProductNotFoundException(id)

}
