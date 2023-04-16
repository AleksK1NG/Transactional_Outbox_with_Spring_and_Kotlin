package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import com.alexbryksin.ordersmicroservice.order.domain.of
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*


@Repository
class OrderBaseRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val entityTemplate: R2dbcEntityTemplate
) : OrderBaseRepository {

    override suspend fun updateOrderVersion(id: UUID, newVersion: Long): Long = coroutineScope {
        dbClient.sql("UPDATE microservices.orders SET version = (version + 1) WHERE id = :id AND version = :version")
            .bind("id", id)
            .bind("version", newVersion - 1)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
            .also { log.info("for order with id: $id version updated to $newVersion") }
    }

    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScope {
        dbClient.sql(
            """SELECT o.id, o.email, o.status, o.address, o.version, o.created_at, o.updated_at, 
            |pi.id as productId, pi.price, pi.title, pi.quantity, pi.order_id, pi.version as itemVersion, pi.created_at as itemCreatedAt, pi.updated_at as itemUpdatedAt
            |FROM microservices.orders o 
            |LEFT JOIN microservices.product_items pi on o.id = pi.order_id 
            |WHERE o.id = :id""".trimMargin()
        )
            .bind("id", id)
            .map { row, _ -> Pair(OrderEntity.of(row), ProductItemEntity.of(row)) }
            .flow()
            .toList()
            .let { orderFromList(it) }
            .also { log.info("loaded order: $it") }
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
            .map { row, _ -> Pair(OrderEntity.of(row), ProductItemEntity.of(row)) }
            .all()
            .collectList()
            .map { orderFromMutableList(it) }
            .doOnNext { log.info("loaded order: $it") }
    }


    private fun orderFromMutableList(list: MutableList<Pair<OrderEntity, ProductItemEntity>>): Order = Order(
        id = list[0].first.id,
        email = list[0].first.email,
        status = list[0].first.status,
        address = list[0].first.address,
        version = list[0].first.version,
        createdAt = list[0].first.createdAt,
        updatedAt = list[0].first.updatedAt,
        productItemEntities = list.map { item -> item.second }.toMutableList()
    )

    private fun orderFromList(list: List<Pair<OrderEntity, ProductItemEntity>>): Order = Order(
        id = list[0].first.id,
        email = list[0].first.email,
        status = list[0].first.status,
        address = list[0].first.address,
        version = list[0].first.version,
        createdAt = list[0].first.createdAt,
        updatedAt = list[0].first.updatedAt,
        productItemEntities = list.map { item -> item.second }.toMutableList()
    )

    companion object {
        private val log = LoggerFactory.getLogger(OrderBaseRepositoryImpl::class.java)
    }
}