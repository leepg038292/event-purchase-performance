package com.eventpurchase.project.global.init;

import com.eventpurchase.project.domain.product.entity.ProductEntity;
import com.eventpurchase.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("이미 데이터 존재, 스킵");
            return;
        }

        String filePath = "C:\\Users\\leepg\\OneDrive\\바탕 화면\\2026\\멋쟁이사자_백엔드\\event-purchase-performance\\data\\fashion_products_all_final.csv";

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), Charset.forName("EUC-KR")))) {

            br.readLine(); // 헤더 스킵

            List<ProductEntity> products = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                try {
                    String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    String productName = fields[0].replaceAll("\"", "").trim();
                    String brand = fields[1].replaceAll("\"", "").trim();
                    String category = fields[2].replaceAll("\"", "").trim();
                    BigDecimal price = fields[3].isEmpty() ? null : new BigDecimal(fields[3].trim());
                    BigDecimal discountRate = fields[4].isEmpty() ? null : new BigDecimal(fields[4].trim());
                    BigDecimal rating = fields[5].isEmpty() ? null : new BigDecimal(fields[5].trim());
                    Integer reviewCount = fields[6].isEmpty() ? null : (int) Double.parseDouble(fields[6].trim());
                    String imageUrl = fields[7].replaceAll("\"", "").trim();
                    String source = fields[8].replaceAll("\"", "").trim();
                    String sourceUrl = fields[9].replaceAll("\"", "").trim();
                    String subCategory = fields.length > 10 ? fields[10].replaceAll("\"", "").trim() : null;

                    products.add(ProductEntity.builder()
                            .productName(productName)
                            .brand(brand)
                            .category(category)
                            .price(price)
                            .discountRate(discountRate)
                            .rating(rating)
                            .reviewCount(reviewCount)
                            .imageUrl(imageUrl)
                            .source(source)
                            .sourceUrl(sourceUrl)
                            .subCategory(subCategory)
                            .build());

                } catch (Exception e) {
                    log.warn("파싱 실패 라인 스킵: {}", line);
                }
            }

            productRepository.saveAll(products);
            log.info("데이터 {}개 저장 완료", products.size());

        } catch (Exception e) {
            log.error("DataLoader 실패: {}", e.getMessage());
        }
    }
}