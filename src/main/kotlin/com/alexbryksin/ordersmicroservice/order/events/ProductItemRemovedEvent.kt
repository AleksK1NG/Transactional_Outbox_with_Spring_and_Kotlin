package com.alexbryksin.ordersmicroservice.order.events

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem

data class ProductItemRemovedEvent(
    val orderId: String,
    val productItem: ProductItem
) {
    companion object {
        const val PRODUCT_ITEM_REMOVED_EVENT = "PRODUCT_ITEM_REMOVED"
    }
}
