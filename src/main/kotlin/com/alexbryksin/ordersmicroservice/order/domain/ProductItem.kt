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


@Table(schema = "microservices", name = "product_items")
data class ProductItem(
    @Id @Column("id") var id: UUID?,
    @Column("order_id") var orderId: UUID?,
    @Column("title") var title: String?,
    @Column("price") var price: BigDecimal = BigDecimal.ZERO,
    @Column("quantity") var quantity: Long = 0,
    @Version @Column("version") var version: Long = 0,
    @CreatedDate @Column("created_at") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column("updated_at") var updatedAt: LocalDateTime? = null
)
