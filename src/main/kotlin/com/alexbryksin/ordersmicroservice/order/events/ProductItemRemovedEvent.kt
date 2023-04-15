package com.alexbryksin.ordersmicroservice.order.events

data class ProductItemRemovedEvent(
    val orderId: String,
    val productItemId: String
) {
    companion object {
        const val PRODUCT_ITEM_REMOVED_EVENT = "PRODUCT_ITEM_REMOVED"
    }
}
