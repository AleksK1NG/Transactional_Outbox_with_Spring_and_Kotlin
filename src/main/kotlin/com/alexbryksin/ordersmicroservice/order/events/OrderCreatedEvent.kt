package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order

data class OrderCreatedEvent(val order: Order) {
    companion object {
        const val ORDER_CREATED_EVENT = "ORDER_CREATED"
    }
}
