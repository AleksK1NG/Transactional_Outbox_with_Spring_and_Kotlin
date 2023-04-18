package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order

data class OrderSubmittedEvent(val orderId: String, override val version: Long) : BaseEvent(orderId, version) {
    companion object {
        const val ORDER_SUBMITTED_EVENT = "ORDER_SUBMITTED"
    }
}

fun OrderSubmittedEvent.Companion.of(order: Order): OrderSubmittedEvent = OrderSubmittedEvent(
    orderId = order.id.toString(),
    version = order.version
)