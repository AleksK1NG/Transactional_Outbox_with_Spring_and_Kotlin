package com.alexbryksin.ordersmicroservice.order.exceptions

data class OrderNotPaidException(val id: String) : RuntimeException("order with id: $id not paid")
