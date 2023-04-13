package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import com.alexbryksin.ordersmicroservice.order.domain.of
import com.alexbryksin.ordersmicroservice.order.dto.OrderItemProjection
import com.alexbryksin.ordersmicroservice.order.dto.OrderProjection
import com.alexbryksin.ordersmicroservice.order.repository.OrderRepository
import com.alexbryksin.ordersmicroservice.order.repository.ProductItemRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.*


@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productItemRepository: ProductItemRepository,
    private val dbClient: DatabaseClient,
    private val entityTemplate: R2dbcEntityTemplate
) : OrderService {


    @Transactional
    suspend fun saveOrder(order: Order): Order = coroutineScope {

        val savedOrder = orderRepository.save(OrderEntity.of(order))
        val savedOrderItems = productItemRepository.saveAll(order.productItems).toList()

        log.info("savedOrder: $savedOrder, savedOrderItems: $savedOrderItems")
        savedOrder.toOrder()
    }

    @Transactional
    suspend fun addOrderItem(productItem: ProductItem) = coroutineScope {
        productItemRepository.save(productItem).also { log.info("saved product item: $it") }
    }

    @Transactional
    suspend fun removeProductItem(id: UUID) = coroutineScope {
        productItemRepository.deleteById(id).also { log.info("deleted product item: $id") }
    }

    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {
        orderRepository.getOrderWithProductItemsByID(id)
    }

    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return orderRepository.getOrderWithProductItemsByIDMono(id)
    }

    @Transactional
    override suspend fun saveOrderWithItems(orderEntity: OrderEntity, orderItems: List<ProductItem>) = coroutineScope {

        val savedOrder = orderRepository.save(orderEntity)
        val savedOrderItems = productItemRepository.saveAll(orderItems).toList()

        log.info("savedOrder: $savedOrder, savedOrderItems: $savedOrderItems")
        savedOrder
    }

    override suspend fun getOrderWithOrderItemsByIDMono(id: UUID): Any = coroutineScope {
        val result = dbClient.sql(
            """SELECT o.id as orderId, o.email, oi.id as itemId, oi.title FROM microservices.orders o 
            LEFT JOIN microservices.product_items oi on o.id = oi.order_id
            WHERE o.id = :orderId
            """.trimMargin()
        )
            .bind("orderId", id)
            .map { row, _ ->
                val order = OrderEntity(id = row["orderId", UUID::class.java]!!, row["email", String::class.java]!!)
                val item = OrderItemProjection(
                    id = row["itemId", UUID::class.java]!!,
                    title = row["title", String::class.java]!!,
                    orderId = row["orderId", UUID::class.java]!!
                )
                Pair(order, item)
            }
            .all()
            .collectList()
            .map {
                OrderProjection(
                    id = it[0].first.id!!,
                    email = it[0].first.email!!,
                    items = it.map { value -> value.second }.toMutableList()
                )
            }.awaitSingle()

        log.info("getOrderWithItemsByID result: $result")
        result
    }


    override suspend fun getOrderWithOrderItemsByID(id: UUID): Any = coroutineScope {

        val result = dbClient.sql(
            """SELECT o.id as orderId, o.email, oi.id as itemId, oi.title FROM microservices.orders o 
            LEFT JOIN microservices.product_items oi on o.id = oi.order_id
            WHERE o.id = :orderId
            """.trimMargin()
        )
            .bind("orderId", id)
            .map { row, _ ->
                val order = OrderEntity(id = row["orderId", UUID::class.java]!!, row["email", String::class.java]!!)
                val item = OrderItemProjection(
                    id = row["itemId", UUID::class.java]!!,
                    title = row["title", String::class.java]!!,
                    orderId = row["orderId", UUID::class.java]!!
                )
                Pair(order, item)
            }
            .flow()
            .toList()

        val orderProjection = OrderProjection(
            id = result[0].first.id!!,
            email = result[0].first.email!!,
            items = result.map { it.second }.toMutableList()
        )

        log.info("getOrderWithItemsByID result: $orderProjection")
        orderProjection
    }

    override suspend fun getOrderWithItemsByID(id: UUID): Any {
        val result = dbClient.sql(
            """SELECT o.id as orderId, o.email, oi.id as itemId, oi.title FROM microservices.orders o 
            LEFT JOIN microservices.product_items oi on o.id = oi.order_id
            WHERE o.id = :orderId
            """.trimMargin()
        )
            .bind("orderId", id)
            .map { row, _ ->
                val order = OrderEntity(id = row["orderId", UUID::class.java], row["email", String::class.java])
                val item =
                    ProductItem(id = row["itemId", UUID::class.java], title = row["title", String::class.java], orderId = row["orderId", UUID::class.java])
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