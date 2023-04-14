package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import reactor.core.publisher.Mono
import java.util.*

interface OrderService {


    suspend fun createOrder(order: Order): Order
    suspend fun getOrderByID(id: UUID): OrderEntity
    suspend fun addOrderItem(productItem: ProductItem)
    suspend fun removeProductItem(productItem: ProductItem)
    suspend fun pay(id: UUID): Order
    suspend fun cancel(id: UUID): Order
    suspend fun submit(id: UUID): Order
    suspend fun complete(id: UUID): Order


    suspend fun getOrderWithProductItemsByID(id: UUID): Order

    fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order>
}