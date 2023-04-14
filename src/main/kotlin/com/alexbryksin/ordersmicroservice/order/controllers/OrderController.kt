package com.alexbryksin.ordersmicroservice.order.controllers

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import com.alexbryksin.ordersmicroservice.order.dto.CreateOrderDTO
import com.alexbryksin.ordersmicroservice.order.service.OrderService
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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


    @PutMapping(path = ["{id}"])
    suspend fun updateOrder(@PathVariable id: String) = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("createOrder") }
    }

    @PostMapping
    suspend fun createOrder(@RequestBody createOrderDTO: CreateOrderDTO) = coroutineScope {
        val order = Order(
            email = createOrderDTO.email,
            address = createOrderDTO.address,
            productItems = createOrderDTO.productItems.map { ProductItem(title = it.title, price = it.price, quantity = it.quantity) }
                .toMutableList()
        )
        val createdOrder = orderService.createOrder(order)
        ResponseEntity.status(HttpStatus.CREATED).body(createdOrder).also { log.info("createOrder: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}