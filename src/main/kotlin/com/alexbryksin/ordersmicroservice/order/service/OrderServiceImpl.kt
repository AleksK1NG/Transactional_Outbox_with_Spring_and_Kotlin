package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.*
import com.alexbryksin.ordersmicroservice.order.events.*
import com.alexbryksin.ordersmicroservice.order.events.OrderCancelledEvent.Companion.ORDER_CANCELLED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCompletedEvent.Companion.ORDER_COMPLETED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCreatedEvent.Companion.ORDER_CREATED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderPaidEvent.Companion.ORDER_PAID_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderSubmittedEvent.Companion.ORDER_SUBMITTED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemAddedEvent.Companion.PRODUCT_ITEM_ADDED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemRemovedEvent.Companion.PRODUCT_ITEM_REMOVED_EVENT
import com.alexbryksin.ordersmicroservice.order.repository.OrderOutboxRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
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
    private val entityTemplate: R2dbcEntityTemplate,
    private val txOp: TransactionalOperator,
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScope {
        txOp.executeAndAwait {
            val savedOrder = orderRepository.save(OrderEntity.of(order))
            if (order.productItems.isNotEmpty()) {
                order.productItems.forEach { it.orderId = savedOrder.id }
                productItemRepository.insertAll(order.productItems).toList().also { log.info("savedOrderItems: $it") }
            }

            val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderCreatedEvent(order), ORDER_CREATED_EVENT)
            outboxRepository.save(event).also { log.info("saved outbox record: $it") }
            savedOrder.toOrder().also { log.info("saved order: $it") }
        }
    }

    @Transactional
    override suspend fun addOrderItem(productItem: ProductItem): Unit = coroutineScope {
        val order = orderRepository.findById(productItem.orderId!!) ?: throw RuntimeException("order with id: ${productItem.orderId} not found")
        productItemRepository.insert(productItem).also { log.info("saved product item: $it") }

        val event = outboxEvent(order.id.toString(), order.version, ProductItemAddedEvent(order.id.toString(), productItem), PRODUCT_ITEM_ADDED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        log.info("addOrderItem result: $result, version: ${order.version + 1}")
    }

    @Transactional
    override suspend fun removeProductItem(productItem: ProductItem): Unit = coroutineScope {
        val order = orderRepository.findById(productItem.orderId!!) ?: throw RuntimeException("order with id: ${productItem.orderId} not found")
        productItemRepository.deleteById(productItem.id!!).also { log.info("deleted product item: ${productItem.id}") }

        val event = outboxEvent(order.id.toString(), order.version, ProductItemRemovedEvent(order.id.toString(), productItem), PRODUCT_ITEM_REMOVED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        log.info("removeProductItem result: $result, version: ${order.version + 1}")
    }


    @Transactional
    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { pay() }
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderPaidEvent(savedOrder.id.toString(), paymentId), ORDER_PAID_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        savedOrder.also { log.info("paid order: $it") }
    }


    @Transactional
    override suspend fun cancel(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { cancel() }
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderCancelledEvent(savedOrder.id.toString(), ""), ORDER_CANCELLED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        savedOrder.also { log.info("order cancelled: ${order.status} for id: $id") }
    }

    @Transactional
    override suspend fun submit(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { submit() }
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderSubmittedEvent(savedOrder.id.toString()), ORDER_SUBMITTED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        savedOrder.also { log.info("order submitted: ${savedOrder.status} for id: $id") }
    }

    @Transactional
    override suspend fun complete(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { complete() }
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()


        val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderCompletedEvent(savedOrder.id.toString()), ORDER_COMPLETED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }
        savedOrder.also { log.info("order submitted: ${savedOrder.status} for id: $id") }
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {
        orderRepository.getOrderWithProductItemsByID(id)
    }

    @Transactional(readOnly = true)
    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }


    override suspend fun getOrderByID(id: UUID): OrderEntity {
        TODO("Not yet implemented")
    }

    private fun outboxEvent(aggregateId: String, version: Long, data: Any, eventType: String): OutboxRecord = OutboxRecord(
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