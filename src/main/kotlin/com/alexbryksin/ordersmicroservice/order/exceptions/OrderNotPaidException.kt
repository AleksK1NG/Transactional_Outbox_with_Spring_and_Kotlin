package com.alexbryksin.ordersmicroservice.order.exceptions

import java.util.*

data class OrderNotPaidException(val id: UUID?) : RuntimeException("order with id: $id not paid")
