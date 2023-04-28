package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import com.alexbryksin.ordersmicroservice.order.domain.of
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.util.*

class OutboxBaseRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val txOp: TransactionalOperator,
    private val or: ObservationRegistry,
) : OutboxBaseRepository {

    override suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long = coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_BY_ID, or) {
        withTimeout(DELETE_OUTBOX_RECORD_TIMEOUT_MILLIS) {
            txOp.executeAndAwait {

                callback()

                dbClient.sql("DELETE FROM microservices.outbox_table WHERE event_id = :eventId")
                    .bind("eventId", id)
                    .fetch()
                    .rowsUpdated()
                    .awaitSingle()
                    .also { log.info("outbox event with id: $it deleted") }
            }
        }
    }

    override suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxRecord: OutboxRecord) -> Unit) =
        coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_WITH_LOCK, or) {
            withTimeout(DELETE_OUTBOX_RECORD_TIMEOUT_MILLIS) {
                txOp.executeAndAwait {
                    log.info("starting delete outbox events")

                    dbClient.sql("SELECT * FROM microservices.outbox_table ORDER BY timestamp ASC LIMIT 10 FOR UPDATE SKIP LOCKED")
                        .map { row, _ -> OutboxRecord.of(row) }
                        .flow()
                        .onEach {
                            log.info("deleting outboxEvent with id: ${it.eventId}")

                            callback(it)

                            dbClient.sql("DELETE FROM microservices.outbox_table WHERE event_id = :eventId")
                                .bind("eventId", it.eventId!!)
                                .fetch()
                                .rowsUpdated()
                                .awaitSingle()

                            log.info("outboxEvent with id: ${it.eventId} published and deleted")
                        }
                        .collect()
                    log.info("complete delete outbox events")
                }
            }
        }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxBaseRepositoryImpl::class.java)
        private const val DELETE_OUTBOX_RECORD_TIMEOUT_MILLIS = 3000L

        private const val DELETE_OUTBOX_RECORD_WITH_LOCK = "OutboxBaseRepository.deleteOutboxRecordsWithLock"
        private const val DELETE_OUTBOX_RECORD_BY_ID = "OutboxBaseRepository.deleteOutboxRecordByID"
    }
}