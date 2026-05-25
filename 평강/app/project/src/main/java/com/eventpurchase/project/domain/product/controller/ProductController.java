package com.eventpurchase.project.domain.product.controller;

import com.eventpurchase.project.domain.product.dto.response.ProductResponse;
import com.eventpurchase.project.domain.product.service.ProductService;
import com.eventpurchase.project.global.common.response.CursorResponse;
import com.eventpurchase.project.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @GetMapping
    public ResponseEntity<SuccessResponse<CursorResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size)
    {
        CursorResponse<ProductResponse> products = productService.getProducts(lastId, size);
        return ResponseEntity.ok(SuccessResponse.of(HttpStatus.OK, "상품 목록 조회 성공", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse productResponse = productService.getProductById(id);
        return ResponseEntity.ok(SuccessResponse.of(HttpStatus.OK, "상품 상세 조회 성공", productResponse));
    }

}
