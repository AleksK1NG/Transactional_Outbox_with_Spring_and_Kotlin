package com.alexbryksin.ordersmicroservice.configuration

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.CommandLineRunner
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Component
import reactor.util.Loggers

@Component
class KafkaTopicsInitializer(
    private val kafkaAdmin: KafkaAdmin,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaTopicsConfiguration.getTopics().filterNotNull().forEach { createOrModifyTopic(it) }
        log.info("topics created or modified")
    }

    private fun createOrModifyTopic(topicConfiguration: TopicConfiguration) {
        return try {
            val topic = NewTopic(topicConfiguration.name, topicConfiguration.partitions, topicConfiguration.replication.toShort())
            kafkaAdmin.createOrModifyTopics(topic).also { log.info("(KafkaTopicsInitializer) created or modified topic: $topic") }
        } catch (ex: Exception) {
            log.error("KafkaTopicsInitializer createTopic", ex)
        }
    }

    companion object {
        private val log = Loggers.getLogger(KafkaTopicsInitializer::class.java)
    }
}