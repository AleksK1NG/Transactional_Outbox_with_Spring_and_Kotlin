package com.alexbryksin.ordersmicroservice.order.exceptions

import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus

data class CompleteOrderException(val id: String, val status: OrderStatus) : RuntimeException("cant complete order with id: $id and status: $status")
