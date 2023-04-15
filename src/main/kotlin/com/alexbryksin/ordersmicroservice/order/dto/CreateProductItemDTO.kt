package com.alexbryksin.ordersmicroservice.order.dto

import java.math.BigDecimal
import java.util.*

data class CreateProductItemDTO(
    val id: UUID,
    val title: String,
    val price: BigDecimal,
    var quantity: Long = 0,
)
