package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.repository.OrderMongoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*


@Service
class OrderEventProcessorImpl(private val orderMongoRepository: OrderMongoRepository) : OrderEventProcessor {
    override suspend fun on(orderCreatedEvent: OrderCreatedEvent) = withContext(Dispatchers.IO) {
        val order = orderMongoRepository.insert(orderCreatedEvent.order)
        log.info("created order: $order")
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent)= withContext(Dispatchers.IO) {
        val order = orderMongoRepository.getByID(productItemAddedEvent.orderId)
        order.addProductItem(productItemAddedEvent.productItem)
        order.version = productItemAddedEvent.version
        log.info("productItemAddedEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("productItemAddedEvent updatedOrder: $updatedOrder")
    }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent)= withContext(Dispatchers.IO) {
        val order = orderMongoRepository.getByID(productItemRemovedEvent.orderId)
        order.removeProductItem(UUID.fromString(productItemRemovedEvent.productItemId))
        order.version = productItemRemovedEvent.version
        log.info("productItemRemovedEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("productItemRemovedEvent updatedOrder: $updatedOrder")
    }

    override suspend fun on(orderPaidEvent: OrderPaidEvent)= withContext(Dispatchers.IO) {
        val order = orderMongoRepository.getByID(orderPaidEvent.orderId)
        order.pay()
        order.version = orderPaidEvent.version
        log.info("orderPaidEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("orderPaidEvent updatedOrder: $updatedOrder")
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent) = withContext(Dispatchers.IO){
        val order = orderMongoRepository.getByID(orderCancelledEvent.orderId)
        order.cancel()
        order.version = orderCancelledEvent.version
        log.info("orderCancelledEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("orderCancelledEvent updatedOrder: $updatedOrder")
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent) = withContext(Dispatchers.IO){
        val order = orderMongoRepository.getByID(orderSubmittedEvent.orderId)
        order.submit()
        order.version = orderSubmittedEvent.version
        log.info("orderSubmittedEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("orderSubmittedEvent updatedOrder: $updatedOrder")
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent)= withContext(Dispatchers.IO) {
        val order = orderMongoRepository.getByID(orderCompletedEvent.orderId)
        order.complete()
        order.version = orderCompletedEvent.version
        log.info("orderCompletedEvent order: $order")
        val updatedOrder = orderMongoRepository.update(order)
        log.info("orderCompletedEvent updatedOrder: $updatedOrder")
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventProcessorImpl::class.java)
    }
}