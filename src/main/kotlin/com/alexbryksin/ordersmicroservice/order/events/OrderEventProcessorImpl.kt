package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.repository.OrderMongoRepository
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class OrderEventProcessorImpl(
    private val orderMongoRepository: OrderMongoRepository,
    private val or: ObservationRegistry,
) : OrderEventProcessor {

    override suspend fun on(orderCreatedEvent: OrderCreatedEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.OrderCreatedEvent", or) {
        orderMongoRepository.insert(orderCreatedEvent.order).also { log.info("created order: $it") }
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.ProductItemAddedEvent", or) {
        orderMongoRepository.getByID(productItemAddedEvent.orderId).let {
            it.addProductItem(productItemAddedEvent.productItem)
            it.version = productItemAddedEvent.version

            orderMongoRepository.update(it).also { order -> log.info("productItemAddedEvent updatedOrder: $order") }
        }
    }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.ProductItemRemovedEvent", or) {
        orderMongoRepository.getByID(productItemRemovedEvent.orderId).let {
            it.removeProductItem(productItemRemovedEvent.productItemId)
            it.version = productItemRemovedEvent.version

            orderMongoRepository.update(it).also { order -> log.info("productItemRemovedEvent updatedOrder: $order") }
        }
    }

    override suspend fun on(orderPaidEvent: OrderPaidEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.OrderPaidEvent", or) {
        orderMongoRepository.getByID(orderPaidEvent.orderId).let {
            it.pay(orderPaidEvent.paymentId)
            it.version = orderPaidEvent.version

            orderMongoRepository.update(it).also { order -> log.info("orderPaidEvent updatedOrder: $order") }
        }
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.OrderCancelledEvent", or) {
        orderMongoRepository.getByID(orderCancelledEvent.orderId).let {
            it.cancel()
            it.version = orderCancelledEvent.version

            orderMongoRepository.update(it).also { order -> log.info("orderCancelledEvent updatedOrder: $order") }
        }
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.OrderSubmittedEvent", or) {
        orderMongoRepository.getByID(orderSubmittedEvent.orderId).let {
            it.submit()
            it.version = orderSubmittedEvent.version

            orderMongoRepository.update(it).also { order -> log.info("orderSubmittedEvent updatedOrder: $order") }
        }
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent): Unit = coroutineScopeWithObservation("OrderEventProcessor.OrderCompletedEvent", or) {
        orderMongoRepository.getByID(orderCompletedEvent.orderId).let {
            it.complete()
            it.version = orderCompletedEvent.version

            orderMongoRepository.update(it).also { order -> log.info("orderCompletedEvent updatedOrder: $order") }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventProcessorImpl::class.java)
    }
}