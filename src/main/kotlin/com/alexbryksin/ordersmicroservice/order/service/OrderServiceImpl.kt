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
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.insert(order).let {
                val productItemsEntityList = ProductItemEntity.listOf(order.productItems, it.id!!)
                val insertedItems = productItemRepository.insertAll(productItemsEntityList).toList()
                it.addProductItems(insertedItems.map { item -> item.toProductItem() })

                val record = outboxRecord(it.id.toString(), it.version, OrderCreatedEvent(it), ORDER_CREATED_EVENT)
                Pair(it, outboxRepository.save(record))
            }
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun addProductItem(productItem: ProductItem): Unit = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.findOrderByID(productItem.orderId!!).let { order ->
                order.incVersion()

                productItemRepository.insert(ProductItemEntity.of(productItem)).let { productItemEntity ->
                    val record = outboxRecord(
                        order.id.toString(),
                        order.version,
                        ProductItemAddedEvent.of(order, productItemEntity.toProductItem()),
                        PRODUCT_ITEM_ADDED_EVENT,
                    )

                    outboxRepository.save(record).let { savedRecord ->
                        orderRepository.updateOrderVersion(order.id!!, order.version)
                            .also { result -> log.info("addOrderItem result: $result, version: ${order.version}") }
                        savedRecord
                    }
                }
            }
        }.run { publish(this) }
    }

    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.findOrderByID(orderID).let {
                productItemRepository.deleteById(productItemId)

                it.incVersion()

                val record = outboxRecord(
                    it.id.toString(),
                    it.version,
                    ProductItemRemovedEvent.of(it, productItemId),
                    PRODUCT_ITEM_REMOVED_EVENT
                )

                outboxRepository.save(record).let { savedRecord ->
                    val result = orderRepository.updateOrderVersion(it.id!!, it.version)
                    log.info("removeProductItem result: $result, version: ${it.version}")
                    savedRecord
                }
            }
        }.run { publish(this) }
    }

    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.getOrderWithProductItemsByID(id).let {
                it.pay()

                orderRepository.update(it).let { updatedOrder ->
                    val record = outboxRecord(
                        updatedOrder.id.toString(),
                        updatedOrder.version,
                        OrderPaidEvent.of(updatedOrder, paymentId),
                        ORDER_PAID_EVENT
                    )
                    Pair(updatedOrder, outboxRepository.save(record))
                }
            }
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.findOrderByID(id).let {
                it.cancel()

                orderRepository.update(it).let { updatedOrder ->
                    val record = outboxRecord(
                        updatedOrder.id.toString(),
                        updatedOrder.version,
                        OrderCancelledEvent.of(updatedOrder, reason),
                        ORDER_CANCELLED_EVENT,
                    )
                    Pair(updatedOrder, outboxRepository.save(record))
                }
            }
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun submit(id: UUID): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.getOrderWithProductItemsByID(id).let {
                it.submit()

                orderRepository.update(it).let { updatedOrder ->
                    val record = outboxRecord(
                        updatedOrder.id.toString(),
                        updatedOrder.version,
                        OrderSubmittedEvent.of(updatedOrder),
                        ORDER_SUBMITTED_EVENT
                    )
                    Pair(updatedOrder.addProductItems(it.productItems), outboxRepository.save(record))
                }
            }
        }.run {
            publish(second)
            first
        }
    }

    override suspend fun complete(id: UUID): Order = coroutineScope {
        txOp.executeAndAwait {
            orderRepository.findOrderByID(id).let {
                it.complete()

                orderRepository.update(it).let { updatedOrder ->
                    val event = outboxRecord(
                        updatedOrder.id.toString(),
                        updatedOrder.version,
                        OrderCompletedEvent.of(updatedOrder),
                        ORDER_COMPLETED_EVENT
                    )
                    log.info("order submitted: ${updatedOrder.status} for id: $id")
                    Pair(updatedOrder, outboxRepository.save(event))
                }
            }
        }.run {
            publish(second)
            first
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

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScope {
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