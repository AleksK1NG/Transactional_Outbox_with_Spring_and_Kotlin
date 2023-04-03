package com.alexbryksin.ordersmicroservice.eventPublisher

interface EventsPublisher {
    suspend fun publish(topic: String?, data: Any)
    suspend fun publish(topic: String?, data: Any, headers: Map<String, ByteArray>)
}