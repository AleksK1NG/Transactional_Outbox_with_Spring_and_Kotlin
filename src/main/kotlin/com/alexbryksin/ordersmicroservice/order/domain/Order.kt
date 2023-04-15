package com.alexbryksin.ordersmicroservice.order.domain

import java.time.LocalDateTime
import java.util.*

class Order(
    var id: UUID? = null,
    var email: String?,
    var address: String? = null,
    var status: OrderStatus = OrderStatus.NEW,
    var version: Long = 0,
    var productItems: MutableList<ProductItem> = arrayListOf(),
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {

    fun addProductItem(productItem: ProductItem): Order {
        productItems.add(productItem)
        return this
    }

    fun addProductItems(items: List<ProductItem>): Order {
        productItems.addAll(items)
        return this
    }

    fun removeProductItem(id: UUID): Order {
        productItems.removeIf { it.id == id }
        return this
    }

    fun pay() {
        if (productItems.isEmpty()) throw RuntimeException("invalid state")
        status = OrderStatus.PAID
    }

    fun submit() {
        if (productItems.isEmpty()) throw RuntimeException("invalid state")
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw RuntimeException("invalid state")
        if (status != OrderStatus.PAID) throw RuntimeException("invalid state")
        status = OrderStatus.SUBMITTED
    }

    fun cancel() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw RuntimeException("invalid state")
        status = OrderStatus.CANCELLED
    }

    fun complete() {
        if (status == OrderStatus.CANCELLED) throw RuntimeException("invalid state")
        if (status != OrderStatus.SUBMITTED) throw RuntimeException("invalid state")
        status = OrderStatus.COMPLETED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Order

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Order(id=$id, email=$email, address=$address, status=$status, version=$version, productItems=${productItems.size}, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    companion object
}