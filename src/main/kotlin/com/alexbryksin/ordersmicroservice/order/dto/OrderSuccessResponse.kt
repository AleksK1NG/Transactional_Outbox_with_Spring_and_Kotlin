package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus

data class OrderSuccessResponse(
    val id: String,
    val email: String,
    val address: String,
    val paymentId: String,
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
    paymentId = order.paymentId,
    productItems = order.productsList().map { ProductItemSuccessResponse.of(it) }.toList(),
    createdAt = order.createdAt.toString(),
    updatedAt = order.updatedAt.toString(),
)

fun Order.toSuccessResponse(): OrderSuccessResponse = OrderSuccessResponse(
    id = this.id,
    email = this.email,
    address = this.address,
    status = this.status,
    version = this.version,
    paymentId = this.paymentId,
    productItems = this.productsList().map { ProductItemSuccessResponse.of(it) }.toList(),
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString(),
)