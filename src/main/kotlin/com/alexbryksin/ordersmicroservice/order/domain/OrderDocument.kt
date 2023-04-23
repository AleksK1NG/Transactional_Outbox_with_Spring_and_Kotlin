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
    @Id @Field(name = ID) var id: String? = null,
    @Field(name = EMAIL) var email: String?,
    @Field(name = ADDRESS) var address: String? = null,
    @Field(name = STATUS) var status: OrderStatus = OrderStatus.NEW,
    @Field(name = VERSION) var version: Long = 0,
    @Field(name = PRODUCT_ITEMS) val productItems: MutableList<ProductItem> = arrayListOf(),
    @CreatedDate @Field(name = CREATED_AT) var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Field(name = UPDATED_AT) var updatedAt: LocalDateTime? = null
) {


    fun addProductItem(productItem: ProductItem): OrderDocument {
        productItems.add(productItem)
        return this
    }

    fun addProductItems(items: List<ProductItem>): OrderDocument {
        productItems.addAll(items)
        return this
    }

    fun removeProductItem(id: UUID): OrderDocument {
        productItems.removeIf { it.id == id }
        return this
    }

    fun pay() {
        if (productItems.isEmpty()) throw OrderHasNotProductItemsException(UUID.fromString(id))
        status = OrderStatus.PAID
    }

    fun submit() {
        if (productItems.isEmpty()) throw OrderHasNotProductItemsException(UUID.fromString(id))
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw SubmitOrderException(
            UUID.fromString(
                id
            ), status
        )
        if (status != OrderStatus.PAID) throw OrderNotPaidException(UUID.fromString(id))
        status = OrderStatus.SUBMITTED
    }

    fun cancel() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) throw CancelOrderException(
            UUID.fromString(
                id
            ), status
        )
        status = OrderStatus.CANCELLED
    }

    fun complete() {
        if (status == OrderStatus.CANCELLED || status != OrderStatus.SUBMITTED) throw CompleteOrderException(
            UUID.fromString(
                id
            ), status
        )
        status = OrderStatus.COMPLETED
    }

    fun toOrder() = Order(
        id = UUID.fromString(this.id),
        email = this.email,
        address = this.address,
        status = this.status,
        version = this.version,
        productItems = this.productItems,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

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


fun OrderDocument.Companion.of(order: Order): OrderDocument = OrderDocument(
    id = order.id.toString(),
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    productItems = order.productItems,
    createdAt = order.createdAt,
    updatedAt = order.updatedAt
)