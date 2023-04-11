package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.OrderItem
import com.alexbryksin.ordersmicroservice.order.repository.OrderItemRepository
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val dbClient: DatabaseClient,
) : OrderService {

    @Transactional
    override suspend fun saveOrderWithItems(orderEntity: OrderEntity, orderItems: List<OrderItem>) = coroutineScope {

        val savedOrder = orderRepository.save(orderEntity)
        val savedOrderItems = orderItemRepository.saveAll(orderItems).toList()

        log.info("savedOrder: $savedOrder, savedOrderItems: $savedOrderItems")
        savedOrder
    }

    override suspend fun getOrderWithItemsByID(id: UUID): Any {
        val result = dbClient.sql(
            """SELECT o.id as orderId, o.email, oi.id as itemId, oi.title FROM microservices.orders o 
            LEFT JOIN microservices.order_items oi on o.id = oi.order_id
            WHERE o.id = :orderId
            """.trimMargin()
        )
            .bind("orderId", id)
            .map { row, _ ->
                val order = OrderEntity(id = row["orderId", UUID::class.java], row["email", String::class.java])
                val item = OrderItem(id = row["itemId", UUID::class.java], title = row["title", String::class.java], orderId = row["orderId", UUID::class.java])
                Pair(order, item)
            }
            .flow()
            .toList()
            .groupBy({ it.first.id }, { it.second })

        log.info("getOrderWithItemsByID result: $result")
        return result
    }

    override suspend fun getOrderByID(id: UUID): OrderEntity {
        TODO("Not yet implemented")
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderServiceImpl::class.java)
    }
}