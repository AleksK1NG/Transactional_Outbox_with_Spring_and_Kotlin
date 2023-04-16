package com.alexbryksin.ordersmicroservice.order.domain

import com.alexbryksin.ordersmicroservice.order.exceptions.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime
import java.util.*


@Document(collection = "orders")
data class OrderDocument(
    @Id @Field(name = "id") var id: String? = null,
    @Field(name = "email") var email: String?,
    @Field(name = "address") var address: String? = null,
    @Field(name = "status") var status: OrderStatus = OrderStatus.NEW,
    @Field(name = "version") var version: Long = 0,
    @Field(name = "productItems") val productItemEntities: MutableList<ProductItem> = arrayListOf(),
    @CreatedDate @Field(name = "createdAt") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Field(name = "updatedAt") var updatedAt: LocalDateTime? = null
) {


    fun addProductItem(productItem: ProductItem): OrderDocument {
        productItemEntities.add(productItem)
        return this
    }

    fun addProductItems(items: List<ProductItem>): OrderDocument {
        productItemEntities.addAll(items)
        return this
    }

    fun removeProductItem(id: String): OrderDocument {
        productItemEntities.removeIf { it.id == id }
        return this
    }

    fun pay() {
        if (productItemEntities.isEmpty()) throw OrderHasNotProductItemsException(UUID.fromString(id))
        status = OrderStatus.PAID
    }

    fun submit() {
        if (productItemEntities.isEmpty()) throw OrderHasNotProductItemsException(UUID.fromString(id))
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw SubmitOrderException(UUID.fromString(id), status)
        if (status != OrderStatus.PAID) throw OrderNotPaidException(UUID.fromString(id))
        status = OrderStatus.SUBMITTED
    }

    fun cancel() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw CancelOrderException(UUID.fromString(id), status)
        status = OrderStatus.CANCELLED
    }

    fun complete() {
        if (status == OrderStatus.CANCELLED || status != OrderStatus.SUBMITTED) throw CompleteOrderException(UUID.fromString(id), status)
        status = OrderStatus.COMPLETED
    }

    fun toOrder() = Order(
        id = UUID.fromString(this.id),
        email = this.email,
        address = this.address,
        status = this.status,
        version = this.version,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    companion object
}


fun OrderDocument.Companion.of(order: Order): OrderDocument = OrderDocument(
    id = order.id.toString(),
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    createdAt = order.createdAt,
    updatedAt = order.updatedAt
)