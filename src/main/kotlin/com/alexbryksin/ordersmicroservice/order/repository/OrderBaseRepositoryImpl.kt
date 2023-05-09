package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.*
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity.Companion.ID
import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity.Companion.VERSION
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import java.util.*


@Repository
class OrderBaseRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val entityTemplate: R2dbcEntityTemplate,
    private val or: ObservationRegistry
) : OrderBaseRepository {

    override suspend fun updateVersion(id: UUID, newVersion: Long): Long = coroutineScopeWithObservation(UPDATE_VERSION, or) { observation ->
        dbClient.sql("UPDATE microservices.orders SET version = (version + 1) WHERE id = :id AND version = :version")
            .bind(ID, id)
            .bind(VERSION, newVersion - 1)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
            .also { log.info("for order with id: $id version updated to $newVersion") }
            .also {
                observation.highCardinalityKeyValue("id", id.toString())
                observation.highCardinalityKeyValue("newVersion", newVersion.toString())
            }
    }

    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_WITH_PRODUCTS_BY_ID, or) { observation ->
        dbClient.sql(
            """SELECT o.id, o.email, o.status, o.address, o.version, o.payment_id, o.created_at, o.updated_at, 
            |pi.id as productId, pi.price, pi.title, pi.quantity, pi.order_id, pi.version as itemVersion, pi.created_at as itemCreatedAt, pi.updated_at as itemUpdatedAt
            |FROM microservices.orders o 
            |LEFT JOIN microservices.product_items pi on o.id = pi.order_id 
            |WHERE o.id = :id""".trimMargin()
        )
            .bind(ID, id)
            .map { row, _ -> Pair(OrderEntity.of(row), ProductItemEntity.of(row)) }
            .flow()
            .toList()
            .let { orderFromList(it) }
            .also {
                log.info("getOrderWithProductItemsByID order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    override suspend fun findOrderByID(id: UUID): Order = coroutineScopeWithObservation(FIND_ORDER_BY_ID, or) { observation ->
        val query = Query.query(Criteria.where(ID).`is`(id))
        entityTemplate.selectOne(query, OrderEntity::class.java).awaitSingleOrNull()?.toOrder()
            .also { observation.highCardinalityKeyValue("order", it.toString()) }
            ?: throw OrderNotFoundException(id)
    }

    override suspend fun insert(order: Order): Order = coroutineScopeWithObservation(INSERT, or) { observation ->
        entityTemplate.insert(order.toEntity()).awaitSingle().toOrder()
            .also {
                log.info("inserted order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    override suspend fun update(order: Order): Order = coroutineScopeWithObservation(UPDATE, or) { observation ->
        entityTemplate.update(order.toEntity()).awaitSingle().toOrder()
            .also {
                log.info("updated order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    private fun orderFromList(list: List<Pair<OrderEntity, ProductItemEntity>>): Order = Order(
        id = list[0].first.id.toString(),
        email = list[0].first.email ?: "",
        status = list[0].first.status,
        address = list[0].first.address ?: "",
        version = list[0].first.version,
        paymentId = list[0].first.paymentId ?: "",
        createdAt = list[0].first.createdAt,
        updatedAt = list[0].first.updatedAt,
        productItems = getProductItemsList(list).associateBy { it.id }.toMutableMap()
    )

    private fun getProductItemsList(list: List<Pair<OrderEntity, ProductItemEntity>>): MutableList<ProductItem> {
        if (list[0].second.id == null) return mutableListOf()
        return list.map { item -> item.second.toProductItem() }.toMutableList()
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderBaseRepositoryImpl::class.java)

        private const val UPDATE = "OrderBaseRepository.update"
        private const val INSERT = "OrderBaseRepository.insert"
        private const val FIND_ORDER_BY_ID = "OrderBaseRepository.findOrderByID"
        private const val GET_ORDER_WITH_PRODUCTS_BY_ID = "OrderBaseRepository.getOrderWithProductsByID"
        private const val UPDATE_VERSION = "OrderBaseRepository.updateVersion"
    }
}