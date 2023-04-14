package com.alexbryksin.ordersmicroservice.order.dto

import java.math.BigDecimal

data class CreateProductItemDTO(
    val title: String,
    val price: BigDecimal,
    var quantity: Long = 0,
)
