package com.alexbryksin.ordersmicroservice.order.exceptions

import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus

data class SubmitOrderException(val id: String, val status: OrderStatus) : RuntimeException("cannot submit order with id: $id and status: $status") {
}