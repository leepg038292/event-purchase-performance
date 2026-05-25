package com.eventpurchase.project.domain.product.repository;

import com.eventpurchase.project.domain.product.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // lastId보다 작은 id를 내림차순으로 size만큼 가져옴
    List<ProductEntity> findByIdLessThanOrderByIdDesc(Long lastId, Pageable pageable);

}
