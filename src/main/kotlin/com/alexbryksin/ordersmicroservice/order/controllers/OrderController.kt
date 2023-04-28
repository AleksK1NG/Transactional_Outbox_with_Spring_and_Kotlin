package com.alexbryksin.ordersmicroservice.order.controllers

import com.alexbryksin.ordersmicroservice.order.dto.*
import com.alexbryksin.ordersmicroservice.order.service.OrderService
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/v1/orders"])
class OrderController(private val orderService: OrderService, private val or: ObservationRegistry) {

    @GetMapping
    suspend fun getOrders(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ) = coroutineScopeWithObservation("OrderController.getOrders", or, Pair("page", page.toString()), Pair("size", size.toString())) {
        ResponseEntity.ok().body(orderService.getAllOrders(PageRequest.of(page, size)).map { OrderSuccessResponse.of(it) })
            .also { log.info("getOrderByID") }
    }

    @GetMapping(path = ["{id}"])
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScopeWithObservation("OrderController.getOrderByID", or, Pair("id", id)) {
        orderService.getOrderWithProductsByID(UUID.fromString(id))
            .let { ResponseEntity.ok().body(OrderSuccessResponse.of(it)) }
            .also { response -> log.info("getOrderByID response: $response") }
    }

    @PostMapping
    suspend fun createOrder(@RequestBody createOrderDTO: CreateOrderDTO) = coroutineScopeWithObservation("OrderController.createOrder", or) {
        orderService.createOrder(createOrderDTO.toOrder()).let {
            log.info("createOrder: $it")
            ResponseEntity.status(HttpStatus.CREATED).body(OrderSuccessResponse.of(it))
        }
    }

    @PutMapping(path = ["/add/{id}"])
    suspend fun addProductItem(@PathVariable id: UUID, @RequestBody dto: CreateProductItemDTO) =
        coroutineScopeWithObservation("OrderController.addProductItem", or) {
            orderService.addProductItem(dto.toProductItem(id)).let { ResponseEntity.ok(it) }
                .also { log.info("addProductItem result: $it") }
        }

    @PutMapping(path = ["/remove/{orderId}/{productItemId}"])
    suspend fun removeProductItem(@PathVariable orderId: UUID, @PathVariable productItemId: UUID) =
        coroutineScopeWithObservation("OrderController.removeProductItem", or) {
            orderService.removeProductItem(orderId, productItemId).let { ResponseEntity.ok(it) }
                .also { log.info("removeProductItem result: $it") }
        }

    @PutMapping(path = ["/pay/{id}"])
    suspend fun payOrder(@PathVariable id: UUID, @RequestBody dto: PayOrderDTO) = coroutineScopeWithObservation("OrderController.payOrder", or) {
        orderService.pay(id, dto.paymentId).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }.also { log.info("payOrder result: $it") }
    }

    @PutMapping(path = ["/cancel/{id}"])
    suspend fun cancelOrder(@PathVariable id: UUID, @RequestBody dto: CancelOrderDTO) = coroutineScopeWithObservation("OrderController.cancelOrder", or) {
        orderService.cancel(id, dto.reason).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }.also { log.info("cancelOrder result: $it") }
    }

    @PutMapping(path = ["/submit/{id}"])
    suspend fun submitOrder(@PathVariable id: UUID) = coroutineScopeWithObservation("OrderController.submitOrder", or) {
        orderService.submit(id).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also { log.info("submitOrder result: $it") }
    }

    @PutMapping(path = ["/complete/{id}"])
    suspend fun completeOrder(@PathVariable id: UUID) = coroutineScopeWithObservation("OrderController.completeOrder", or) {
        orderService.complete(id).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also { log.info("completeOrder result: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)
    }
}