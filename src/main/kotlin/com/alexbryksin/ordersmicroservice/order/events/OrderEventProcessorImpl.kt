package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.exceptions.AlreadyProcessedVersionException
import com.alexbryksin.ordersmicroservice.order.exceptions.InvalidVersionException
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

    override suspend fun on(orderCreatedEvent: OrderCreatedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CREATED_EVENT, or) { observation ->
        orderMongoRepository.insert(orderCreatedEvent.order).also {
            log.info("created order: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_ADDED_EVENT, or) { observation ->
            val order = orderMongoRepository.getByID(productItemAddedEvent.orderId)
            validateVersion(order.id, order.version, productItemAddedEvent.version)

            order.addProductItem(productItemAddedEvent.productItem)
            order.version = productItemAddedEvent.version

            orderMongoRepository.update(order).also {
                log.info("productItemAddedEvent updatedOrder: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
        }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_REMOVED_EVENT, or) { observation ->
            val order = orderMongoRepository.getByID(productItemRemovedEvent.orderId)
            validateVersion(order.id, order.version, productItemRemovedEvent.version)

            order.removeProductItem(productItemRemovedEvent.productItemId)
            order.version = productItemRemovedEvent.version

            orderMongoRepository.update(order).also {
                log.info("productItemRemovedEvent updatedOrder: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
        }

    override suspend fun on(orderPaidEvent: OrderPaidEvent): Unit = coroutineScopeWithObservation(ON_ORDER_PAID_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderPaidEvent.orderId)
        validateVersion(order.id, order.version, orderPaidEvent.version)

        order.pay(orderPaidEvent.paymentId)
        order.version = orderPaidEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderPaidEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CANCELLED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderCancelledEvent.orderId)
        validateVersion(order.id, order.version, orderCancelledEvent.version)

        order.cancel()
        order.version = orderCancelledEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderCancelledEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_SUBMITTED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderSubmittedEvent.orderId)
        validateVersion(order.id, order.version, orderSubmittedEvent.version)

        order.submit()
        order.version = orderSubmittedEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderSubmittedEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_COMPLETED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderCompletedEvent.orderId)
        validateVersion(order.id, order.version, orderCompletedEvent.version)

        order.complete()
        order.version = orderCompletedEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderCompletedEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    private fun validateVersion(id: Any, currentDomainVersion: Long, eventVersion: Long) {
        log.info("validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
        if (currentDomainVersion >= eventVersion) {
            log.warn("currentDomainVersion >= eventVersion validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
            throw AlreadyProcessedVersionException(id, eventVersion)
        }
        if ((currentDomainVersion + 1) < eventVersion) {
            log.warn("currentDomainVersion + 1) < eventVersion validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
            throw InvalidVersionException(eventVersion)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventProcessorImpl::class.java)

        private const val ON_ORDER_COMPLETED_EVENT = "OrderEventProcessor.OrderCompletedEvent"
        private const val ON_ORDER_SUBMITTED_EVENT = "OrderEventProcessor.OrderSubmittedEvent"
        private const val ON_ORDER_CANCELLED_EVENT = "OrderEventProcessor.OrderCancelledEvent"
        private const val ON_ORDER_PAID_EVENT = "OrderEventProcessor.OrderPaidEvent"
        private const val ON_ORDER_PRODUCT_REMOVED_EVENT = "OrderEventProcessor.ProductItemRemovedEvent"
        private const val ON_ORDER_PRODUCT_ADDED_EVENT = "OrderEventProcessor.ProductItemAddedEvent"
        private const val ON_ORDER_CREATED_EVENT = "OrderEventProcessor.OrderCreatedEvent"
    }
}