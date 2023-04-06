package com.alexbryksin.ordersmicroservice.bankAccount.consumer

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import com.alexbryksin.ordersmicroservice.bankAccount.events.BalanceDepositedEvent
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.utils.serializer.SerializationException
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import reactor.util.retry.Retry
import java.math.BigDecimal
import java.time.Duration


@Component
class OutboxConsumer(
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer,
    private val eventsPublisher: EventsPublisher
) {

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.bankAccountCreated.name}", "\${topics.balanceDeposited.name}", "\${topics.balanceWithdrawn.name}", "\${topics.emailChanged.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun consume(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process record: ${String(consumerRecord.value())}")
            val outboxEvent = serializer.deserialize(consumerRecord.value(), OutboxEvent::class.java)
            log.info("serialized outbox event: $outboxEvent")
            if (outboxEvent.eventType == BalanceDepositedEvent.BALANCE_DEPOSITED_EVENT) {
                val balanceDepositedEvent = serializer.deserialize(outboxEvent.data, BalanceDepositedEvent::class.java)
                log.info("deserialized balance deposited event: $balanceDepositedEvent")

                if (balanceDepositedEvent.amount == BigDecimal.valueOf(10)) throw RuntimeException("exception amount")
            }
            ack.acknowledge()
            log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                log.error("commit not serializable record: ${String(consumerRecord.value())}")
                ack.acknowledge()
            }

            log.error("exception while processing record: ${ex.localizedMessage}")

            mono { publishRetryRecord(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord.value(), consumerRecord.headers(), 1) }
                .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                .awaitSingle()
        }
    }

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.retryTopic.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun consumeRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process retry record >>>>>>>: ${String(consumerRecord.value())}")

            val outboxEvent = serializer.deserialize(consumerRecord.value(), OutboxEvent::class.java)
            log.info("serialized outbox event: $outboxEvent")

            ack.acknowledge()
            log.info("committed retry record >>>>>>>>>>>>>> topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                log.error("commit not serializable record: ${String(consumerRecord.value())}")
                ack.acknowledge()
                return@runBlocking
            }

            val retryCount = getHeader("retryCount", consumerRecord.headers()).toInt()
            if (retryCount > 5) {
                log.error("retry count exceed, send record to dlq: ${consumerRecord.topic()}")
                mono { publishRetryRecord(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord.value(), consumerRecord.headers(), retryCount + 1) }
                    .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                    .awaitSingle()
                ack.acknowledge()
                return@runBlocking
            }

            log.error("exception while processing record: ${ex.localizedMessage}")
            mono { publishRetryRecord(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord.value(), consumerRecord.headers(), retryCount + 1) }
                .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
                .awaitSingle()
        }
    }

    fun getHeader(name: String, headers: Headers): String {
        val headersMap = headers.associateBy({ it.key() }, { it.value() })
        return String(headersMap[name] ?: byteArrayOf())
    }

    private suspend fun publishRetryRecord(topic: String, data: ByteArray, headers: Headers, retryCount: Int) {
        log.info("publishing retry record: ${String(data)}, retryCount: $retryCount")
        val headersMap = mutableMapOf("retryCount" to retryCount.toString().toByteArray())
        headers.forEach { headersMap[it.key()] = it.value() }
        eventsPublisher.publishRetryRecord(topic, data, headersMap)
    }


    private suspend fun publishRetryRecord(topic: String, consumerRecord: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        log.info("publishing retry record: ${String(consumerRecord.value())}, retryCount: $retryCount")
        val headersMap = mutableMapOf("retryCount" to retryCount.toString().toByteArray())
        consumerRecord.headers().forEach { headersMap[it.key()] = it.value() }
        eventsPublisher.publish(topic, consumerRecord.value(), headersMap)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}