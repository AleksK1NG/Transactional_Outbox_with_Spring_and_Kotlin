package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateOrderDTO(
    @field:NotBlank @field:Size(min = 6, max = 250) val email: String,
    @field:NotBlank @field:Size(min = 10, max = 1000) val address: String,
    @field:Valid var productItems: MutableList<CreateProductItemDTO> = arrayListOf(),
) {
    companion object

    fun toOrder(): Order = Order(
        email = this.email,
        address = this.address,
        productItems = this.productItems.map { ProductItem(title = it.title, price = it.price, quantity = it.quantity, id = it.id.toString()) }
            .associateBy { it.id }.toMutableMap()
    )
}

