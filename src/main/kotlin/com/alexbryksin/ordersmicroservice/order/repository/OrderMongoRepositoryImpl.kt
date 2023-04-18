package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument
import com.alexbryksin.ordersmicroservice.order.domain.of
import com.alexbryksin.ordersmicroservice.order.exceptions.OrderNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository


@Repository
class OrderMongoRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate) : OrderMongoRepository {
    override suspend fun insert(order: Order): Order = withContext(Dispatchers.IO) {
        val result = mongoTemplate.insert(OrderDocument.of(order)).awaitSingle()
        log.info("inserted order document: $result")
        result.toOrder()
    }

    override suspend fun update(order: Order): Order = withContext(Dispatchers.IO) {
        val query = Query.query(
            Criteria.where("id").`is`(order.id.toString()).and("version").`is`(order.version - 1)
        )

        val update = Update()
            .set("email", order.email)
            .set("address", order.address)
            .set("status", order.status)
            .set("version", order.version)
            .set("productItemEntities", order.productItemEntities)

        val options = FindAndModifyOptions.options().returnNew(true).upsert(false)
        val updatedOrderDocument = mongoTemplate.findAndModify(query, update, options, OrderDocument::class.java).awaitSingleOrNull()
            ?: throw OrderNotFoundException(order.id)

        log.info("updated order document: $updatedOrderDocument")
        updatedOrderDocument.toOrder()
    }

    override suspend fun getByID(id: String): Order = withContext(Dispatchers.IO) {
        mongoTemplate.findById(id, OrderDocument::class.java).awaitSingle().toOrder()
            .also { log.info("found order document: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderMongoRepositoryImpl::class.java)
    }
}