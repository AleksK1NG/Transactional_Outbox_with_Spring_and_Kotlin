package com.alexbryksin.ordersmicroservice.order.dto

import java.util.*

data class OrderItemProjection(val id: UUID, val orderId: UUID, val title: String) {
}