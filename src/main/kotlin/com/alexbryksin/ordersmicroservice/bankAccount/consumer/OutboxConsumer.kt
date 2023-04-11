package com.alexbryksin.ordersmicroservice.bankAccount.consumer

import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import com.alexbryksin.ordersmicroservice.bankAccount.events.*
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.InvalidVersionException
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.UnknownEventTypeException
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
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
class OutboxConsumer(
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer,
    private val eventsPublisher: EventsPublisher,
    private val bankAccountEventProcessor: BankAccountEventProcessor
) {


    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.bankAccountCreated.name}", "\${topics.balanceDeposited.name}", "\${topics.balanceWithdrawn.name}", "\${topics.emailChanged.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun process(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process record: ${String(consumerRecord.value())}")
            handle(deserializeEventByType(serializer.deserialize(consumerRecord.value(), OutboxEvent::class.java)))
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
        groupId = "\${kafka.consumer-group-id:bank-accounts-service-group-id}",
        topics = ["\${topics.retryTopic.name}"],
//        errorHandler = "consumerExceptionHandler"
    )
    fun processRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process retry record >>>>>>>: ${String(consumerRecord.value())}, retryCount: ${getHeader("retryCount", consumerRecord.headers())}")
            handle(deserializeEventByType(serializer.deserialize(consumerRecord.value(), OutboxEvent::class.java)))
            ack.acknowledge()
            log.info("committed retry record >>>>>>>>>>>>>> topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException) {
                log.error("commit not serializable record: ${String(consumerRecord.value())}")
                ack.acknowledge()
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


    suspend fun handle(event: Any) = when (event) {
        is BankAccountCreatedEvent -> bankAccountEventProcessor.on(event)
        is BalanceDepositedEvent -> bankAccountEventProcessor.on(event)
        is BalanceWithdrawnEvent -> bankAccountEventProcessor.on(event)
        is EmailChangedEvent -> bankAccountEventProcessor.on(event)
        else -> throw UnknownEventTypeException(event.toString())
    }


    fun deserializeEventByType(outboxEvent: OutboxEvent): Any = when (outboxEvent.eventType) {
        BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_EVENT -> serializer.deserialize(outboxEvent.data, BankAccountCreatedEvent::class.java)
        BalanceDepositedEvent.BALANCE_DEPOSITED_EVENT -> serializer.deserialize(outboxEvent.data, BalanceDepositedEvent::class.java)
        BalanceWithdrawnEvent.BALANCE_WITHDRAWN_EVENT -> serializer.deserialize(outboxEvent.data, BalanceWithdrawnEvent::class.java)
        EmailChangedEvent.EMAIL_CHANGED_EVENT -> serializer.deserialize(outboxEvent.data, EmailChangedEvent::class.java)
        else -> throw UnknownEventTypeException(outboxEvent.eventType)
    }


    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}