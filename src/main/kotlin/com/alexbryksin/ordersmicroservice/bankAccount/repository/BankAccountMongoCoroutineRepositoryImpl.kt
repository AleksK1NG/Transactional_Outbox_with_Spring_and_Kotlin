package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountDocument
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.BankAccountNotFoundException
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
class BankAccountMongoCoroutineRepositoryImpl(private val mongoTemplate: ReactiveMongoTemplate) : BankAccountMongoCoroutineRepository {

    override suspend fun findByID(id: String): BankAccountDocument = withContext(Dispatchers.IO) {
        mongoTemplate.findById(id, BankAccountDocument::class.java).awaitSingleOrNull() ?: throw BankAccountNotFoundException(id)
    }

    override suspend fun insert(bankAccountDocument: BankAccountDocument): BankAccountDocument = withContext(Dispatchers.IO) {
        mongoTemplate.insert(bankAccountDocument).awaitSingle()
    }

    override suspend fun update(bankAccountDocument: BankAccountDocument): BankAccountDocument = withContext(Dispatchers.IO) {
        val query = Query.query(
            Criteria.where("id").`is`(bankAccountDocument.id!!).and("version").`is`(bankAccountDocument.version - 1)
        )

        val update = Update()
            .set("email", bankAccountDocument.email)
            .set("address", bankAccountDocument.address)
            .set("firstName", bankAccountDocument.firstName)
            .set("lastName", bankAccountDocument.lastName)
            .set("phone", bankAccountDocument.phone)
            .set("balance", bankAccountDocument.balance)
            .set("currency", bankAccountDocument.currency)
//            .set("updatedAt", bankAccountDocument.updatedAt)
            .set("version", bankAccountDocument.version)

        val options = FindAndModifyOptions.options().returnNew(true).upsert(false)
        mongoTemplate.findAndModify(query, update, options, BankAccountDocument::class.java).awaitSingleOrNull() ?: throw BankAccountNotFoundException(
            bankAccountDocument.id!!
        )
    }

    override suspend fun deleteByID(id: String) {
        val query = Query.query(Criteria.where("_id").`is`(id))
        if (!mongoTemplate.exists(query, BankAccountDocument::class.java).awaitSingle()) throw BankAccountNotFoundException(id)
        mongoTemplate.remove(query, BankAccountDocument::class.java).awaitSingle()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountMongoCoroutineRepository::class.java)
    }
}