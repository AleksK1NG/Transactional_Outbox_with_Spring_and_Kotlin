package com.alexbryksin.ordersmicroservice.bankAccount.domain

import io.r2dbc.spi.Row
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Table(schema = "microservices", name = "bank_accounts_outbox")
data class OutboxEvent(
    @Id @Column("event_id") var eventId: UUID?,
    @Column("aggregate_id") var aggregateId: UUID?,
    @Column("event_type") var eventType: String?,
    @Column("data") var data: ByteArray = byteArrayOf(),
    @Column("version") var version: Long = 0,
    @Column("timestamp") var timestamp: LocalDateTime?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutboxEvent

        if (eventId != other.eventId) return false
        return aggregateId == other.aggregateId
    }

    override fun hashCode(): Int {
        var result = eventId?.hashCode() ?: 0
        result = 31 * result + (aggregateId?.hashCode() ?: 0)
        return result
    }

    companion object
}


fun OutboxEvent.Companion.fromRow(row: Row) = OutboxEvent(
    eventId = row["event_id", UUID::class.java],
    aggregateId = row["aggregate_id", UUID::class.java],
    eventType = row["event_type", String::class.java],
    data = row["data", ByteArray::class.java] ?: byteArrayOf(),
    version = row["version", BigInteger::class.java]?.toLong() ?: 0,
    timestamp = row["timestamp", LocalDateTime::class.java],
)