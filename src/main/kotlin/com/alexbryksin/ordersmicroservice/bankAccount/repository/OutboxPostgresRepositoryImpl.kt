package com.alexbryksin.ordersmicroservice.bankAccount.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.util.*


@Repository
class OutboxPostgresRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val txOp: TransactionalOperator
) : OutboxPostgresRepository {

    override suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long = withContext(Dispatchers.IO) {
        txOp.executeAndAwait {
            dbClient.sql("SELECT event_id FROM microservices.bank_accounts_outbox WHERE event_id = :eventId FOR UPDATE")
                .bind("eventId", id)
                .await()

            callback()

            dbClient.sql("DELETE FROM microservices.bank_accounts_outbox WHERE event_id = :eventId")
                .bind("eventId", id)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
                .also { log.info("outbox event with id: $it deleted") }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxPostgresRepositoryImpl::class.java)
    }
}