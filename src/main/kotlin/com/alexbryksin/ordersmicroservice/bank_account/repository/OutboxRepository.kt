package com.alexbryksin.ordersmicroservice.bank_account.repository

import com.alexbryksin.ordersmicroservice.bank_account.domain.OutboxEvent
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface OutboxRepository : CoroutineCrudRepository<OutboxEvent, UUID> {
}