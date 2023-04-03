package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface OutboxRepository : CoroutineCrudRepository<OutboxEvent, UUID> {
}