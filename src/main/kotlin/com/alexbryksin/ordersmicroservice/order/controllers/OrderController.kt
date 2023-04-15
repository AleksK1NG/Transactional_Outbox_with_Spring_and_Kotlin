package com.alexbryksin.ordersmicroservice.order.controllers

import com.alexbryksin.ordersmicroservice.order.dto.CreateOrderDTO
import com.alexbryksin.ordersmicroservice.order.dto.CreateProductItemDTO
import com.alexbryksin.ordersmicroservice.order.dto.OrderSuccessResponse
import com.alexbryksin.ordersmicroservice.order.dto.of
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
        orderService.getOrderWithProductItemsByID(UUID.fromString(id))
            .let { ResponseEntity.ok().body(OrderSuccessResponse.of(it)) }
            .also { response -> log.info("getOrderByID response: $response") }
    }


    @PutMapping(path = ["{id}"])
    suspend fun updateOrder(@PathVariable id: String) = coroutineScope {
        ResponseEntity.ok().body("OK").also { log.info("createOrder") }
    }

    @PostMapping
    suspend fun createOrder(@RequestBody createOrderDTO: CreateOrderDTO) = coroutineScope {
        orderService.createOrder(createOrderDTO.toOrder()).let {
            log.info("createOrder: $it")
            ResponseEntity.status(HttpStatus.CREATED).body(OrderSuccessResponse.of(it))
        }
    }


    @PutMapping(path = ["/add/{id}"])
    suspend fun addProductItem(@PathVariable id: UUID, @RequestBody dto: CreateProductItemDTO) = coroutineScope {
        orderService.addOrderItem(dto.toProductItem(id)).let { ResponseEntity.ok(it) }.also { log.info("addProductItem result: $it") }
    }

    @PutMapping(path = ["/remove/{orderId}/{productItemId}"])
    suspend fun removeProductItem(@PathVariable orderId: UUID, @PathVariable productItemId: UUID) = coroutineScope {
        orderService.removeProductItem(orderId, productItemId).let { ResponseEntity.ok(it) }.also { log.info("removeProductItem result: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}