package com.eventpurchase.project.domain.product.entity;

import com.eventpurchase.project.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;


@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "products")
public class ProductEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String productName;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String category;

    @Column
    private String subCategory;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal discountRate;
    private BigDecimal rating;
    private Integer reviewCount;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    private String source;


}
