package com.alexbryksin.ordersmicroservice.configuration

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import reactor.util.Loggers

@Configuration
@ConfigurationProperties(prefix = "topics")
class KafkaTopicsConfiguration {

    var bankAccountCreated: TopicConfiguration? = null
    var balanceDeposited: TopicConfiguration? = null
    var balanceWithdrawn: TopicConfiguration? = null
    var emailChanged: TopicConfiguration? = null


    fun getTopics() = listOf(bankAccountCreated, balanceDeposited, balanceWithdrawn, emailChanged)

    @PostConstruct
    fun logConfigProperties() {
        log.info("KafkaTopicsConfiguration created topics: ${getTopics()}")
    }

    companion object {
        private val log = Loggers.getLogger(KafkaTopicsConfiguration::class.java)
    }
}