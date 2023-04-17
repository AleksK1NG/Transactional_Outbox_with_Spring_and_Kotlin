package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem

data class CreateOrderDTO(
    val email: String,
    val address: String,
    var productItems: MutableList<CreateProductItemDTO> = arrayListOf(),
) {
    companion object

    fun toOrder(): Order = Order(
        email = this.email,
        address = this.address,
        productItemEntities = this.productItems.map { ProductItem(title = it.title, price = it.price, quantity = it.quantity, id = it.id) }
            .toMutableList()
    )
}

