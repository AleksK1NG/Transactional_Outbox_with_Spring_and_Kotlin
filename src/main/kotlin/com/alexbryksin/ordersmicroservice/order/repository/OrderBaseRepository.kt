package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
interface OrderBaseRepository {

    suspend fun getOrderWithProductItemsByID(id: UUID): Order

    fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order>

    suspend fun updateOrderVersion(id: UUID, newVersion: Long): Long

    suspend fun findOrderByID(id: UUID): Order

    suspend fun insert(order: Order): Order

    suspend fun update(order: Order): Order
}