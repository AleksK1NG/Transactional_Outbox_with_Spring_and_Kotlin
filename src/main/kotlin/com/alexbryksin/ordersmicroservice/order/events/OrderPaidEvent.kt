package com.alexbryksin.ordersmicroservice.order.events

data class OrderPaidEvent(val orderId: String, val paymentId: String) {
    companion object {
        const val ORDER_PAID_EVENT = "ORDER_PAID"
    }
}
