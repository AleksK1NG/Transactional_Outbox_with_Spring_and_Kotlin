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
    private val orderEventProcessor: OrderEventProcessor
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
            val deserializedEvent = deserializeEventByType(outboxRecord)
            log.info("deserializedEvent: $deserializedEvent")
            process(deserializedEvent)
            ack.acknowledge()
            log.info("committed record topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException || ex is UnknownEventTypeException) {
                log.error("commit not serializable or unknown record: ${String(consumerRecord.value())}")
                ack.acknowledge()
                return@runBlocking
            }

            log.error("exception while processing record: ${ex.localizedMessage}")
            publishRetryTopic(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, 1)
        }
    }


    @KafkaListener(groupId = "\${kafka.consumer-group-id:order-service-group-id}", topics = ["\${topics.retryTopic.name}"])
    fun processRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        try {
            log.info("process retry record >>>>>>>: ${String(consumerRecord.value())}, retryCount: ${getHeader("retryCount", consumerRecord.headers())}")

            val outboxRecord = serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java)
            log.info("deserialized outbox record: $outboxRecord")
            val deserializedEvent = deserializeEventByType(outboxRecord)
            log.info("deserializedEvent: $deserializedEvent")

            ack.acknowledge()
            log.info("committed retry record >>>>>>>>>>>>>> topic: ${consumerRecord.topic()} offset: ${consumerRecord.offset()} partition: ${consumerRecord.partition()}")
        } catch (ex: Exception) {
            if (ex is SerializationException|| ex is UnknownEventTypeException) {
                ack.acknowledge().also { log.error("commit not serializable or unknown record: ${String(consumerRecord.value())}") }
                return@runBlocking
            }

            val retryCount = getHeader("retryCount", consumerRecord.headers()).toInt()

            if (ex is InvalidVersionException) {
                log.info("processing retry invalid version exception >>>>>>>>>>>>>>>> ${ex.localizedMessage}")
                publishRetryTopic(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount)
                return@runBlocking
            }

            if (retryCount > 5) {
                log.error("retry count exceed, send record to dlq: ${consumerRecord.topic()}")
                publishRetryTopic(kafkaTopicsConfiguration.deadLetterQueue?.name!!, consumerRecord, retryCount + 1)
                ack.acknowledge()
                return@runBlocking
            }

            log.error("exception while processing record: ${ex.localizedMessage}")
            publishRetryTopic(kafkaTopicsConfiguration.retryTopic?.name!!, consumerRecord, retryCount + 1)
        }
    }


    private suspend fun publishRetryTopic(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        return mono { publishRetryRecord(topic, record, retryCount) }
            .retryWhen(Retry.backoff(5, Duration.ofMillis(3000)).filter { it is SerializationException })
            .awaitSingle()
    }


    private suspend fun publishRetryRecord(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) {
        log.info("publishing retry record: ${String(record.value())}, retryCount: $retryCount")
        val headersMap = mutableMapOf("retryCount" to retryCount.toString().toByteArray())
        record.headers().forEach { headersMap[it.key()] = it.value() }
        eventsPublisher.publishRetryRecord(topic, record.key(), record.value(), headersMap)
    }

    private suspend fun process(event: Any) = when(event) {
        is OrderCreatedEvent -> orderEventProcessor.on(event)
        is ProductItemAddedEvent -> orderEventProcessor.on(event)
        is ProductItemRemovedEvent -> orderEventProcessor.on(event)
        is OrderPaidEvent -> orderEventProcessor.on(event)
        is OrderCancelledEvent -> orderEventProcessor.on(event)
        is OrderSubmittedEvent -> orderEventProcessor.on(event)
        is OrderCompletedEvent -> orderEventProcessor.on(event)
        else -> throw UnknownEventTypeException(event.toString())
    }


    private fun deserializeEventByType(outboxRecord: OutboxRecord): Any = when (outboxRecord.eventType) {
        ORDER_CREATED_EVENT -> serializer.deserialize(outboxRecord.data, OrderCreatedEvent::class.java)
        PRODUCT_ITEM_ADDED_EVENT -> serializer.deserialize(outboxRecord.data, ProductItemAddedEvent::class.java)
        PRODUCT_ITEM_REMOVED_EVENT -> serializer.deserialize(outboxRecord.data, ProductItemRemovedEvent::class.java)
        ORDER_PAID_EVENT -> serializer.deserialize(outboxRecord.data, OrderPaidEvent::class.java)
        ORDER_CANCELLED_EVENT -> serializer.deserialize(outboxRecord.data, OrderCancelledEvent::class.java)
        ORDER_SUBMITTED_EVENT -> serializer.deserialize(outboxRecord.data, OrderSubmittedEvent::class.java)
        ORDER_COMPLETED_EVENT -> serializer.deserialize(outboxRecord.data, OrderCompletedEvent::class.java)
        else -> throw UnknownEventTypeException(outboxRecord.eventType)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxConsumer::class.java)
    }
}