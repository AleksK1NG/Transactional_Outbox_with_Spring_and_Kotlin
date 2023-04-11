package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface OrderRepository : CoroutineCrudRepository<OrderEntity, UUID> {
}