package com.alexbryksin.ordersmicroservice.eventPublisher

import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component


@Component
class KafkaEventsPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    private val objectMapper: ObjectMapper,
    private val or: ObservationRegistry
) : EventsPublisher {

    override suspend fun publish(topic: String?, data: Any) = withContext(Dispatchers.IO) {
        coroutineScopeWithObservation("EventsPublisher.publish", or) {
            val msg = ProducerRecord<String, ByteArray>(topic, objectMapper.writeValueAsBytes(data))
            val result = kafkaTemplate.send(msg).await()
            log.info("publish sendResult: $result")
        }
    }

    override suspend fun publish(topic: String?, key: String, data: Any) = withContext(Dispatchers.IO) {
        coroutineScopeWithObservation("EventsPublisher.publish", or) {
            val msg = ProducerRecord(topic, key, objectMapper.writeValueAsBytes(data))
            val result = kafkaTemplate.send(msg).await()
            log.info("publish sendResult: $result")
        }
    }

    override suspend fun publish(topic: String?, data: Any, headers: Map<String, ByteArray>) =
        withContext(Dispatchers.IO) {
            coroutineScopeWithObservation("EventsPublisher.publish", or) {
                val msg = ProducerRecord<String, ByteArray>(topic, objectMapper.writeValueAsBytes(data))
                headers.forEach { (key, value) -> msg.headers().add(key, value) }
                val result = kafkaTemplate.send(msg).await()
                log.info("publish sendResult: $result")
            }
        }

    override suspend fun publish(topic: String?, key: String, data: Any, headers: Map<String, ByteArray>) =
        withContext(Dispatchers.IO) {
            coroutineScopeWithObservation("EventsPublisher.publish", or) {
                val msg = ProducerRecord(topic, key, objectMapper.writeValueAsBytes(data))
                headers.forEach { (key, value) -> msg.headers().add(key, value) }
                val result = kafkaTemplate.send(msg).await()
                log.info("publish sendResult: $result")
            }
        }

    override suspend fun publishRetryRecord(topic: String?, data: ByteArray, headers: Map<String, ByteArray>) =
        withContext(Dispatchers.IO) {
            coroutineScopeWithObservation("EventsPublisher.publish", or) {
                val msg = ProducerRecord<String, ByteArray>(topic, data)
                headers.forEach { (key, value) -> msg.headers().add(key, value) }
                val result = kafkaTemplate.send(msg).await()
                log.info("publish sendResult: $result")
            }
        }

    override suspend fun publishRetryRecord(
        topic: String?,
        key: String,
        data: ByteArray,
        headers: Map<String, ByteArray>
    ) = withContext(Dispatchers.IO) {
        coroutineScopeWithObservation("EventsPublisher.publish", or) {
            val msg = ProducerRecord(topic, key, data)
            headers.forEach { (key, value) -> msg.headers().add(key, value) }
            val result = kafkaTemplate.send(msg).await()
            log.info("publish sendResult: $result")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaEventsPublisher::class.java)
    }
}