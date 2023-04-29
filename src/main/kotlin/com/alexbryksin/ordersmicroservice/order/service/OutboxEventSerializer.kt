package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import com.alexbryksin.ordersmicroservice.order.events.*
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class OutboxEventSerializer(private val serializer: Serializer) {

    fun orderCreatedEventOf(order: Order) = outboxRecord(
        order.id,
        order.version,
        OrderCreatedEvent(order),
        OrderCreatedEvent.ORDER_CREATED_EVENT,
    )

    fun productItemAddedEventOf(order: Order, productItemEntity: ProductItemEntity) = outboxRecord(
        order.id,
        order.version,
        ProductItemAddedEvent.of(order, productItemEntity.toProductItem()),
        ProductItemAddedEvent.PRODUCT_ITEM_ADDED_EVENT,
    )

    fun productItemRemovedEventOf(order: Order, productItemId: UUID) = outboxRecord(
        order.id,
        order.version,
        ProductItemRemovedEvent.of(order, productItemId),
        ProductItemRemovedEvent.PRODUCT_ITEM_REMOVED_EVENT
    )

    fun orderPaidEventOf(order: Order, paymentId: String) = outboxRecord(
        order.id,
        order.version,
        OrderPaidEvent.of(order, paymentId),
        OrderPaidEvent.ORDER_PAID_EVENT
    )

    fun orderCancelledEventOf(order: Order, reason: String?) = outboxRecord(
        order.id,
        order.version,
        OrderCancelledEvent.of(order, reason),
        OrderCancelledEvent.ORDER_CANCELLED_EVENT,
    )

    fun orderSubmittedEventOf(order: Order) = outboxRecord(
        order.id,
        order.version,
        OrderSubmittedEvent.of(order),
        OrderSubmittedEvent.ORDER_SUBMITTED_EVENT
    )

    fun orderCompletedEventOf(order: Order) = outboxRecord(
        order.id,
        order.version,
        OrderCompletedEvent.of(order),
        OrderCompletedEvent.ORDER_COMPLETED_EVENT
    )


    fun outboxRecord(aggregateId: String, version: Long, data: Any, eventType: String): OutboxRecord =
        OutboxRecord(
            eventId = null,
            aggregateId = aggregateId,
            eventType = eventType,
            data = serializer.serializeToBytes(data),
            version = version,
            timestamp = LocalDateTime.now()
        )
}