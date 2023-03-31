package com.alexbryksin.ordersmicroservice.order.domain

import java.time.LocalDateTime

class Order(
    var id: String? = null,
    var email: String? = null,
    var address: String? = null,
    var status: OrderStatus? = null,
    var version: Long = 0,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {


    override fun toString(): String {
        return "Order(id=$id, email=$email, address=$address, status=$status, version=$version, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}