package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable


interface OrderMongoRepository {
    suspend fun insert(order: Order): Order
    suspend fun update(order: Order): Order
    suspend fun getByID(id: String): Order
    suspend fun getAllOrders(pageable: Pageable): Page<Order>
}