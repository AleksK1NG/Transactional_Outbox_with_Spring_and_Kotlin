package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order


data class OrderCancelledEvent(val orderId: String, override val version: Long, val reason: String? = "") : BaseEvent(orderId, version) {
    companion object {
        const val ORDER_CANCELLED_EVENT = "ORDER_CANCELLED_EVENT"
    }
}

fun OrderCancelledEvent.Companion.of(order: Order, reason: String? = ""): OrderCancelledEvent = OrderCancelledEvent(
    orderId = order.id,
    version = order.version,
    reason = reason
)