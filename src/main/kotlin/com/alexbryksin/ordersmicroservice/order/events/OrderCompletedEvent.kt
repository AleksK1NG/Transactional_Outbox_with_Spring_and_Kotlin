package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order

data class OrderCompletedEvent(val orderId: String, override val version: Long) : BaseEvent(orderId, version) {
    companion object {
        const val ORDER_COMPLETED_EVENT = "ORDER_COMPLETED"
    }
}

fun OrderCompletedEvent.Companion.of(order: Order): OrderCompletedEvent = OrderCompletedEvent(
    orderId = order.id.toString(),
    version = order.version
)