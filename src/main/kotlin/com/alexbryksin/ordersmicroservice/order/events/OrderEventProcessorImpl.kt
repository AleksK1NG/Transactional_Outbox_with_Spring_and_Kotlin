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

    override suspend fun on(orderCreatedEvent: OrderCreatedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CREATED_EVENT, or) { observation ->
        orderMongoRepository.insert(orderCreatedEvent.order)
            .also {
                log.info("created order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_ADDED_EVENT, or) { observation ->
            orderMongoRepository.getByID(productItemAddedEvent.orderId).let {
                it.addProductItem(productItemAddedEvent.productItem)
                it.version = productItemAddedEvent.version

                orderMongoRepository.update(it).also { order ->
                    log.info("productItemAddedEvent updatedOrder: $order")
                    observation.highCardinalityKeyValue("order", order.toString())
                }
            }
        }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_REMOVED_EVENT, or) { observation ->
            orderMongoRepository.getByID(productItemRemovedEvent.orderId).let {
                it.removeProductItem(productItemRemovedEvent.productItemId)
                it.version = productItemRemovedEvent.version

                orderMongoRepository.update(it).also { order ->
                    log.info("productItemRemovedEvent updatedOrder: $order")
                    observation.highCardinalityKeyValue("order", order.toString())
                }
            }
        }

    override suspend fun on(orderPaidEvent: OrderPaidEvent): Unit = coroutineScopeWithObservation(ON_ORDER_PAID_EVENT, or) { observation ->
        orderMongoRepository.getByID(orderPaidEvent.orderId).let {
            it.pay(orderPaidEvent.paymentId)
            it.version = orderPaidEvent.version

            orderMongoRepository.update(it).also { order ->
                log.info("orderPaidEvent updatedOrder: $order")
                observation.highCardinalityKeyValue("order", order.toString())
            }
        }
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CANCELLED_EVENT, or) { observation ->
        orderMongoRepository.getByID(orderCancelledEvent.orderId).let {
            it.cancel()
            it.version = orderCancelledEvent.version

            orderMongoRepository.update(it).also { order ->
                log.info("orderCancelledEvent updatedOrder: $order")
                observation.highCardinalityKeyValue("order", order.toString())
            }
        }
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_SUBMITTED_EVENT, or) { observation ->
        orderMongoRepository.getByID(orderSubmittedEvent.orderId).let {
            it.submit()
            it.version = orderSubmittedEvent.version

            orderMongoRepository.update(it).also { order ->
                log.info("orderSubmittedEvent updatedOrder: $order")
                observation.highCardinalityKeyValue("order", order.toString())
            }
        }
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_COMPLETED_EVENT, or) { observation ->
        orderMongoRepository.getByID(orderCompletedEvent.orderId).let {
            it.complete()
            it.version = orderCompletedEvent.version

            orderMongoRepository.update(it).also { order ->
                log.info("orderCompletedEvent updatedOrder: $order")
                observation.highCardinalityKeyValue("order", order.toString())
            }
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