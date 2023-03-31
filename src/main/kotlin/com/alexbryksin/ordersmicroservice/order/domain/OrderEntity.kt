package com.alexbryksin.ordersmicroservice.order.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(schema = "microservices", name = "orders")
class OrderEntity(@Id var id: String? = null) {

    @Column("email")
    var email: String? = null

    @Column("address")
    var address: String? = null

    @Column("status")
    var status: OrderStatus? = null

    @Column("createdAt")
    @CreatedDate
    var createdAt: LocalDateTime? = null

    @Column("updatedAt")
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
}