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
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
import com.alexbryksin.ordersmicroservice.order.repository.OrderOutboxRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.coroutineScope
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
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.save(OrderEntity.of(order)).let {
                order.productItems.forEach { item -> item.orderId = it.id }
                val insertedItems = productItemRepository.insertAll(order.productItems).toList()

                val record = outboxRecord(it.id.toString(), it.version, OrderCreatedEvent(order), ORDER_CREATED_EVENT)
                outboxRepository.save(record).also { savedRecord -> log.info("saved outbox record: $savedRecord") }

                it.toOrder().addProductItems(insertedItems).also { result -> log.info("saved order: $result") }
            }
        }
    }

    @Transactional
    override suspend fun addOrderItem(productItem: ProductItem): Unit = coroutineScope {
        val order = orderRepository.findById(productItem.orderId!!) ?: throw OrderNotFoundException(productItem.orderId)
        productItemRepository.insert(productItem).also { log.info("saved product item: $it") }

        val event = outboxRecord(order.id.toString(), order.version + 1, ProductItemAddedEvent(order.id.toString(), productItem), PRODUCT_ITEM_ADDED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        log.info("addOrderItem result: $result, version: ${order.version + 1}")
    }

    @Transactional
    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScope {
        val order = orderRepository.findById(orderID) ?: throw OrderNotFoundException(orderID)
        productItemRepository.deleteById(productItemId).also { log.info("deleted product item: $productItemId") }

        val record = outboxRecord(
            order.id.toString(),
            order.version + 1,
            ProductItemRemovedEvent(order.id.toString(), productItemId.toString()), PRODUCT_ITEM_REMOVED_EVENT
        )
        outboxRepository.save(record).also { log.info("saved outbox record: $it") }

        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        log.info("removeProductItem result: $result, version: ${order.version + 1}")
    }


    @Transactional
    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScope {
        val order = orderRepository.getOrderWithProductItemsByID(id)
        order.pay()
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val record = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderPaidEvent(savedOrder.id.toString(), paymentId), ORDER_PAID_EVENT)
        outboxRepository.save(record).also { log.info("saved outbox record: $it") }

        savedOrder.also { log.info("paid order: $it") }
    }


    @Transactional
    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw OrderNotFoundException(id)
        val order = orderEntity.toOrder().apply { cancel() }
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val record = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderCancelledEvent(savedOrder.id.toString(), reason), ORDER_CANCELLED_EVENT)
        outboxRepository.save(record).also { log.info("saved outbox record: $it") }

        savedOrder.also { log.info("order cancelled: ${order.status} for id: $id") }
    }

    @Transactional
    override suspend fun submit(id: UUID): Order = coroutineScope {
        val order = orderRepository.getOrderWithProductItemsByID(id)
        order.submit()
        val savedOrder = orderRepository.save(OrderEntity.of(order)).toOrder()

        val event = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderSubmittedEvent(savedOrder.id.toString()), ORDER_SUBMITTED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }

        savedOrder.addProductItems(order.productItems)
            .also { log.info("order submitted: ${savedOrder.status} for id: $id") }
    }

    @Transactional
    override suspend fun complete(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw OrderNotFoundException(id)
        orderEntity.toOrder().let {
            it.complete()
            val savedOrder = orderRepository.save(OrderEntity.of(it)).toOrder()

            val event = outboxRecord(savedOrder.id.toString(), savedOrder.version, OrderCompletedEvent(savedOrder.id.toString()), ORDER_COMPLETED_EVENT)
            outboxRepository.save(event).also { log.info("saved outbox record: $it") }
            savedOrder.also { log.info("order submitted: ${savedOrder.status} for id: $id") }
        }
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {
        orderRepository.getOrderWithProductItemsByID(id)
    }

    @Transactional(readOnly = true)
    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }


    override suspend fun getOrderByID(id: UUID): Order = coroutineScope {
        orderRepository.findById(id)?.toOrder() ?: throw OrderNotFoundException(id)
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