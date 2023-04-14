package com.alexbryksin.ordersmicroservice.order.controllers

import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.domain.OrderStatus
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import com.alexbryksin.ordersmicroservice.order.service.OrderService
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/v1/orders"])
class OrderController(private val orderService: OrderService) {

    @GetMapping
    suspend fun getOrders() = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("getOrderByID") }
    }

    @GetMapping(path = ["{id}"])
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScope {
        val result = orderService.getOrderWithProductItemsByID(UUID.fromString(id))
        ResponseEntity.ok().body(result).also { log.info("getOrderByID: $result") }
    }

    @GetMapping(path = ["/items/{id}"])
    suspend fun getOrderWithItemsByID(@PathVariable id: String) = coroutineScope {
        val result = orderService.getOrderWithOrderItemsByID(UUID.fromString(id))
        ResponseEntity.ok().body(result).also { log.info("getOrderWithItemsByID: $result") }
    }

    @GetMapping(path = ["/mono/{id}"])
    suspend fun getOrderWithItemsByIDMono(@PathVariable id: String) = coroutineScope {
        val result = orderService.getOrderWithOrderItemsByIDMono(UUID.fromString(id))
        ResponseEntity.ok().body(result).also { log.info("getOrderWithItemsByIDMono: $result") }
    }

    @PostMapping
    suspend fun createOrder() = coroutineScope {

        val order = OrderEntity(
            id = UUID.randomUUID(),
            email = "email123",
            address = "address1213",
            status = OrderStatus.NEW,
        )

        val items = sequenceOf(1, 2, 3).map {
            ProductItem(
                id = UUID.randomUUID(),
                orderId = order.id,
                title = "title $it",
                quantity = it.toLong()
            )
        }.toList()

        val result = orderService.saveOrderWithItems(order, items)
        ResponseEntity.ok().body(result).also { log.info("createOrder: $result") }
    }

    @PutMapping(path = ["{id}"])
    suspend fun updateOrder(@PathVariable id: String) = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("createOrder") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}