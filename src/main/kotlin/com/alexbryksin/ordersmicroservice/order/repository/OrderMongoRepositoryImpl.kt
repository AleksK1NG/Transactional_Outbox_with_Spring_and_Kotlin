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
import com.alexbryksin.ordersmicroservice.order.domain.of
import com.alexbryksin.ordersmicroservice.order.domain.toUUID
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
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
class OrderMongoRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate) : OrderMongoRepository {

    override suspend fun insert(order: Order): Order = withContext(Dispatchers.IO) {
        mongoTemplate.insert(OrderDocument.of(order)).awaitSingle().toOrder()
            .also { log.info("inserted order: $it") }
    }

    override suspend fun update(order: Order): Order = withContext(Dispatchers.IO) {
        val query = Query.query(Criteria.where(ID).`is`(order.id).and(VERSION).`is`(order.version - 1))

        val update = Update()
            .set(EMAIL, order.email)
            .set(ADDRESS, order.address)
            .set(STATUS, order.status)
            .set(VERSION, order.version)
            .set(PAYMENT_ID, order.paymentId)
            .set(PRODUCT_ITEMS, order.productItems)

        val options = FindAndModifyOptions.options().returnNew(true).upsert(false)
        val updatedOrderDocument = mongoTemplate.findAndModify(query, update, options, OrderDocument::class.java)
            .awaitSingleOrNull() ?: throw OrderNotFoundException(order.id.toUUID())

        updatedOrderDocument.toOrder().also { log.info("updated order: $it") }
    }

    override suspend fun getByID(id: String): Order = withContext(Dispatchers.IO) {
        mongoTemplate.findById(id, OrderDocument::class.java).awaitSingle().toOrder()
            .also { log.info("found order document: $it") }
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = withContext(Dispatchers.IO) {
        val query = Query().with(pageable)
        val data = async { mongoTemplate.find(query, OrderDocument::class.java).collectList().awaitSingle() }.await()
        val count = async { mongoTemplate.count(Query(), OrderDocument::class.java).awaitSingle() }.await()
        PageableExecutionUtils.getPage(data.map { it.toOrder() }, pageable) { count }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderMongoRepositoryImpl::class.java)
    }
}