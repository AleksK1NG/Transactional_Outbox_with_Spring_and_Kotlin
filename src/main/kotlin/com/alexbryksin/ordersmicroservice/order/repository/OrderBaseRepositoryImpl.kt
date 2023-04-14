package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Repository
class OrderBaseRepositoryImpl(private val dbClient: DatabaseClient) : OrderBaseRepository {

    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {

        val result = dbClient.sql(
            """SELECT o.id, o.email, o.status, o.address, o.version, o.created_at, o.updated_at, 
            |pi.id as productId, pi.price, pi.title, pi.quantity, pi.order_id, pi.version as itemVersion, pi.created_at as itemCreatedAt, pi.updated_at as itemUpdatedAt
            |FROM microservices.orders o 
            |LEFT JOIN microservices.product_items pi on o.id = pi.order_id 
            |WHERE o.id = :id""".trimMargin()
        )
            .bind("id", id)
            .map { row, _ ->
                try {
                    val order = OrderEntity(
                        id = row["id", UUID::class.java],
                        email = row["email", String::class.java],
                        status = OrderStatus.valueOf(row["status", String::class.java] ?: ""),
                        address = row["address", String::class.java],
                        version = row["version", BigInteger::class.java]?.toLong() ?: 0,
                        createdAt = row["created_at", LocalDateTime::class.java],
                        updatedAt = row["updated_at", LocalDateTime::class.java],
                    )
                    val productItem = ProductItem(
                        id = row["productId", UUID::class.java],
                        title = row["title", String::class.java],
                        orderId = row["order_id", UUID::class.java],
                        price = row["price", BigDecimal::class.java] ?: BigDecimal.ZERO,
                        quantity = row["quantity", BigInteger::class.java]?.toLong() ?: 0,
                        version = row["version", BigInteger::class.java]?.toLong() ?: 0,
                        createdAt = row["itemCreatedAt", LocalDateTime::class.java],
                        updatedAt = row["itemUpdatedAt", LocalDateTime::class.java],
                    )
                    Pair(order, productItem)
                } catch (e: Exception) {
                    log.error("error while mapping: ${e.localizedMessage}")
                    throw e
                }
            }
            .flow()
            .toList()
        val order = Order(
            id = result[0].first.id,
            email = result[0].first.email,
            status = result[0].first.status,
            address = result[0].first.address,
            version = result[0].first.version,
            createdAt = result[0].first.createdAt,
            updatedAt = result[0].first.updatedAt,
            productItems = result.map { it.second }.toMutableList()
        )

        order.also { log.info("loaded order: $order") }
    }

    override fun getOrderWithProductItemsByIDMono(id: UUID): Mono<Order> {
        return dbClient.sql(
            """SELECT o.id, o.email, o.status, o.address, o.version, o.created_at, o.updated_at, 
            |pi.id as productId, pi.price, pi.title, pi.quantity, pi.order_id
            |FROM microservices.orders o 
            |LEFT JOIN microservices.product_items pi on o.id = pi.order_id 
            |WHERE o.id = :id""".trimMargin()
        )
            .bind("id", id)
            .map { row, _ ->
                val order = OrderEntity(
                    id = row["id", UUID::class.java]!!,
                    email = row["email", String::class.java]!!,
                    status = row["status", OrderStatus::class.java]!!,
                    address = row["address", String::class.java]!!,
                    version = row["version", Long::class.java]!!,
                    createdAt = row["created_at", LocalDateTime::class.java]!!,
                    updatedAt = row["updated_at", LocalDateTime::class.java]!!,
                )
                val productItem = ProductItem(
                    id = row["productId", UUID::class.java],
                    title = row["title", String::class.java],
                    orderId = row["order_id", UUID::class.java],
                    price = row["price", BigDecimal::class.java] ?: BigDecimal.ZERO,
                    quantity = row["quantity", Long::class.java] ?: 0,
                )
                Pair(order, productItem)
            }
            .all()
            .collectList()
            .map {
                Order(
                    id = it[0].first.id,
                    email = it[0].first.email,
                    status = it[0].first.status,
                    address = it[0].first.address,
                    version = it[0].first.version,
                    createdAt = it[0].first.createdAt,
                    updatedAt = it[0].first.updatedAt,
                    productItems = it.map { it.second }.toMutableList()
                )
            }
            .doOnNext { log.info("loaded order: $it") }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderBaseRepositoryImpl::class.java)
    }
}