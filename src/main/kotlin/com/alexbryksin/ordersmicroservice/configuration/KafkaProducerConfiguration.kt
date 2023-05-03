package com.alexbryksin.ordersmicroservice.configuration

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaProducerConfiguration(
    @Value(value = "\${spring.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
) {

    private fun senderProps(): Map<String, Any> {
        return hashMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 5,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to 120000,
            ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 1068576,
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
        )
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, ByteArray> {
        return DefaultKafkaProducerFactory(senderProps())
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, ByteArray>): KafkaTemplate<String, ByteArray> {
        val kafkaTemplate = KafkaTemplate(producerFactory)
        kafkaTemplate.setObservationEnabled(true)
        kafkaTemplate.setMicrometerEnabled(true)
        return kafkaTemplate
    }
}