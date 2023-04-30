package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.*

data class CreateProductItemDTO(
    val id: UUID,
    @field:Size(min = 6, max = 250) val title: String,
    @field:DecimalMin(value = "0.0") val price: BigDecimal,
    @field:Min(value = 0) val quantity: Long = 0,
) {
    fun toProductItem(orderId: UUID): ProductItem = ProductItem(
        id = this.id.toString(),
        title = this.title,
        price = this.price,
        quantity = this.quantity,
        orderId = orderId.toString()
    )
}
