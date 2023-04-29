package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.UnknownEventTypeException
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.order.domain.*
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
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.core.publisher.Mono
import java.util.*


@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productItemRepository: ProductItemRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val orderMongoRepository: OrderMongoRepository,
    private val txOp: TransactionalOperator,
    private val eventsPublisher: EventsPublisher,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val or: ObservationRegistry,
    private val outboxEventSerializer: OutboxEventSerializer
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScopeWithObservation(CREATE, or) { observation ->
        txOp.executeAndAwait {
            orderRepository.insert(order).let {
                val productItemsEntityList = ProductItemEntity.listOf(order.productItems, UUID.fromString(it.id))
                val insertedItems = productItemRepository.insertAll(productItemsEntityList).toList()

                it.addProductItems(insertedItems.map { item -> item.toProductItem() })

                Pair(it, outboxRepository.save(outboxEventSerializer.orderCreatedEventOf(it)))
            }
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())
            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun addProductItem(productItem: ProductItem): Unit = coroutineScopeWithObservation(ADD_PRODUCT, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(UUID.fromString(productItem.orderId))
            order.incVersion()

            val productItemEntity = productItemRepository.insert(ProductItemEntity.of(productItem))

            val savedRecord = outboxRepository.save(outboxEventSerializer.productItemAddedEventOf(order, productItemEntity))

            orderRepository.updateVersion(UUID.fromString(order.id), order.version)
                .also { result -> log.info("addOrderItem result: $result, version: ${order.version}") }

            savedRecord
        }.run {
            observation.highCardinalityKeyValue("outboxEvent", this.toString())
            publishOutboxEvent(this)
        }
    }

    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScopeWithObservation(REMOVE_PRODUCT, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(orderID)
            productItemRepository.deleteById(productItemId)

            order.incVersion()

            val savedRecord = outboxRepository.save(outboxEventSerializer.productItemRemovedEventOf(order, productItemId))

            orderRepository.updateVersion(UUID.fromString(order.id), order.version)
                .also { log.info("removeProductItem update order result: $it, version: ${order.version}") }

            savedRecord
        }.run {
            observation.highCardinalityKeyValue("outboxEvent", this.toString())
            publishOutboxEvent(this)
        }
    }

    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScopeWithObservation(PAY, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.pay(paymentId)

            val updatedOrder = orderRepository.update(order)
            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderPaidEventOf(updatedOrder, paymentId)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())
            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScopeWithObservation(CANCEL, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.cancel()

            val updatedOrder = orderRepository.update(order)
            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderCancelledEventOf(updatedOrder, reason)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())
            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun submit(id: UUID): Order = coroutineScopeWithObservation(SUBMIT, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.submit()

            val updatedOrder = orderRepository.update(order)
            updatedOrder.addProductItems(order.productItems)

            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderSubmittedEventOf(updatedOrder)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())
            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun complete(id: UUID): Order = coroutineScopeWithObservation(COMPLETE, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.complete()

            val updatedOrder = orderRepository.update(order)
            log.info("order submitted: ${updatedOrder.status} for id: $id")
            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderCompletedEventOf(updatedOrder)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())
            publishOutboxEvent(second)
            first
        }
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductsByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_WITH_PRODUCTS_BY_ID, or) { observation ->
        orderRepository.getOrderWithProductItemsByID(id).also { observation.highCardinalityKeyValue("order", it.toString()) }
    }

    @Transactional(readOnly = true)
    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScopeWithObservation(GET_ALL_ORDERS, or) { observation ->
        orderMongoRepository.getAllOrders(pageable).also { observation.highCardinalityKeyValue("pageResult", it.toString()) }
    }

    override suspend fun deleteOutboxRecordsWithLock() = coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_WITH_LOCK, or) { observation ->
        outboxRepository.deleteOutboxRecordsWithLock {
            observation.highCardinalityKeyValue("outboxEvent", it.toString())
            eventsPublisher.publish(getTopicName(it.eventType), it)
        }
    }


    override suspend fun getOrderByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_BY_ID, or) { observation ->
        orderMongoRepository.getByID(id.toString())
            .also { log.info("getOrderByID: $it") }
            .also { observation.highCardinalityKeyValue("order", it.toString()) }
    }

    private suspend fun publishOutboxEvent(event: OutboxRecord) = coroutineScopeWithObservation(PUBLISH_OUTBOX_EVENT, or) {observation ->
        try {
            log.info("publishing outbox event >>>>>: $event")

            outboxRepository.deleteOutboxRecordByID(event.eventId!!) {
                eventsPublisher.publish(getTopicName(event.eventType), event.aggregateId.toString(), event)
            }

            log.info("outbox event published and deleted >>>>>>: $event")
            observation.highCardinalityKeyValue("event", event.toString())
        } catch (ex: Exception) {
            log.error("exception while publishing outbox event: ${ex.localizedMessage}")
            observation.error(ex)
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


    companion object {
        private val log = LoggerFactory.getLogger(OrderServiceImpl::class.java)

        private const val PUBLISH_OUTBOX_EVENT = "OrderService.getAllOrders"
        private const val GET_ORDER_BY_ID = "OrderService.getOrderByID"
        private const val DELETE_OUTBOX_RECORD_WITH_LOCK = "OrderService.deleteOutboxRecordsWithLock"
        private const val GET_ALL_ORDERS = "OrderService.getAllOrders"
        private const val GET_ORDER_WITH_PRODUCTS_BY_ID = "OrderService.getOrderWithProductsByID"
        private const val COMPLETE = "OrderService.complete"
        private const val SUBMIT = "OrderService.submit"
        private const val CANCEL = "OrderService.cancel"
        private const val PAY = "OrderService.pay"
        private const val ADD_PRODUCT = "OrderService.addProduct"
        private const val REMOVE_PRODUCT = "OrderService.removeProduct"
        private const val CREATE = "OrderService.create"
    }
}