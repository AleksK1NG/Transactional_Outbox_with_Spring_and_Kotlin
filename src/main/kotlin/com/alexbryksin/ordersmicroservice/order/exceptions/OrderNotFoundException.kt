package com.alexbryksin.ordersmicroservice.order.exceptions

import java.util.*

data class OrderNotFoundException(val orderId: UUID?) : RuntimeException("order with id: $orderId not found")
