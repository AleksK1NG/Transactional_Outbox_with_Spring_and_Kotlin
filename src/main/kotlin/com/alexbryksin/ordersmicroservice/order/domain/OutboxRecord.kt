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
    @Id @Column("event_id") var eventId: UUID? = null,
    @Column("event_type") var eventType: String?,
    @Column("aggregate_id") var aggregateId: String?,
    @Column("data") var data: ByteArray = byteArrayOf(),
    @Column("version") var version: Long = 0,
    @Column("timestamp") var timestamp: LocalDateTime?,
) {
    companion object
}

fun OutboxRecord.Companion.of(row: Row) = OutboxRecord(
    eventId = row["event_id", UUID::class.java],
    aggregateId = row["aggregate_id", String::class.java],
    eventType = row["event_type", String::class.java],
    data = row["data", ByteArray::class.java] ?: byteArrayOf(),
    version = row["version", BigInteger::class.java]?.toLong() ?: 0,
    timestamp = row["timestamp", LocalDateTime::class.java],
)
