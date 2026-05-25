package com.eventpurchase.project.domain.product.service;

import com.eventpurchase.project.domain.product.dto.response.ProductResponse;
import com.eventpurchase.project.domain.product.entity.ProductEntity;
import com.eventpurchase.project.domain.product.repository.ProductRepository;
import com.eventpurchase.project.global.common.exception.CustomException;
import com.eventpurchase.project.global.common.exception.ErrorCode;
import com.eventpurchase.project.global.common.response.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public CursorResponse<ProductResponse> getProducts(Long lastId, int size) {

        Pageable pageable = PageRequest.of(0, size + 1);
        List<ProductEntity> products;

        if(lastId == null) {
            products = productRepository.findAll(pageable).getContent();
        }
        else{
            products = productRepository.findByIdLessThanOrderByIdDesc(lastId, pageable);
        }

        boolean hasNext = products.size() > size;
        if (hasNext) products = products.subList(0, size);
        Long nextLastId = products.isEmpty() ? null : products.getLast().getId();

        return new CursorResponse<>(products.stream().map(ProductResponse::from).toList(), nextLastId, hasNext);
    }


    public ProductResponse getProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);


    }

}
