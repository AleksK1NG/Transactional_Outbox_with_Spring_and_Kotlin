package com.alexbryksin.ordersmicroservice.order.exceptions

data class OrderHasNotProductItemsException(val orderId: String): RuntimeException("order with id: $orderId has not products")