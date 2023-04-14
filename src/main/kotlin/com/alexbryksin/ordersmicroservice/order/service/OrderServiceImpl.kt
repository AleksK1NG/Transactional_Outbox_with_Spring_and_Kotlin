package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.*
import com.alexbryksin.ordersmicroservice.order.events.OrderCreatedEvent
import com.alexbryksin.ordersmicroservice.order.events.OrderCreatedEvent.Companion.ORDER_CREATED_EVENT
import com.alexbryksin.ordersmicroservice.order.repository.OrderOutboxRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*


@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productItemRepository: ProductItemRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val serializer: Serializer
) : OrderService {

    @Transactional
    override suspend fun createOrder(order: Order): Order = coroutineScope {
        val savedOrder = orderRepository.save(OrderEntity.of(order))
        if (order.productItems.isNotEmpty()) {
            productItemRepository.saveAll(order.productItems).toList().also { log.info("savedOrderItems: $it") }
        }

        val event = outboxEvent(savedOrder.id.toString(), savedOrder.version, OrderCreatedEvent(order), ORDER_CREATED_EVENT)
        outboxRepository.save(event).also { log.info("saved outbox record: $it") }
        savedOrder.toOrder().also { log.info("saved order: $it") }
    }

    @Transactional
    override suspend fun addOrderItem(productItem: ProductItem): Unit = coroutineScope {
        val order = orderRepository.findById(productItem.orderId!!) ?: throw RuntimeException("order with id: ${productItem.orderId} not found")
        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        productItemRepository.save(productItem).also { log.info("saved product item: $it") }
    }

    @Transactional
    override suspend fun removeProductItem(productItem: ProductItem): Unit = coroutineScope {
        val order = orderRepository.findById(productItem.orderId!!) ?: throw RuntimeException("order with id: ${productItem.orderId} not found")
        val result = orderRepository.updateOrderVersion(order.id!!, order.version + 1)
        productItemRepository.deleteById(productItem.id!!).also { log.info("deleted product item: ${productItem.id}") }
    }


    @Transactional
    override suspend fun pay(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { pay() }
        orderRepository.save(OrderEntity.of(order)).toOrder()
            .also { log.info("order status updated to: ${order.status} for id: $id") }
    }


    @Transactional
    override suspend fun cancel(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { cancel() }
        orderRepository.save(OrderEntity.of(order)).toOrder()
            .also { log.info("order status updated to: ${order.status} for id: $id") }
    }

    @Transactional
    override suspend fun submit(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { submit() }
        orderRepository.save(OrderEntity.of(order)).toOrder()
            .also { log.info("order status updated to: ${order.status} for id: $id") }
    }

    @Transactional
    override suspend fun complete(id: UUID): Order = coroutineScope {
        val orderEntity = orderRepository.findById(id) ?: throw RuntimeException("order with id: $id not found")
        val order = orderEntity.toOrder().apply { complete() }
        orderRepository.save(OrderEntity.of(order)).toOrder()
            .also { log.info("order status updated to: ${order.status} for id: $id") }
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