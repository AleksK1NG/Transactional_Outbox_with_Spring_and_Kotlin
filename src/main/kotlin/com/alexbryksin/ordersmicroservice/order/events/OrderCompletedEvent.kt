package com.alexbryksin.ordersmicroservice.order.events

data class OrderCompletedEvent(val orderId: String) {
    companion object {
        const val ORDER_COMPLETED_EVENT = "ORDER_COMPLETED"
    }
}
