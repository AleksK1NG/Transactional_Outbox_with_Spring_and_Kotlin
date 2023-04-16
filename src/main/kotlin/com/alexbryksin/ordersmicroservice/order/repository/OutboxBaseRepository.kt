package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface OutboxBaseRepository {
    suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long
    suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxRecord: OutboxRecord) -> Unit)
}