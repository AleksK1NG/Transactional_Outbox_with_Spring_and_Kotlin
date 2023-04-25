package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem

data class ProductItemAddedEvent(
    val orderId: String,
    override val version: Long,
    val productItem: ProductItem
) : BaseEvent(orderId, version) {
    companion object {
        const val PRODUCT_ITEM_ADDED_EVENT = "PRODUCT_ITEM_ADDED"
    }
}

fun ProductItemAddedEvent.Companion.of(order: Order, item: ProductItem): ProductItemAddedEvent = ProductItemAddedEvent(
    orderId = order.id,
    version = order.version,
    item
)
