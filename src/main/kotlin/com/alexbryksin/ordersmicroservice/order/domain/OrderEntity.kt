package com.alexbryksin.ordersmicroservice.order.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Table(schema = "microservices", name = "orders")
data class OrderEntity(
    @Id @Column("id") var id: UUID?,
    @Column("email") var email: String?,
    @Column("address") var address: String? = null,
    @Column("status") var status: OrderStatus = OrderStatus.NEW,
    @Column("total_sum") var totalSum: BigDecimal = BigDecimal.ZERO,
    @Version @Column("version") var version: Int = 0,
    @CreatedDate @Column("created_at") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column("updated_at") var updatedAt: LocalDateTime? = null
) {

    companion object
}