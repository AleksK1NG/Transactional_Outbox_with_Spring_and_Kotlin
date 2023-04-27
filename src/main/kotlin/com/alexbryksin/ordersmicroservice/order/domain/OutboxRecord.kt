package com.alexbryksin.ordersmicroservice.order.domain

import io.r2dbc.spi.Row
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Table(schema = "microservices", name = "outbox_table")
data class OutboxRecord(
    @Id @Column(EVENT_ID) var eventId: UUID? = null,
    @Column(EVENT_TYPE) var eventType: String?,
    @Column(AGGREGATE_ID) var aggregateId: String?,
    @Column(DATA) var data: ByteArray = byteArrayOf(),
    @Column(VERSION) var version: Long = 0,
    @Column(TIMESTAMP) var timestamp: LocalDateTime?,
) {
    companion object {
        const val EVENT_ID = "event_id"
        const val EVENT_TYPE = "event_type"
        const val AGGREGATE_ID = "aggregate_id"
        const val DATA = "data"
        const val VERSION = "version"
        const val TIMESTAMP = "timestamp"
    }
}

fun OutboxRecord.Companion.of(row: Row): OutboxRecord = OutboxRecord(
    eventId = row[EVENT_ID, UUID::class.java],
    aggregateId = row[AGGREGATE_ID, String::class.java],
    eventType = row[EVENT_TYPE, String::class.java],
    data = row[DATA, ByteArray::class.java] ?: byteArrayOf(),
    version = row[VERSION, BigInteger::class.java]?.toLong() ?: 0,
    timestamp = row[TIMESTAMP, LocalDateTime::class.java],
)
