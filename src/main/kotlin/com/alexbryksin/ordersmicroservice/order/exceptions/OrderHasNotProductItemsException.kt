package com.alexbryksin.ordersmicroservice.order.exceptions

import java.util.*

data class OrderHasNotProductItemsException(val orderId: UUID?): RuntimeException("order with id: $orderId has not products")
