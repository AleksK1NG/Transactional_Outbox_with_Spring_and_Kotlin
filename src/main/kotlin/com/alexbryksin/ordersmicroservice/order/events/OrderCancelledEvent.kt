package com.alexbryksin.ordersmicroservice.order.events

data class OrderCancelledEvent(val orderId: String, val reason: String? = ""): BaseEvent {
    companion object {
        const val ORDER_CANCELLED_EVENT = "ORDER_CANCELLED_EVENT"
    }
}