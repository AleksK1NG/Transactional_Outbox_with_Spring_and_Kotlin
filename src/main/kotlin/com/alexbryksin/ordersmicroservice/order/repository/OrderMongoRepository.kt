package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order


interface OrderMongoRepository {
    suspend fun insert(order: Order): Order
    suspend fun update(order: Order): Order
    suspend fun getByID(id: String): Order
}