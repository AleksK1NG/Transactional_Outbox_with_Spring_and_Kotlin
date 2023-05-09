package com.alexbryksin.ordersmicroservice.order.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime


@Document(collection = "orders")
data class OrderDocument(
    @Id @Field(name = ID) var id: String = "",
    @Field(name = EMAIL) var email: String = "",
    @Field(name = ADDRESS) var address: String = "",
    @Field(name = PAYMENT_ID) var paymentId: String = "",
    @Field(name = STATUS) var status: OrderStatus = OrderStatus.NEW,
    @Field(name = VERSION) var version: Long = 0,
    @Field(name = PRODUCT_ITEMS) val productItems: MutableList<ProductItem> = arrayListOf(),
    @CreatedDate @Field(name = CREATED_AT) var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Field(name = UPDATED_AT) var updatedAt: LocalDateTime? = null
) {

    fun toOrder() = Order(
        id = id,
        email = this.email,
        address = this.address,
        status = this.status,
        version = this.version,
        paymentId = this.paymentId,
        productItems = this.productItems.associateBy { it.id }.toMutableMap(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    companion object {
        const val ID = "id"
        const val EMAIL = "email"
        const val ADDRESS = "address"
        const val STATUS = "status"
        const val VERSION = "version"
        const val PAYMENT_ID = "paymentId"
        const val PRODUCT_ITEMS = "productItems"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}


fun OrderDocument.Companion.of(order: Order): OrderDocument = OrderDocument(
    id = order.id,
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    paymentId = order.paymentId,
    productItems = order.productsList().toMutableList(),
    createdAt = order.createdAt,
    updatedAt = order.updatedAt
)

fun Order.toDocument(): OrderDocument = OrderDocument(
    id = this.id,
    email = this.email,
    address = this.address,
    status = this.status,
    version = this.version,
    paymentId = this.paymentId,
    productItems = this.productsList().toMutableList(),
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)