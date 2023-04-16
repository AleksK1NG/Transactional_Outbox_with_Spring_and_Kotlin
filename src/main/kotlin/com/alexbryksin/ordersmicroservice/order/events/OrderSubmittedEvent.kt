package com.alexbryksin.ordersmicroservice.order.events

data class OrderSubmittedEvent(val orderId: String): BaseEvent {
    companion object {
        const val ORDER_SUBMITTED_EVENT = "ORDER_SUBMITTED"
    }
}