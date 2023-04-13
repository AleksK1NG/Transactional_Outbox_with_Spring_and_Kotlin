package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
interface OrderBaseRepository {

    suspend fun getOrderWithProductItemsByID(id: UUID): Order

    fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order>
}