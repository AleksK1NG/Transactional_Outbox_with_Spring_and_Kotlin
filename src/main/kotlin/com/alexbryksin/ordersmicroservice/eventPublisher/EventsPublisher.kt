package com.alexbryksin.ordersmicroservice.eventPublisher

interface EventsPublisher {
    suspend fun publish(topic: String?, data: Any)
    suspend fun publish(topic: String?, key: String, data: Any)
    suspend fun publish(topic: String?, data: Any, headers: Map<String, ByteArray>)
    suspend fun publish(topic: String?, key: String, data: Any, headers: Map<String, ByteArray>)
    suspend fun publishRetryRecord(topic: String?, data: ByteArray, headers: Map<String, ByteArray>)
    suspend fun publishRetryRecord(topic: String?, key: String, data: ByteArray, headers: Map<String, ByteArray>)
}