package com.alexbryksin.ordersmicroservice.bankAccount.consumer

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.utils.serializer.SerializationException
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@Component
class OutboxConsumer(
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer
) {

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.bankAccountCreated.name}", "\${topics.balanceDeposited.name}", "\${topics.balanceWithdrawn.name}", "\${topics.emailChanged.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun consume(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>) = runBlocking {
        try {
            log.info("process record: ${String(consumerRecord.value())}")
            val outboxEvent = serializer.deserialize(consumerRecord.value(), OutboxEvent::class.java)
            log.info("serialized outbox event: $outboxEvent")
            ack.acknowledge()
            log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                log.error("commit not serializable record: ${String(consumerRecord.value())}")
                ack.acknowledge()
            }
            log.error("exception while processing record: ${ex.localizedMessage}")
        }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}