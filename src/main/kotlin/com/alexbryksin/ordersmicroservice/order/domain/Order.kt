package com.alexbryksin.ordersmicroservice.order.domain

import com.alexbryksin.ordersmicroservice.order.exceptions.*
import java.time.LocalDateTime
import java.util.*

class Order(
    var id: String = "",
    var email: String = "",
    var address: String = "",
    var status: OrderStatus = OrderStatus.NEW,
    var version: Long = 0,
    var productItems: MutableMap<String, ProductItem> = linkedMapOf(),
    var paymentId: String = "",
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {

    fun addProductItem(productItem: ProductItem): Order {
        if (productItems.containsKey(productItem.id)) {
            val item = productItems[productItem.id]!!
            productItems[productItem.id] = item.copy(quantity = (item.quantity + productItem.quantity), version = productItem.version)
            return this
        }

        productItems[productItem.id] = productItem
        return this
    }

    fun addProductItems(items: List<ProductItem>): Order {
        items.forEach { addProductItem(it) }
        return this
    }

    fun removeProductItem(id: String): Order {
        productItems.remove(id)
        return this
    }

    fun productsList() = productItems.toList().map { it.second }

    fun pay(paymentId: String) {
        if (productItems.isEmpty()) throw OrderHasNotProductItemsException(id)
        if (paymentId.isBlank()) throw InvalidPaymentIdException(id, paymentId)
        this.paymentId = paymentId
        status = OrderStatus.PAID
    }

    fun submit() {
        if (productItems.isEmpty()) throw OrderHasNotProductItemsException(id)
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw SubmitOrderException(id, status)
        if (status != OrderStatus.PAID) throw OrderNotPaidException(id)
        status = OrderStatus.SUBMITTED
    }

    fun cancel() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw CancelOrderException(id, status)
        status = OrderStatus.CANCELLED
    }

    fun complete() {
        if (status == OrderStatus.CANCELLED || status != OrderStatus.SUBMITTED) throw CompleteOrderException(id, status)
        status = OrderStatus.COMPLETED
    }

    fun incVersion(): Order {
        version++
        return this
    }

    fun decVersion(): Order {
        version--
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Order

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Order(id='$id', email='$email', address='$address', status=$status, version=$version, productItems=${productItems.size}, paymentId='$paymentId', createdAt=$createdAt, updatedAt=$updatedAt)"
    }


    companion object
}

fun String.toUUID(): UUID? {
    if (this == "") return null
    return UUID.fromString(this)
}