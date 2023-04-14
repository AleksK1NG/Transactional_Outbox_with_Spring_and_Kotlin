package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface OutboxBaseRepository {
    suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long
    suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxEvent: OutboxEvent) -> Unit)
}