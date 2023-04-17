package com.alexbryksin.ordersmicroservice.order.domain

import com.alexbryksin.ordersmicroservice.order.exceptions.*
import java.time.LocalDateTime
import java.util.*

class Order(
    var id: UUID? = null,
    var email: String?,
    var address: String? = null,
    var status: OrderStatus = OrderStatus.NEW,
    var version: Long = 0,
    var productItemEntities: MutableList<ProductItem> = arrayListOf(),
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {

    fun addProductItem(productItemEntity: ProductItem): Order {
        productItemEntities.add(productItemEntity)
        return this
    }

    fun addProductItems(items: List<ProductItem>): Order {
        productItemEntities.addAll(items)
        return this
    }

    fun removeProductItem(id: UUID): Order {
        productItemEntities.removeIf { it.id == id }
        return this
    }

    fun pay() {
        if (productItemEntities.isEmpty()) throw OrderHasNotProductItemsException(id)
        status = OrderStatus.PAID
    }

    fun submit() {
        if (productItemEntities.isEmpty()) throw OrderHasNotProductItemsException(id)
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
        return "Order(id=$id, email=$email, address=$address, status=$status, version=$version, productItems=${productItemEntities.size}, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    companion object
}