package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.LocalDateTime
import java.util.*


@Repository
class OutboxPostgresRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val txOp: TransactionalOperator
) : OutboxPostgresRepository {

    override suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long = withContext(Dispatchers.IO) {
        withTimeout(3000) {
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
    }

    override suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxEvent: OutboxEvent) -> Unit) = withContext(Dispatchers.IO) {
        withTimeout(3000) {
            txOp.executeAndAwait {
                log.info("starting delete outbox events")
                dbClient.sql("SELECT * FROM microservices.bank_accounts_outbox ORDER BY timestamp ASC LIMIT 10 FOR UPDATE SKIP LOCKED")
                    .map { row, _ ->
                        try {
                            OutboxEvent(
                                eventId = row["event_id", UUID::class.java],
                                aggregateId = row["aggregate_id", UUID::class.java],
                                eventType = row["event_type", String::class.java],
                                data = row["data", ByteArray::class.java] ?: byteArrayOf(),
                                version = row["version", String::class.java]?.toLong() ?: 0,
                                timestamp = row["timestamp", LocalDateTime::class.java],
                            )
                        } catch (e: Exception) {
                            log.error(e.localizedMessage)
                            throw e
                        }
                    }
                    .flow()
                    .collectIndexed { _, outboxEvent ->
                        log.info("deleting outboxEvent with id: ${outboxEvent.eventId}")

                        callback(outboxEvent)

                        dbClient.sql("DELETE FROM microservices.bank_accounts_outbox WHERE event_id = :eventId")
                            .bind("eventId", outboxEvent.eventId!!)
                            .fetch()
                            .rowsUpdated()
                            .awaitSingle()
                        log.info("outboxEvent with id: ${outboxEvent.eventId} published and deleted")
                    }
                log.info("complete delete outbox events")
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxPostgresRepositoryImpl::class.java)
    }
}