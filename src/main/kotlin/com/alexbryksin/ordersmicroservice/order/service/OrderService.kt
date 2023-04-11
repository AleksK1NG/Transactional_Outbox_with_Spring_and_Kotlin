package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.OrderItem
import java.util.*

interface OrderService {

    suspend fun saveOrderWithItems(orderEntity: OrderEntity, orderItems: List<OrderItem>): OrderEntity
    suspend fun getOrderWithItemsByID(id: UUID): Any
    suspend fun getOrderByID(id: UUID): OrderEntity
}