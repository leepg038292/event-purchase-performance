package com.eventpurchase.project.cart

import com.eventpurchase.project.shared.response.SuccessResponse
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/cart")
class CartController(private val cartService: CartService) {

    @GetMapping("/{userId}")
    fun getCart(@PathVariable userId: Long): SuccessResponse<CartResponse> =
        SuccessResponse.of(200, "장바구니 조회 성공", cartService.getCart(userId).toResponse())

    @PostMapping("/{userId}/items")
    fun addItem(@PathVariable userId: Long, @RequestBody request: CartItemRequest): SuccessResponse<CartResponse> =
        SuccessResponse.of(200, "장바구니 상품 추가 성공", cartService.addItem(userId, request.productId, request.quantity).toResponse())

    @PutMapping("/{userId}/items/{cartItemId}")
    fun updateItem(@PathVariable userId: Long, @PathVariable cartItemId: Long, @RequestBody request: CartItemRequest): SuccessResponse<CartResponse> =
        SuccessResponse.of(200, "장바구니 수량 수정 성공", cartService.updateItemQuantity(userId, cartItemId, request.quantity).toResponse())

    @DeleteMapping("/{userId}/items/{cartItemId}")
    fun deleteItem(@PathVariable userId: Long, @PathVariable cartItemId: Long): SuccessResponse<CartResponse> =
        SuccessResponse.of(200, "장바구니 상품 삭제 성공", cartService.deleteItem(userId, cartItemId).toResponse())

    @DeleteMapping("/{userId}")
    fun clearCart(@PathVariable userId: Long): SuccessResponse<Unit> {
        cartService.clearCart(userId)
        return SuccessResponse.of(200, "장바구니 비우기 성공", Unit)
    }
}

data class CartItemRequest(val productId: Long, val quantity: Int)

data class CartResponse(val cartId: Long?, val userId: Long, val items: List<CartItemResponse>)
data class CartItemResponse(
    val cartItemId: Long?, val quantity: Int, val productId: Long,
    val productName: String, val brand: String, val price: BigDecimal,
    val discountRate: BigDecimal?, val imageUrl: String?
)

private fun CartView.toResponse() = CartResponse(cartId, userId, items.map { it.toResponse() })
private fun CartItemView.toResponse() = CartItemResponse(cartItemId, quantity, product.id, product.productName, product.brand, product.price, product.discountRate, product.imageUrl)
