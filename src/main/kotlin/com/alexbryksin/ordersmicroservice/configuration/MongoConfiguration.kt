package com.alexbryksin.ordersmicroservice.configuration

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountDocument
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

        val exists = mongoTemplate.collectionExists(BankAccountDocument::class.java).awaitSingle()
        if (!exists) {
            val productsCollection = mongoTemplate.createCollection(BankAccountDocument::class.java).awaitSingle()
            log.info("bankAccounts collection: ${productsCollection.namespace.fullName}")
        }

        val indexInfo = mongoTemplate.indexOps(BankAccountDocument::class.java).indexInfo.collectList().awaitSingle()
        log.info("indexInfo: $indexInfo")

        val emailIndex = mongoTemplate.indexOps(BankAccountDocument::class.java)
            .ensureIndex(Index().sparse().on("email", Sort.DEFAULT_DIRECTION).unique()).awaitSingle()
        log.info("emailIndex: $emailIndex")

        log.info("mongodb initialized")
    }


    companion object {
        private val log = LoggerFactory.getLogger(MongoConfiguration::class.java)
    }
}