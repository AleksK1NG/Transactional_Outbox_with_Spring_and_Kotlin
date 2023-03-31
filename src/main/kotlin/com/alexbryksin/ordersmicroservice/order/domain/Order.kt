package com.alexbryksin.ordersmicroservice.order.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(schema = "microservices", name = "orders")
class Order(@Id var id: String? = null) {

    @Column("email") var email: String? = null
    @Column("address") var address: String? = null
    @Column("status") var status: OrderStatus? = null
}