package com.alexbryksin.ordersmicroservice.order.consumer

import com.alexbryksin.ordersmicroservice.bankAccount.consumer.OutboxConsumer
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.InvalidVersionException
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.UnknownEventTypeException
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.order.domain.OutboxRecord
import com.alexbryksin.ordersmicroservice.order.events.*
import com.alexbryksin.ordersmicroservice.order.events.OrderCancelledEvent.Companion.ORDER_CANCELLED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCompletedEvent.Companion.ORDER_COMPLETED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderCreatedEvent.Companion.ORDER_CREATED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderPaidEvent.Companion.ORDER_PAID_EVENT
import com.alexbryksin.ordersmicroservice.order.events.OrderSubmittedEvent.Companion.ORDER_SUBMITTED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemAddedEvent.Companion.PRODUCT_ITEM_ADDED_EVENT
import com.alexbryksin.ordersmicroservice.order.events.ProductItemRemovedEvent.Companion.PRODUCT_ITEM_REMOVED_EVENT
import com.alexbryksin.ordersmicroservice.utils.kafkaUtils.getHeader
import com.alexbryksin.ordersmicroservice.utils.serializer.SerializationException
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
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
    private val orderEventProcessor: OrderEventProcessor,
    private val or: ObservationRegistry,
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
        coroutineScopeWithObservation("OrderConsumer.process", or) {
            try {
                processOutboxRecord(serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java))
                    .also { ack.acknowledge() }
                log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
            } catch (ex: Exception) {
                if (ex is SerializationException || ex is UnknownEventTypeException) {
                    log.error("commit not serializable or unknown record: ${String(consumerRecord.value())}")
                    ack.acknowledge()
                    return@coroutineScopeWithObservation
                }

                log.error("exception while processing record: ${ex.localizedMessage}")
                publishRetryTopic(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, 1)
            }
        }
    }


    @KafkaListener(groupId = "\${kafka.consumer-group-id:order-service-group-id}", topics = ["\${topics.retryTopic.name}"])
    fun processRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        coroutineScopeWithObservation("OrderConsumer.process", or) {
            try {
                processOutboxRecord(
                    serializer.deserialize(
                        consumerRecord.value(),
                        OutboxRecord::class.java
                    )
                ).also { ack.acknowledge() }
                log.info("committed retry record >>>>>>>>>>>>>> topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
            } catch (ex: Exception) {
                if (ex is SerializationException || ex is UnknownEventTypeException) {
                    ack.acknowledge()
                        .also { log.error("commit not serializable or unknown record: ${String(consumerRecord.value())}") }
                    return@coroutineScopeWithObservation
                }

                val retryCount = getHeader(RETRY_COUNT_HEADER, consumerRecord.headers()).toInt()

                if (ex is InvalidVersionException) {
                    log.info("processing retry invalid version exception >>>>>>>>>>>>>>>> ${ex.localizedMessage}")
                    publishRetryTopic(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount)
                    return@coroutineScopeWithObservation
                }

                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("retry count exceed, send record to dlq: ${consumerRecord.topic()}")
                    publishRetryTopic(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount + 1)
                    ack.acknowledge()
                    return@coroutineScopeWithObservation
                }

                log.error("exception while processing record: ${ex.localizedMessage}")
                publishRetryTopic(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, retryCount + 1)
            }
        }
    }


    private suspend fun publishRetryTopic(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        return mono { publishRetryRecord(topic, record, retryCount) }
            .retryWhen(Retry.backoff(PUBLISH_RETRY_COUNT, Duration.ofMillis(PUBLISH_RETRY_BACKOFF_DURATION_MILLIS))
                .filter { it is SerializationException })
            .awaitSingle()
    }


    private suspend fun publishRetryRecord(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        log.info("publishing retry record: ${String(record.value())}, retryCount: $retryCount")
        val headersMap = mutableMapOf(RETRY_COUNT_HEADER to retryCount.toString().toByteArray())
        record.headers().forEach { headersMap[it.key()] = it.value() }
        eventsPublisher.publishRetryRecord(topic, record.key(), record.value(), headersMap)
    }

    private suspend fun processOutboxRecord(outboxRecord: OutboxRecord) = when (outboxRecord.eventType) {
        ORDER_CREATED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                OrderCreatedEvent::class.java
            )
        )

        PRODUCT_ITEM_ADDED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                ProductItemAddedEvent::class.java
            )
        )

        PRODUCT_ITEM_REMOVED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                ProductItemRemovedEvent::class.java
            )
        )

        ORDER_PAID_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                OrderPaidEvent::class.java
            )
        )

        ORDER_CANCELLED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                OrderCancelledEvent::class.java
            )
        )

        ORDER_SUBMITTED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                OrderSubmittedEvent::class.java
            )
        )

        ORDER_COMPLETED_EVENT -> orderEventProcessor.on(
            serializer.deserialize(
                outboxRecord.data,
                OrderCompletedEvent::class.java
            )
        )

        else -> throw UnknownEventTypeException(outboxRecord.eventType)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
        const val RETRY_COUNT_HEADER = "retryCount"
        const val MAX_RETRY_COUNT = 5
        const val PUBLISH_RETRY_COUNT = 5L
        const val PUBLISH_RETRY_BACKOFF_DURATION_MILLIS = 3000L
    }
}