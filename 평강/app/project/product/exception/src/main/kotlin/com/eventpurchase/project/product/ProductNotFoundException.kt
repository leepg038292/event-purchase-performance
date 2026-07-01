package com.eventpurchase.project.product

class ProductNotFoundException(productId: Long) : RuntimeException("Product not found: $productId")
