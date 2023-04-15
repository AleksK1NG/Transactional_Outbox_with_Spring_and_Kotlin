package com.alexbryksin.ordersmicroservice.order.exceptions

import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus
import java.util.*

data class CompleteOrderException(val id: UUID?, val status: OrderStatus) : RuntimeException("cant complete order with id: $id and status: $status")
