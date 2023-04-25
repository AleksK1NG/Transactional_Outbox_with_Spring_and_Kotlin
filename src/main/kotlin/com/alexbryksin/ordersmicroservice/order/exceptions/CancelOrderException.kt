package com.alexbryksin.ordersmicroservice.order.exceptions

import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus

data class CancelOrderException(val id: String, val status: OrderStatus) : RuntimeException("cant cancel order with id: $id and status: $status")
