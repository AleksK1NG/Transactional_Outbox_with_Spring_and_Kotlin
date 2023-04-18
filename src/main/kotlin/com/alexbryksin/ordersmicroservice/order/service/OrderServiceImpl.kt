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
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
import com.alexbryksin.ordersmicroservice.order.repository.OrderOutboxRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
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
    private val serializer: Serializer,
    private val txOp: TransactionalOperator,
    private val eventsPublisher: EventsPublisher,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScope {
        val resultPair = txOp.executeAndAwait {
            orderRepository.save(OrderEntity.of(order)).let {
                val productItemsList = order.productItemEntities.map { item -> ProductItemEntity.of(item.copy(orderId = it.id)) }
                val insertedItems = productItemRepository.insertAll(productItemsList).toList()

                val record = outboxRecord(it.id.toString(), it.version, OrderCreatedEvent(it.toOrder()), ORDER_CREATED_EVENT)
                outboxRepository.save(record).also { savedRecord -> log.info("saved outbox record: $savedRecord") }
                Pair(it.toOrder().addProductItems(insertedItems.map { item -> item.toProductItem() }), record)
            }
        }

        publish(resultPair.second)
        resultPair.first
    }

    override suspend fun addOrderItem(productItemEntity: ProductItemEntity): Unit = coroutineScope {
        val record = txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(productItemEntity.orderId).incVersion()

            productItemRepository.insert(productItemEntity).also { log.info("saved product item: $it") }

            val event = outboxRecord(
                order.id.toString(),
                order.version,
                ProductItemAddedEvent.of(order, productItemEntity.toProductItem()),
                PRODUCT_ITEM_ADDED_EVENT
            )
            outboxRepository.save(event).also { log.info("saved outbox record: $it") }

            val result = orderRepository.updateOrderVersion(order.id!!, order.version)
            log.info("addOrderItem result: $result, version: ${order.version}")
            event
        }

        publish(record)
    }

    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScope {
        val record = txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(orderID).incVersion()

            productItemRepository.deleteById(productItemId).also { log.info("deleted product item: $productItemId") }

            val record = outboxRecord(
                order.id.toString(),
                order.version,
                ProductItemRemovedEvent.of(order, productItemId),
                PRODUCT_ITEM_REMOVED_EVENT
            )
            outboxRepository.save(record).also { log.info("saved outbox record: $it") }

            val result = orderRepository.updateOrderVersion(order.id!!, order.version)
            log.info("removeProductItem result: $result, version: ${order.version}")
            record
        }

        publish(record)
    }

    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScope {
        val resultPair = txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.pay()
            val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

            val record = outboxRecord(
                savedOrder.id.toString(),
                savedOrder.version,
                OrderPaidEvent.of(savedOrder, paymentId),
                ORDER_PAID_EVENT
            )
            outboxRepository.save(record).also { log.info("saved outbox record: $it") }

            savedOrder.also { log.info("paid order: $it") }
            Pair(savedOrder, record)
        }

        publish(resultPair.second)
        resultPair.first
    }

    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScope {
        val resultPair = txOp.executeAndAwait {
            val orderEntity = orderRepository.findById(id) ?: throw OrderNotFoundException(id)
            val order = orderEntity.toOrder().apply { cancel() }
            val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

            val record = outboxRecord(
                savedOrder.id.toString(),
                savedOrder.version,
                OrderCancelledEvent.of(savedOrder, reason),
                ORDER_CANCELLED_EVENT,
            )
            outboxRepository.save(record).also { log.info("saved outbox record: $it") }

            savedOrder.also { log.info("order cancelled: ${order.status} for id: $id") }
            Pair(savedOrder, record)
        }

        publish(resultPair.second)
        resultPair.first
    }

    override suspend fun submit(id: UUID): Order = coroutineScope {
        val resultPair = txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.submit()
            val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

            val record = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderSubmittedEvent.of(savedOrder), ORDER_SUBMITTED_EVENT)
            outboxRepository.save(record).also { log.info("saved outbox record: $it") }
            Pair(savedOrder.addProductItems(order.productItemEntities), record)
        }

        publish(resultPair.second)
        resultPair.first
    }

    override suspend fun complete(id: UUID): Order = coroutineScope {
        val resultPair = txOp.executeAndAwait {
            val orderEntity = orderRepository.findById(id) ?: throw OrderNotFoundException(id)
            orderEntity.toOrder().let {
                it.complete()
                val savedOrder = orderRepository.save(OrderEntity.of(it)).toOrder()

                val event = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderCompletedEvent.of(savedOrder), ORDER_COMPLETED_EVENT)
                outboxRepository.save(event).also { record -> log.info("saved outbox record: $record") }
                log.info("order submitted: ${savedOrder.status} for id: $id")
                Pair(savedOrder, event)
            }

        }

        publish(resultPair.second)
        resultPair.first
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {
        orderRepository.getOrderWithProductItemsByID(id)
    }

    @Transactional(readOnly = true)
    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }


    override suspend fun deleteOutboxRecordsWithLock() =
        outboxRepository.deleteOutboxRecordsWithLock { eventsPublisher.publish(getTopicName(it.eventType), it) }


    override suspend fun getOrderByID(id: UUID): Order = coroutineScope {
        orderRepository.findById(id)?.toOrder() ?: throw OrderNotFoundException(id)
    }


    private suspend fun publish(event: OutboxRecord) = withContext(Dispatchers.IO) {
        try {
            log.info("publishing event >>>>>: $event")
            outboxRepository.deleteOutboxRecordByID(event.eventId!!) {
                eventsPublisher.publish(getTopicName(event.eventType), event.aggregateId.toString(), event)
            }
            log.info("event published and deleted >>>>>>: $event")
        } catch (ex: Exception) {
            log.error("exception while publishing outbox event: ${ex.localizedMessage}")
        }
    }


    private fun getTopicName(eventType: String?) = when (eventType) {
        ORDER_CREATED_EVENT -> kafkaTopicsConfiguration.orderCreated?.name
        PRODUCT_ITEM_ADDED_EVENT -> kafkaTopicsConfiguration.productAdded?.name
        PRODUCT_ITEM_REMOVED_EVENT -> kafkaTopicsConfiguration.productRemoved?.name
        ORDER_CANCELLED_EVENT -> kafkaTopicsConfiguration.orderCancelled?.name
        ORDER_PAID_EVENT -> kafkaTopicsConfiguration.orderCancelled?.name
        ORDER_SUBMITTED_EVENT -> kafkaTopicsConfiguration.orderSubmitted?.name
        ORDER_COMPLETED_EVENT -> kafkaTopicsConfiguration.orderCompleted?.name
        else -> throw UnknownEventTypeException(eventType)
    }


    private fun outboxRecord(aggregateId: String, version: Long, data: Any, eventType: String): OutboxRecord = OutboxRecord(
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