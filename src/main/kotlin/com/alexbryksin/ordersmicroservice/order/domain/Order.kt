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
    val productItems: MutableList<ProductItem> = arrayListOf(),
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {

    fun addProductItem(productItemEntity: ProductItem): Order {
        productItems.add(productItemEntity)
        return this
    }

    fun addProductItems(items: List<ProductItem>): Order {
        productItems.addAll(items)
        return this
    }

    fun removeProductItem(id: String): Order {
        productItems.removeIf { it.id == id }
        return this
    }

    fun pay() {
        if (productItems.isEmpty()) throw OrderHasNotProductItemsException(id)
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
        return "Order(id=$id, email=$email, address=$address, status=$status, version=$version, productItems=${productItems.size}, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    companion object {
        const val ID = "id"
        const val EMAIL = "email"
        const val ADDRESS = "address"
        const val STATUS = "status"
        const val VERSION = "version"
        const val PRODUCT_ITEMS = "productItems"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}

fun String.toUUID(): UUID? {
    if (this == "") return null
    return UUID.fromString(this)
}