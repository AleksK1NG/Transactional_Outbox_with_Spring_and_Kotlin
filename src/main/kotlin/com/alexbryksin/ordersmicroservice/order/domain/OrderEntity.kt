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
    @Id @Column(ID) var id: UUID?,
    @Column(EMAIL) var email: String?,
    @Column(ADDRESS) var address: String? = null,
    @Column(PAYMENT_ID) var paymentId: String? = null,
    @Column(STATUS) var status: OrderStatus = OrderStatus.NEW,
    @Version @Column(VERSION) var version: Long = 0,
    @CreatedDate @Column(CREATED_AT) var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column(UPDATED_AT) var updatedAt: LocalDateTime? = null
) {


    fun toOrder() = Order(
        id = this.id.toString(),
        email = this.email ?: "",
        address = this.address ?: "",
        status = this.status,
        version = this.version,
        paymentId = this.paymentId ?: "",
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    companion object {
        const val ID = "id"
        const val EMAIL = "email"
        const val ADDRESS = "address"
        const val STATUS = "status"
        const val VERSION = "version"
        const val PAYMENT_ID = "payment_id"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }
}

fun OrderEntity.Companion.of(order: Order): OrderEntity = OrderEntity(
    id = order.id.toUUID(),
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    paymentId = order.paymentId,
    createdAt = order.createdAt,
    updatedAt = order.updatedAt
)

fun OrderEntity.Companion.of(row: Row) = OrderEntity(
    id = row[ID, UUID::class.java],
    email = row[EMAIL, String::class.java],
    status = OrderStatus.valueOf(row[STATUS, String::class.java] ?: ""),
    address = row[ADDRESS, String::class.java],
    paymentId = row[PAYMENT_ID, String::class.java],
    version = row[VERSION, BigInteger::class.java]?.toLong() ?: 0,
    createdAt = row[CREATED_AT, LocalDateTime::class.java],
    updatedAt = row[UPDATED_AT, LocalDateTime::class.java],
)