package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderOutboxRepository : CoroutineCrudRepository<OutboxRecord, UUID>, OutboxBaseRepository