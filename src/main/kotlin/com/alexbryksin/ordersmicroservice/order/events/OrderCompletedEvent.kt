package com.alexbryksin.ordersmicroservice.order.events

data class OrderCompletedEvent(val orderId: String): BaseEvent {
    companion object {
        const val ORDER_COMPLETED_EVENT = "ORDER_COMPLETED"
    }
}
