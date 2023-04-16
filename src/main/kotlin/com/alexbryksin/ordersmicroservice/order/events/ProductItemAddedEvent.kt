package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity

data class ProductItemAddedEvent(
        val orderId: String,
        val productItemEntity: ProductItemEntity
): BaseEvent {
    companion object {
        const val PRODUCT_ITEM_ADDED_EVENT = "PRODUCT_ITEM_ADDED"
    }
}
