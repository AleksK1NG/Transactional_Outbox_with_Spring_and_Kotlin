package com.alexbryksin.ordersmicroservice.order.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*


@Table(schema = "microservices", name = "outbox_table")
data class OutboxRecord(
    @Id @Column("event_id") var eventId: UUID? = null,
    @Column("event_type") var eventType: String?,
    @Column("aggregate_id") var aggregateId: String?,
    @Column("data") var data: ByteArray = byteArrayOf(),
    @Column("version") var version: Long = 0,
    @Column("timestamp") var timestamp: LocalDateTime?,
)
