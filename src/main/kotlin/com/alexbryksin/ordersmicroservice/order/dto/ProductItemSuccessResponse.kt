package com.alexbryksin.ordersmicroservice.order.dto

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import java.math.BigDecimal

data class ProductItemSuccessResponse(
    val id: String,
    var orderId: String,
    val title: String?,
    val price: BigDecimal,
    val quantity: Long,
    val version: Long,
    val createdAt: String,
    val updatedAt: String
) {
    companion object
}

fun ProductItemSuccessResponse.Companion.of(productItemEntity: ProductItemEntity): ProductItemSuccessResponse = ProductItemSuccessResponse(
    id = productItemEntity.id.toString(),
    orderId = productItemEntity.orderId.toString(),
    title = productItemEntity.title,
    price = productItemEntity.price,
    quantity = productItemEntity.quantity,
    version = productItemEntity.version,
    createdAt = productItemEntity.createdAt.toString(),
    updatedAt = productItemEntity.updatedAt.toString()
)

fun ProductItemSuccessResponse.Companion.of(productItem: ProductItem): ProductItemSuccessResponse = ProductItemSuccessResponse(
    id = productItem.id.toString(),
    orderId = productItem.orderId.toString(),
    title = productItem.title,
    price = productItem.price,
    quantity = productItem.quantity,
    version = productItem.version,
    createdAt = productItem.createdAt.toString(),
    updatedAt = productItem.updatedAt.toString()
)