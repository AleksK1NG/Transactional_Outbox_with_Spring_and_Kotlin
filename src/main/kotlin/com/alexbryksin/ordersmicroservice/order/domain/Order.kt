package com.alexbryksin.ordersmicroservice.order.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class Order(
    var id: UUID?,
    var email: String?,
    var address: String? = null,
    var status: OrderStatus = OrderStatus.NEW,
    var totalSum: BigDecimal = BigDecimal.ZERO,
    var version: Int = 0,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
) {


    override fun toString(): String {
        return "Order(id=$id, email=$email, address=$address, status=$status, version=$version, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}