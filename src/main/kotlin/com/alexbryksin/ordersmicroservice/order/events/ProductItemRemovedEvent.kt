package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order
import java.util.*

data class ProductItemRemovedEvent(
    val orderId: String,
    override val version: Long,
    val productItemId: String,
) : BaseEvent(orderId, version) {
    companion object {
        const val PRODUCT_ITEM_REMOVED_EVENT = "PRODUCT_ITEM_REMOVED"
    }
}

fun ProductItemRemovedEvent.Companion.of(order: Order, itemId: UUID): ProductItemRemovedEvent = ProductItemRemovedEvent(
    orderId = order.id.toString(),
    version = order.version,
    productItemId = itemId.toString()
)