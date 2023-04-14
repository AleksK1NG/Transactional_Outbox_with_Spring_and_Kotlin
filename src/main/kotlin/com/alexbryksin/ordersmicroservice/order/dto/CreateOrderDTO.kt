package com.alexbryksin.ordersmicroservice.order.dto

data class CreateOrderDTO(
    val email: String,
    val address: String,
    var productItems: MutableList<CreateProductItemDTO> = arrayListOf(),
) {
}