package com.alexbryksin.ordersmicroservice.order.dto

import java.util.*

data class OrderProjection(
    val id: UUID,
    val email: String,
    val items: MutableList<OrderItemProjection> = arrayListOf()
)
