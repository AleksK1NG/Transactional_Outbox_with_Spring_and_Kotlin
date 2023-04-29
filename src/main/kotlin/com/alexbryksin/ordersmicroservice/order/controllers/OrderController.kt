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
    ) = coroutineScopeWithObservation(GET_ORDERS, or) { observation ->
        ResponseEntity.ok()
            .body(orderService.getAllOrders(PageRequest.of(page, size))
                .map { OrderSuccessResponse.of(it) }
                .also { response -> observation.highCardinalityKeyValue("response", response.toString()) }
            )
    }

    @GetMapping(path = ["{id}"])
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScopeWithObservation(GET_ORDER_BY_ID, or) { observation ->
        orderService.getOrderWithProductsByID(UUID.fromString(id))
            .let { ResponseEntity.ok().body(OrderSuccessResponse.of(it)) }
            .also { response ->
                observation.highCardinalityKeyValue("response", response.toString())
                log.info("getOrderByID response: $response")
            }
    }

    @PostMapping
    suspend fun createOrder(@RequestBody createOrderDTO: CreateOrderDTO) = coroutineScopeWithObservation(CREATE_ORDER, or) { observation ->
        orderService.createOrder(createOrderDTO.toOrder()).let {
            log.info("created order: $it")
            observation.highCardinalityKeyValue("response", it.toString())
            ResponseEntity.status(HttpStatus.CREATED).body(OrderSuccessResponse.of(it))
        }
    }

    @PutMapping(path = ["/add/{id}"])
    suspend fun addProductItem(@PathVariable id: UUID, @RequestBody dto: CreateProductItemDTO) =
        coroutineScopeWithObservation(ADD_PRODUCT, or) { observation ->
            orderService.addProductItem(dto.toProductItem(id))
                .let { ResponseEntity.ok(it) }
                .also {
                    observation.highCardinalityKeyValue("CreateProductItemDTO", dto.toString())
                    observation.highCardinalityKeyValue("id", id.toString())
                    log.info("removeProductItem id: $id, dto: $dto")
                }
        }

    @PutMapping(path = ["/remove/{orderId}/{productItemId}"])
    suspend fun removeProductItem(@PathVariable orderId: UUID, @PathVariable productItemId: UUID) =
        coroutineScopeWithObservation(REMOVE_PRODUCT, or) { observation ->
            orderService.removeProductItem(orderId, productItemId).let { ResponseEntity.ok(it) }
                .also {
                    observation.highCardinalityKeyValue("productItemId", productItemId.toString())
                    observation.highCardinalityKeyValue("orderId", orderId.toString())
                    log.info("removeProductItem orderId: $orderId, productItemId: $productItemId")
                }
        }

    @PutMapping(path = ["/pay/{id}"])
    suspend fun payOrder(@PathVariable id: UUID, @RequestBody dto: PayOrderDTO) = coroutineScopeWithObservation(PAY_ORDER, or) { observation ->
        orderService.pay(id, dto.paymentId)
            .let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("payOrder result: $it")
            }
    }

    @PutMapping(path = ["/cancel/{id}"])
    suspend fun cancelOrder(@PathVariable id: UUID, @RequestBody dto: CancelOrderDTO) = coroutineScopeWithObservation(CANCEL_ORDER, or) { observation ->
        orderService.cancel(id, dto.reason)
            .let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("cancelOrder result: $it")
            }
    }

    @PutMapping(path = ["/submit/{id}"])
    suspend fun submitOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(SUBMIT_ORDER, or) { observation ->
        orderService.submit(id).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("submitOrder result: $it")
            }
    }

    @PutMapping(path = ["/complete/{id}"])
    suspend fun completeOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(COMPLETE_ORDER, or) { observation ->
        orderService.complete(id).let { ResponseEntity.ok(OrderSuccessResponse.of(it)) }
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("completeOrder result: $it")
            }
    }


    companion object {
        private val log = LoggerFactory.getLogger(OrderController::class.java)

        private const val COMPLETE_ORDER = "OrderController.completeOrder"
        private const val SUBMIT_ORDER = "OrderController.submitOrder"
        private const val CANCEL_ORDER = "OrderController.cancelOrder"
        private const val PAY_ORDER = "OrderController.payOrder"
        private const val CREATE_ORDER = "OrderController.createOrder"
        private const val REMOVE_PRODUCT = "OrderController.removeProductItem"
        private const val ADD_PRODUCT = "OrderController.addProductItem"
        private const val GET_ORDER_BY_ID = "OrderController.getOrderByID"
        private const val GET_ORDERS = "OrderController.getOrders"
    }
}