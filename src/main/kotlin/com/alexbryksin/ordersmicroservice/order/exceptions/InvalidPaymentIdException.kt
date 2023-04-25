package com.alexbryksin.ordersmicroservice.order.exceptions

data class InvalidPaymentIdException(val orderId: String, val paymentId: String) :
    RuntimeException("invalid payment id: $paymentId for order with id: $orderId")
