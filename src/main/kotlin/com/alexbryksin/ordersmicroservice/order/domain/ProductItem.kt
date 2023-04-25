package com.alexbryksin.ordersmicroservice.order.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductItem(
    val id: String = "",
    val orderId: String = "",
    val title: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val quantity: Long = 0,
    val version: Long = 0,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
