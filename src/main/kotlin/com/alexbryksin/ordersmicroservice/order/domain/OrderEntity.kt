package com.alexbryksin.ordersmicroservice.order.domain

import io.r2dbc.spi.Row
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

@Table(schema = "microservices", name = "orders")
data class OrderEntity(
    @Id @Column("id") var id: UUID?,
    @Column("email") var email: String?,
    @Column("address") var address: String? = null,
    @Column("status") var status: OrderStatus = OrderStatus.NEW,
    @Version @Column("version") var version: Long = 0,
    @CreatedDate @Column("created_at") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column("updated_at") var updatedAt: LocalDateTime? = null
) {
    companion object

    fun toOrder() = Order(
        id = this.id,
        email = this.email,
        address = this.address,
        status = this.status,
        version = this.version,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun OrderEntity.Companion.of(order: Order): OrderEntity = OrderEntity(
    id = order.id,
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    createdAt = order.createdAt,
    updatedAt = order.updatedAt
)

fun OrderEntity.Companion.of(row: Row) =  OrderEntity(
    id = row["id", UUID::class.java],
    email = row["email", String::class.java],
    status = OrderStatus.valueOf(row["status", String::class.java] ?: ""),
    address = row["address", String::class.java]!!,
    version = row["version", BigInteger::class.java]?.toLong() ?: 0,
    createdAt = row["created_at", LocalDateTime::class.java],
    updatedAt = row["updated_at", LocalDateTime::class.java],
)