package com.eventpurchase.project.cart

class ProductNotAvailableException(productId: Long) : RuntimeException("Product not available: $productId")
