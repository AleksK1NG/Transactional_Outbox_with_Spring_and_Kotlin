package com.alexbryksin.ordersmicroservice.order.exceptions

import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus
import java.util.*

data class SubmitOrderException(val id: UUID?, val status: OrderStatus) : RuntimeException("cannot submit order with id: $id and status: $status") {
}