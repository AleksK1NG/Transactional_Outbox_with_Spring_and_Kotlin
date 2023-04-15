package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem

data class ProductItemAddedEvent(
        val orderId: String,
        val productItem: ProductItem
) {
    companion object {
        const val PRODUCT_ITEM_ADDED_EVENT = "PRODUCT_ITEM_ADDED"
    }
}
