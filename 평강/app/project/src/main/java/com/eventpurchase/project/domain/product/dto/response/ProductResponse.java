package com.eventpurchase.project.domain.product.dto.response;


import com.eventpurchase.project.domain.product.entity.ProductEntity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductResponse {
    private Long id;
    private String productName;
    private String brand;
    private String category;
    private String subCategory;
    private BigDecimal price;
    private BigDecimal discountRate;
    private BigDecimal rating;
    private Integer reviewCount;
    private String imageUrl;


    public static ProductResponse from(ProductEntity product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .subCategory(product.getSubCategory())
                .price(product.getPrice())
                .discountRate(product.getDiscountRate())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .imageUrl(product.getImageUrl())
                .build();
    }
}
