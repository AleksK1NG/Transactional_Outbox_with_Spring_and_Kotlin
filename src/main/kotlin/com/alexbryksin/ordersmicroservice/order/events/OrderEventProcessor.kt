package com.alexbryksin.ordersmicroservice.order.events

interface OrderEventProcessor {
    suspend fun on(orderCreatedEvent: OrderCreatedEvent)
    suspend fun on(productItemAddedEvent: ProductItemAddedEvent)
    suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent)
    suspend fun on(orderPaidEvent: OrderPaidEvent)
    suspend fun on(orderCancelledEvent: OrderCancelledEvent)
    suspend fun on(orderSubmittedEvent: OrderSubmittedEvent)
    suspend fun on(orderCompletedEvent: OrderCompletedEvent)
}