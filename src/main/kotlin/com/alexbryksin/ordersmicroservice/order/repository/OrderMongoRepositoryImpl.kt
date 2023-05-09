package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.ADDRESS
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.EMAIL
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.ID
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.PAYMENT_ID
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.PRODUCT_ITEMS
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.STATUS
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument.Companion.VERSION
import com.alexbryksin.ordersmicroservice.order.domain.toDocument
import com.alexbryksin.ordersmicroservice.order.domain.toUUID
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository


@Repository
class OrderMongoRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val or: ObservationRegistry,
) : OrderMongoRepository {

    override suspend fun insert(order: Order): Order = coroutineScopeWithObservation(INSERT, or) { observation ->
        withContext(Dispatchers.IO) {
            mongoTemplate.insert(order.toDocument()).awaitSingle().toOrder()
                .also { log.info("inserted order: $it") }
                .also { observation.highCardinalityKeyValue("order", it.toString()) }
        }
    }

    override suspend fun update(order: Order): Order = coroutineScopeWithObservation(UPDATE, or) { observation ->
        withContext(Dispatchers.IO) {
            val query = Query.query(Criteria.where(ID).`is`(order.id).and(VERSION).`is`(order.version - 1))

            val update = Update()
                .set(EMAIL, order.email)
                .set(ADDRESS, order.address)
                .set(STATUS, order.status)
                .set(VERSION, order.version)
                .set(PAYMENT_ID, order.paymentId)
                .set(PRODUCT_ITEMS, order.productsList())

            val options = FindAndModifyOptions.options().returnNew(true).upsert(false)
            val updatedOrderDocument = mongoTemplate.findAndModify(query, update, options, OrderDocument::class.java)
                .awaitSingleOrNull() ?: throw OrderNotFoundException(order.id.toUUID())

            observation.highCardinalityKeyValue("order", updatedOrderDocument.toString())
            updatedOrderDocument.toOrder().also { orderDocument -> log.info("updated order: $orderDocument") }
        }
    }

    override suspend fun getByID(id: String): Order = coroutineScopeWithObservation(GET_BY_ID, or) { observation ->
        withContext(Dispatchers.IO) {
            mongoTemplate.findById(id, OrderDocument::class.java).awaitSingle().toOrder()
                .also { log.info("found order: $it") }
                .also { observation.highCardinalityKeyValue("order", it.toString()) }
        }
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScopeWithObservation(GET_ALL_ORDERS, or) { observation ->
        withContext(Dispatchers.IO) {
            val query = Query().with(pageable)
            val data = async { mongoTemplate.find(query, OrderDocument::class.java).collectList().awaitSingle() }.await()
            val count = async { mongoTemplate.count(Query(), OrderDocument::class.java).awaitSingle() }.await()
            PageableExecutionUtils.getPage(data.map { it.toOrder() }, pageable) { count }
                .also { observation.highCardinalityKeyValue("pageResult", it.pageable.toString()) }
        }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderMongoRepositoryImpl::class.java)

        private const val GET_BY_ID = "OrderMongoRepository.getByID"
        private const val GET_ALL_ORDERS = "OrderMongoRepository.getAllOrders"
        private const val UPDATE = "OrderMongoRepository.update"
        private const val INSERT = "OrderMongoRepository.insert"
    }
}