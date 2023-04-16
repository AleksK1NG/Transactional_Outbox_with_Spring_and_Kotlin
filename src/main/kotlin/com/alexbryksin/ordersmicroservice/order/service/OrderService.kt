package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import reactor.core.publisher.Mono
import java.util.*

interface OrderService {

    suspend fun createOrder(order: Order): Order
    suspend fun getOrderByID(id: UUID): Order
    suspend fun addOrderItem(productItemEntity: ProductItemEntity)
    suspend fun removeProductItem(orderID: UUID, productItemId: UUID)
    suspend fun pay(id: UUID, paymentId: String): Order
    suspend fun cancel(id: UUID, reason: String?): Order
    suspend fun submit(id: UUID): Order
    suspend fun complete(id: UUID): Order
    suspend fun getOrderWithProductItemsByID(id: UUID): Order
    fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order>

    suspend fun deleteOutboxRecordsWithLock()
}