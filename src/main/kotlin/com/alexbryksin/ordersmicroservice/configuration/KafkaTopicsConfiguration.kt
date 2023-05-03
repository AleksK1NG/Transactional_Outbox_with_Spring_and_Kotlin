package com.alexbryksin.ordersmicroservice.configuration

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import reactor.util.Loggers

@Configuration
@ConfigurationProperties(prefix = "topics")
class KafkaTopicsConfiguration {

    var retryTopic: TopicConfiguration = TopicConfiguration()
    var deadLetterQueue: TopicConfiguration = TopicConfiguration()

    var orderCreated: TopicConfiguration = TopicConfiguration()
    var orderPaid: TopicConfiguration = TopicConfiguration()
    var orderCancelled: TopicConfiguration = TopicConfiguration()
    var orderSubmitted: TopicConfiguration = TopicConfiguration()
    var orderCompleted: TopicConfiguration = TopicConfiguration()
    var productAdded: TopicConfiguration = TopicConfiguration()
    var productRemoved: TopicConfiguration = TopicConfiguration()


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