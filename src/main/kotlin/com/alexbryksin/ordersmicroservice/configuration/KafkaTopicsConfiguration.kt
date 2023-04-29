package com.alexbryksin.ordersmicroservice.configuration

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import reactor.util.Loggers

@Configuration
@ConfigurationProperties(prefix = "topics")
class KafkaTopicsConfiguration {

    var retryTopic: TopicConfiguration? = null
    var deadLetterQueue: TopicConfiguration? = null

    var orderCreated: TopicConfiguration? = null
    var orderPaid: TopicConfiguration? = null
    var orderCancelled: TopicConfiguration? = null
    var orderSubmitted: TopicConfiguration? = null
    var orderCompleted: TopicConfiguration? = null
    var productAdded: TopicConfiguration? = null
    var productRemoved: TopicConfiguration? = null


    fun getTopics() = listOf(
        retryTopic,
        deadLetterQueue,
        orderCreated,
        productRemoved,
        productAdded,
        orderCancelled,
        orderPaid,
        orderSubmitted,
        orderCompleted,
    )

    @PostConstruct
    fun logConfigProperties() {
        log.info("KafkaTopicsConfiguration created topics: ${getTopics()}")
    }

    companion object {
        private val log = Loggers.getLogger(KafkaTopicsConfiguration::class.java)
    }
}