package com.eventpurchase.project.product

interface ProductRepository {
    fun findById(id: Long): Product?
    fun findAll(size: Int): List<Product>
    fun findByIdLessThan(lastId: Long, size: Int): List<Product>
    fun count(): Long
    fun saveAll(products: List<Product>)
}
