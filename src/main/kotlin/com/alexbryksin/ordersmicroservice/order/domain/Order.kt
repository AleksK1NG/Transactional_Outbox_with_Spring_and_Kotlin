package com.alexbryksin.ordersmicroservice.order.domain

import java.time.LocalDateTime
import java.util.*

class Order(
    var id: UUID?,
    var email: String?,
    var address: String? = null,
    var status: OrderStatus = OrderStatus.NEW,
    var version: Long = 0,
    var productItems: MutableList<ProductItem> = arrayListOf(),
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {

    fun addProductItem(productItem: ProductItem) = productItems.add(productItem)

    fun addProductItems(items: List<ProductItem>) = productItems.addAll(items)

    fun removeProductItem(id: UUID) = productItems.removeIf { it.id == id }

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