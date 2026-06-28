CREATE TABLE products
(
    id            BIGSERIAL     NOT NULL PRIMARY KEY,
    product_name  VARCHAR(500)  NOT NULL,
    brand         VARCHAR(200)  NOT NULL,
    category      VARCHAR(100)  NOT NULL,
    sub_category  VARCHAR(100),
    price         NUMERIC(15,2) NOT NULL,
    discount_rate NUMERIC(5,2),
    rating        NUMERIC(3,2),
    review_count  INT,
    image_url     VARCHAR(1000),
    source_url    VARCHAR(1000),
    source        VARCHAR(100),
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6)  NOT NULL,

    CONSTRAINT chk_products_price_positive CHECK (price >= 0),
    CONSTRAINT chk_products_discount_rate  CHECK (discount_rate IS NULL OR (discount_rate >= 0 AND discount_rate <= 100)),
    CONSTRAINT chk_products_rating         CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5))
);

CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_brand    ON products (brand);
CREATE INDEX idx_products_source   ON products (source);
