package com.alexbryksin.ordersmicroservice.order.consumer

import com.alexbryksin.ordersmicroservice.bankAccount.consumer.OutboxConsumer
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.InvalidVersionException
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import com.alexbryksin.ordersmicroservice.utils.kafkaUtils.getHeader
import com.alexbryksin.ordersmicroservice.utils.serializer.SerializationException
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import reactor.util.retry.Retry
import java.time.Duration


@Component
class OrderConsumer(
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer,
    private val eventsPublisher: EventsPublisher,
) {

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:order-service-group-id}",
        topics = [
            "\${topics.orderCreated.name}",
            "\${topics.productAdded.name}",
            "\${topics.productRemoved.name}",
            "\${topics.orderPaid.name}",
            "\${topics.orderCancelled.name}",
            "\${topics.orderSubmitted.name}",
            "\${topics.orderCompleted.name}",
        ],
    )
    fun process(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>) = runBlocking {
        try {
            log.info("process record: ${String(consumerRecord.value())}")
            val outboxRecord = serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java)
            log.info("deserialized outbox record: $outboxRecord")
            ack.acknowledge()
            log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                log.error("commit not serializable record: ${String(consumerRecord.value())}")
                ack.acknowledge()
                return@runBlocking
            }

            log.error("exception while processing record: ${ex.localizedMessage}")

            mono { publishRetryRecord(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, 1) }
                .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                .awaitSingle()
        }
    }


    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:order-service-group-id}",
        topics = ["\${topics.retryTopic.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun processRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process retry record >>>>>>>: ${String(consumerRecord.value())}, retryCount: ${getHeader("retryCount", consumerRecord.headers())}")

            val outboxRecord = serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java)
            log.info("deserialized outbox record: $outboxRecord")

            ack.acknowledge()
            log.info("committed retry record >>>>>>>>>>>>>> topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                ack.acknowledge().also {log.error("commit not serializable record: ${String(consumerRecord.value())}")  }
                return@runBlocking
            }

            val retryCount = getHeader("retryCount", consumerRecord.headers()).toInt()
            if (ex is InvalidVersionException) {
                log.info("processing retry invalid version exception")
                mono { publishRetryRecord(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount) }
                    .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                    .awaitSingle()
            }

            if (retryCount > 5) {
                log.error("retry count exceed, send record to dlq: ${consumerRecord.topic()}")
                mono { publishRetryRecord(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount + 1) }
                    .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                    .awaitSingle()
                ack.acknowledge()
                return@runBlocking
            }

            log.error("exception while processing record: ${ex.localizedMessage}")
            mono { publishRetryRecord(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, retryCount + 1) }
                .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                .awaitSingle()
        }
    }




    private suspend fun publishRetryRecord(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        log.info("publishing retry record: ${String(record.value())}, retryCount: $retryCount")
        val headersMap = mutableMapOf("retryCount" to retryCount.toString().toByteArray())
        record.headers().forEach { headersMap[it.key()] = it.value() }
        eventsPublisher.publishRetryRecord(topic, record.key(), record.value(), headersMap)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}