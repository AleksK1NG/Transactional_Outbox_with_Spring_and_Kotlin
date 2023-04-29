package com.alexbryksin.ordersmicroservice.configuration

import com.alexbryksin.ordersmicroservice.order.domain.OrderDocument
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class MongoConfiguration(private val mongoTemplate: ReactiveMongoTemplate) {

    @PostConstruct
    fun init() = runBlocking {
        val mongoDatabase = mongoTemplate.mongoDatabase.awaitSingle()
        log.info("mongoDatabase: ${mongoDatabase.name}")
        val collectionsList = mongoTemplate.collectionNames.collectList().awaitSingle()
        log.info("collectionsList: $collectionsList")


        val orderCollectionExists = mongoTemplate.collectionExists(OrderDocument::class.java).awaitSingle()
        if (!orderCollectionExists) {
            val ordersCollection = mongoTemplate.createCollection(OrderDocument::class.java).awaitSingle()
            log.info("order collection: ${ordersCollection.namespace.fullName}")
        }

        val ordersIndexInfo = mongoTemplate.indexOps(OrderDocument::class.java).indexInfo.collectList().awaitSingle()
        log.info("ordersIndexInfo: $ordersIndexInfo")

        val orderEmailIndex = mongoTemplate.indexOps(OrderDocument::class.java)
            .ensureIndex(Index().sparse().on("email", Sort.DEFAULT_DIRECTION).unique()).awaitSingle()
        log.info("orderEmailIndex: $orderEmailIndex")

        log.info("mongodb initialized")
    }


    companion object {
        private val log = LoggerFactory.getLogger(MongoConfiguration::class.java)
    }
}