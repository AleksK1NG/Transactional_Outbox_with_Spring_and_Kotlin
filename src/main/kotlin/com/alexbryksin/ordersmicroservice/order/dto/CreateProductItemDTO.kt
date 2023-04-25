package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import java.math.BigDecimal
import java.util.*

data class CreateProductItemDTO(
    val id: UUID,
    val title: String,
    val price: BigDecimal,
    var quantity: Long = 0,
) {
    fun toProductItem(orderId: UUID): ProductItem = ProductItem(
        id = this.id.toString(),
        title = this.title,
        price = this.price,
        quantity = this.quantity,
        orderId = orderId.toString()
    )
}
