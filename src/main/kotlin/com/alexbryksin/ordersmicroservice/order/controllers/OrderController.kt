package com.alexbryksin.ordersmicroservice.order.controllers

import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/v1/orders"])
class OrderController {

    @GetMapping
    suspend fun getOrderByID() = coroutineScope {
         ResponseEntity.ok().body("OK").also { log.info("getOrderByID") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}