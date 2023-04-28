package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.UnknownEventTypeException
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.order.domain.*
import com.alexbryksin.ordersmicroservice.order.events.*
import com.alexbryksin.ordersmicroservice.order.events.OrderCancelledEvent.Companion.ORDER_CANCELLED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCompletedEvent.Companion.ORDER_COMPLETED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCreatedEvent.Companion.ORDER_CREATED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderPaidEvent.Companion.ORDER_PAID_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderSubmittedEvent.Companion.ORDER_SUBMITTED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemAddedEvent.Companion.PRODUCT_ITEM_ADDED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemRemovedEvent.Companion.PRODUCT_ITEM_REMOVED_EVENT
import com.alexbryksin.ordersmicroservice.order.repository.OrderMongoRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderOutboxRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*


@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productItemRepository: ProductItemRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val orderMongoRepository: OrderMongoRepository,
    private val serializer: Serializer,
    private val txOp: TransactionalOperator,
    private val eventsPublisher: EventsPublisher,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val or: ObservationRegistry
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScopeWithObservation("OrderService.createOrder", or) {
        txOp.executeAndAwait {
            orderRepository.insert(order).let {
                val productItemsEntityList = ProductItemEntity.listOf(order.productItems, UUID.fromString(it.id))
                val insertedItems = productItemRepository.insertAll(productItemsEntityList).toList()

                it.addProductItems(insertedItems.map { item -> item.toProductItem() })

                Pair(it, outboxRepository.save(orderCreatedEventOf(it)))
            }
        }.run {
            publish(second)
            first
        }
    }


    override suspend fun addProductItem(productItem: ProductItem): Unit = coroutineScopeWithObservation("OrderService.addProductItem", or) {
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(UUID.fromString(productItem.orderId))
            order.incVersion()

            val productItemEntity = productItemRepository.insert(ProductItemEntity.of(productItem))

            val savedRecord = outboxRepository.save(productItemAddedEventOf(order, productItemEntity))

            orderRepository.updateOrderVersion(UUID.fromString(order.id), order.version)
                .also { result -> log.info("addOrderItem result: $result, version: ${order.version}") }

            savedRecord
        }.run { publish(this) }
    }

    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScopeWithObservation("OrderService.removeProductItem", or) {
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(orderID)
            productItemRepository.deleteById(productItemId)

            order.incVersion()

            val savedRecord = outboxRepository.save(productItemRemovedEventOf(order, productItemId))

            orderRepository.updateOrderVersion(UUID.fromString(order.id), order.version)
                .also { log.info("removeProductItem update order result: $it, version: ${order.version}") }

            savedRecord
        }.run { publish(this) }
    }

    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScopeWithObservation("OrderService.pay", or) {
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.pay(paymentId)


            val updatedOrder = orderRepository.update(order)
            Pair(updatedOrder, outboxRepository.save(orderPaidEventOf(updatedOrder, paymentId)))
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScopeWithObservation("OrderService.cancel", or) {
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.cancel()

            orderRepository.update(order).let { Pair(it, outboxRepository.save(orderCancelledEventOf(it, reason))) }
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun submit(id: UUID): Order = coroutineScopeWithObservation("OrderService.submit", or) {
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.submit()

            val updatedOrder = orderRepository.update(order)
            updatedOrder.addProductItems(order.productItems)

            Pair(updatedOrder, outboxRepository.save(orderSubmittedEventOf(updatedOrder)))
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun complete(id: UUID): Order = coroutineScopeWithObservation("OrderService.complete", or) {
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.complete()

            val updatedOrder = orderRepository.update(order)
            log.info("order submitted: ${updatedOrder.status} for id: $id")
            Pair(updatedOrder, outboxRepository.save(orderCompletedEventOf(updatedOrder)))
        }.run {
            publish(second)
            first
        }
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScopeWithObservation("OrderService.getOrderWithProductItemsByID", or) {
        orderRepository.getOrderWithProductItemsByID(id)
    }

    @Transactional(readOnly = true)
    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScopeWithObservation("OrderService.getAllOrders", or) {
        orderMongoRepository.getAllOrders(pageable)
    }

    override suspend fun deleteOutboxRecordsWithLock() =
        outboxRepository.deleteOutboxRecordsWithLock { eventsPublisher.publish(getTopicName(it.eventType), it) }


    override suspend fun getOrderByID(id: UUID): Order = coroutineScope {
        orderMongoRepository.getByID(id.toString()).also { log.info("getOrderByID: $it") }
    }

    private suspend fun publish(event: OutboxRecord) = withContext(Dispatchers.IO) {
        try {
            log.info("publishing outbox event >>>>>: $event")
            outboxRepository.deleteOutboxRecordByID(event.eventId!!) {
                eventsPublisher.publish(getTopicName(event.eventType), event.aggregateId.toString(), event)
            }
            log.info("outbox event published and deleted >>>>>>: $event")
        } catch (ex: Exception) {
            log.error("exception while publishing outbox event: ${ex.localizedMessage}")
        }
    }


    private fun getTopicName(eventType: String?) = when (eventType) {
        ORDER_CREATED_EVENT -> kafkaTopicsConfiguration.orderCreated?.name
        PRODUCT_ITEM_ADDED_EVENT -> kafkaTopicsConfiguration.productAdded?.name
        PRODUCT_ITEM_REMOVED_EVENT -> kafkaTopicsConfiguration.productRemoved?.name
        ORDER_CANCELLED_EVENT -> kafkaTopicsConfiguration.orderCancelled?.name
        ORDER_PAID_EVENT -> kafkaTopicsConfiguration.orderPaid?.name
        ORDER_SUBMITTED_EVENT -> kafkaTopicsConfiguration.orderSubmitted?.name
        ORDER_COMPLETED_EVENT -> kafkaTopicsConfiguration.orderCompleted?.name
        else -> throw UnknownEventTypeException(eventType)
    }


    private fun orderCreatedEventOf(order: Order) = outboxRecord(order.id, order.version, OrderCreatedEvent(order), ORDER_CREATED_EVENT)

    private fun productItemAddedEventOf(order: Order, productItemEntity: ProductItemEntity) = outboxRecord(
        order.id,
        order.version,
        ProductItemAddedEvent.of(order, productItemEntity.toProductItem()),
        PRODUCT_ITEM_ADDED_EVENT,
    )

    private fun productItemRemovedEventOf(order: Order, productItemId: UUID) = outboxRecord(
        order.id,
        order.version,
        ProductItemRemovedEvent.of(order, productItemId),
        PRODUCT_ITEM_REMOVED_EVENT
    )

    private fun orderPaidEventOf(order: Order, paymentId: String) = outboxRecord(
        order.id,
        order.version,
        OrderPaidEvent.of(order, paymentId),
        ORDER_PAID_EVENT
    )

    private fun orderCancelledEventOf(order: Order, reason: String?) = outboxRecord(
        order.id,
        order.version,
        OrderCancelledEvent.of(order, reason),
        ORDER_CANCELLED_EVENT,
    )

    private fun orderSubmittedEventOf(order: Order) = outboxRecord(
        order.id,
        order.version,
        OrderSubmittedEvent.of(order),
        ORDER_SUBMITTED_EVENT
    )

    private fun orderCompletedEventOf(order: Order) = outboxRecord(
        order.id,
        order.version,
        OrderCompletedEvent.of(order),
        ORDER_COMPLETED_EVENT
    )

    private fun outboxRecord(aggregateId: String, version: Long, data: Any, eventType: String): OutboxRecord =
        OutboxRecord(
            eventId = null,
            aggregateId = aggregateId,
            eventType = eventType,
            data = serializer.serializeToBytes(data),
            version = version,
            timestamp = LocalDateTime.now()
        )

    companion object {
        private val log = LoggerFactory.getLogger(OrderServiceImpl::class.java)
    }
}