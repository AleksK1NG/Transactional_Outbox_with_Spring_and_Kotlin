package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.repository.OrderMongoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class OrderEventProcessorImpl(private val orderMongoRepository: OrderMongoRepository) : OrderEventProcessor {
    override suspend fun on(orderCreatedEvent: OrderCreatedEvent) {
        orderMongoRepository.insert(orderCreatedEvent.order)
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent) {
        val order = orderMongoRepository.getByID(productItemAddedEvent.orderId)
        order.addProductItem(productItemAddedEvent.productItem)
    }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun on(orderPaidEvent: OrderPaidEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent) {
        TODO("Not yet implemented")
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventProcessorImpl::class.java)
    }
}