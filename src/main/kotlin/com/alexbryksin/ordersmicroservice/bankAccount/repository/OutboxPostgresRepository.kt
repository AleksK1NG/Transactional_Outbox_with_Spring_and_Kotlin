package com.alexbryksin.ordersmicroservice.bankAccount.repository

import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface OutboxPostgresRepository {
    suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long
    suspend fun deleteOutboxRecordsWithLock(callback: suspend () -> Unit)
}