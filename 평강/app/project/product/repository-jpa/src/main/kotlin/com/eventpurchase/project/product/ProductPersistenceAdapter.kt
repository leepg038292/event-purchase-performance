package com.eventpurchase.project.product

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class ProductPersistenceAdapter(
    private val productJpaRepository: ProductJpaRepository
) : ProductRepository {

    private fun pageable(size: Int) = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"))

    override fun findById(id: Long) = productJpaRepository.findByIdOrNull(id)?.toDomain()
    override fun findAll(size: Int) = productJpaRepository.findAllBy(pageable(size)).map { it.toDomain() }
    override fun findByIdLessThan(lastId: Long, size: Int) = productJpaRepository.findByIdLessThan(lastId, pageable(size)).map { it.toDomain() }
    override fun count() = productJpaRepository.count()
    override fun saveAll(products: List<Product>) { productJpaRepository.saveAll(products.map { ProductEntity.from(it) }) }
}
