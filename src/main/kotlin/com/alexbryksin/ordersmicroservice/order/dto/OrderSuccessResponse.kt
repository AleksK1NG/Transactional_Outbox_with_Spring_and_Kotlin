package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus

data class OrderSuccessResponse(
    val id: String,
    val email: String,
    val address: String,
    val status: OrderStatus,
    val version: Long,
    val productItems: List<ProductItemSuccessResponse>,
    val createdAt: String,
    val updatedAt: String
) {
    companion object
}

fun OrderSuccessResponse.Companion.of(order: Order): OrderSuccessResponse = OrderSuccessResponse(
    id = order.id,
    email = order.email,
    address = order.address,
    status = order.status,
    version = order.version,
    productItems = order.productItems.map { ProductItemSuccessResponse.of(it) }.toList(),
    createdAt = order.createdAt.toString(),
    updatedAt = order.updatedAt.toString(),
)
