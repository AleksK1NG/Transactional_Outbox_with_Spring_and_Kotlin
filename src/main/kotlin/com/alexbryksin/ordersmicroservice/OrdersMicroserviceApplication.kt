package com.alexbryksin.ordersmicroservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrdersMicroserviceApplication

fun main(args: Array<String>) {
	runApplication<OrdersMicroserviceApplication>(*args)
}
