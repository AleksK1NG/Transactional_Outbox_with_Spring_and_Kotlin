package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import reactor.core.publisher.Mono
import java.util.*

interface OrderService {

    suspend fun saveOrderWithItems(orderEntity: OrderEntity, orderItems: List<ProductItem>): OrderEntity
    suspend fun getOrderWithItemsByID(id: UUID): Any
    suspend fun getOrderByID(id: UUID): OrderEntity
    suspend fun getOrderWithOrderItemsByID(id: UUID): Any
    suspend fun getOrderWithOrderItemsByIDMono(id: UUID): Any

    suspend fun getOrderWithProductItemsByID(id: UUID): Order

    fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order>
}