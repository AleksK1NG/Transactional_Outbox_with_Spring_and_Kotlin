package com.alexbryksin.ordersmicroservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableR2dbcAuditing
@EnableScheduling
@EnableReactiveMongoAuditing
class OrdersMicroserviceApplication

fun main(args: Array<String>) {
	runApplication<OrdersMicroserviceApplication>(*args)
}
