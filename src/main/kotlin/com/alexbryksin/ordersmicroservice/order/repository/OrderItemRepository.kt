package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OrderItem
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface OrderItemRepository : CoroutineCrudRepository<OrderItem, UUID> {
}