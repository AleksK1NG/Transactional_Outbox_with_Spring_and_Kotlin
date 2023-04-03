package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OutboxRepository : CoroutineCrudRepository<OutboxEvent, UUID>, OutboxPostgresRepository {
}