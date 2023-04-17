package com.alexbryksin.ordersmicroservice.order.domain

import io.r2dbc.spi.Row
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Table(schema = "microservices", name = "product_items")
data class ProductItemEntity(
    @Id @Column("id") var id: UUID? = null,
    @Column("order_id") var orderId: UUID? = null,
    @Column("title") var title: String?,
    @Column("price") var price: BigDecimal = BigDecimal.ZERO,
    @Column("quantity") var quantity: Long = 0,
    @Version @Column("version") var version: Long = 0,
    @CreatedDate @Column("created_at") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column("updated_at") var updatedAt: LocalDateTime? = null
) {
    companion object

    fun toProductItem() = ProductItem(
        id = this.id,
        orderId = this.orderId,
        title = this.title,
        price = this.price,
        quantity = this.quantity,
        version = this.version,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun ProductItemEntity.Companion.of(row: Row) = ProductItemEntity(
    id = row["productId", UUID::class.java],
    title = row["title", String::class.java],
    orderId = row["order_id", UUID::class.java],
    price = row["price", BigDecimal::class.java] ?: BigDecimal.ZERO,
    quantity = row["quantity", BigInteger::class.java]?.toLong() ?: 0,
)


fun ProductItemEntity.Companion.of(productItem: ProductItem): ProductItemEntity = ProductItemEntity(
    id = productItem.id,
    orderId = productItem.orderId,
    title = productItem.title,
    price = productItem.price,
    quantity = productItem.quantity,
    version = productItem.version,
    createdAt = productItem.createdAt,
    updatedAt = productItem.updatedAt
)