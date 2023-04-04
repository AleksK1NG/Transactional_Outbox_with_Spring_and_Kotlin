package com.alexbryksin.ordersmicroservice.bankAccount.consumer

import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@Component
class OutboxConsumer(
    private val objectMapper: ObjectMapper,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration
) {

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.bankAccountCreated.name}", "\${topics.balanceDeposited.name}", "\${topics.balanceWithdrawn.name}", "\${topics.emailChanged.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun consume(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>) = runBlocking {
        log.info("process record: ${String(consumerRecord.value())}")
        ack.acknowledge()
        log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
    }


    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}