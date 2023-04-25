package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order

data class OrderPaidEvent(val orderId: String, override val version: Long, val paymentId: String) : BaseEvent(orderId, version) {
    companion object {
        const val ORDER_PAID_EVENT = "ORDER_PAID"
    }
}

fun OrderPaidEvent.Companion.of(order: Order, paymentId: String): OrderPaidEvent = OrderPaidEvent(
    orderId = order.id,
    version =  order.version,
    paymentId = paymentId
)