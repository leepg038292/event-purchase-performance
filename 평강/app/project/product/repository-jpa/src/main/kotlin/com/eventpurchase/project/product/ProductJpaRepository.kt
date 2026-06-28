package com.eventpurchase.project.product

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findAllBy(pageable: Pageable): List<ProductEntity>
    fun findByIdLessThan(lastId: Long, pageable: Pageable): List<ProductEntity>
}
