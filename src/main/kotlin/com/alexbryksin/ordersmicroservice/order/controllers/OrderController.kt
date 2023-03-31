package com.alexbryksin.ordersmicroservice.order.controllers

import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(path = ["/api/v1/orders"])
class OrderController {

    @GetMapping
    suspend fun getOrders() = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("getOrderByID") }
    }

    @GetMapping(path = ["{id}"])
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScope {
         ResponseEntity.ok().body("OK").also { log.info("getOrderByID") }
    }

    @PostMapping
    suspend fun createOrder() = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("createOrder") }
    }

    @PutMapping(path = ["{id}"])
    suspend fun updateOrder(@PathVariable id: String) = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("createOrder") }
    }



    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}