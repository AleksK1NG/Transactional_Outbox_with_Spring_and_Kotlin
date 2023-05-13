package com.alexbryksin.ordersmicroservice.order.controllers

import com.alexbryksin.ordersmicroservice.order.dto.*
import com.alexbryksin.ordersmicroservice.order.service.OrderService
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
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
    @Operation(method = "getOrders", summary = "get order with pagination", operationId = "getOrders")
    suspend fun getOrders(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ) = coroutineScopeWithObservation(GET_ORDERS, or) { observation ->
        ResponseEntity.ok()
            .body(orderService.getAllOrders(PageRequest.of(page, size))
                .map { it.toSuccessResponse() }
                .also { response -> observation.highCardinalityKeyValue("response", response.toString()) }
            )
    }

    @GetMapping(path = ["{id}"])
    @Operation(method = "getOrderByID", summary = "get order by id", operationId = "getOrderByID")
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScopeWithObservation(GET_ORDER_BY_ID, or) { observation ->
        ResponseEntity.ok().body(orderService.getOrderWithProductsByID(UUID.fromString(id)).toSuccessResponse())
            .also { response ->
                observation.highCardinalityKeyValue("response", response.toString())
                log.info("getOrderByID response: $response")
            }
    }

    @PostMapping
    @Operation(method = "createOrder", summary = "create new order", operationId = "createOrder")
    suspend fun createOrder(@Valid @RequestBody createOrderDTO: CreateOrderDTO) = coroutineScopeWithObservation(CREATE_ORDER, or) { observation ->
        ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(createOrderDTO.toOrder()).toSuccessResponse())
            .also {
                log.info("created order: $it")
                observation.highCardinalityKeyValue("response", it.toString())
            }
    }

    @PutMapping(path = ["/add/{id}"])
    @Operation(method = "addProductItem", summary = "add to the order product item", operationId = "addProductItem")
    suspend fun addProductItem(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: CreateProductItemDTO
    ) = coroutineScopeWithObservation(ADD_PRODUCT, or) { observation ->
        ResponseEntity.ok().body(orderService.addProductItem(dto.toProductItem(id)))
            .also {
                observation.highCardinalityKeyValue("CreateProductItemDTO", dto.toString())
                observation.highCardinalityKeyValue("id", id.toString())
                log.info("addProductItem id: $id, dto: $dto")
            }
    }

    @PutMapping(path = ["/remove/{orderId}/{productItemId}"])
    @Operation(method = "removeProductItem", summary = "remove product from the order", operationId = "removeProductItem")
    suspend fun removeProductItem(
        @PathVariable orderId: UUID,
        @PathVariable productItemId: UUID
    ) = coroutineScopeWithObservation(REMOVE_PRODUCT, or) { observation ->
        ResponseEntity.ok().body(orderService.removeProductItem(orderId, productItemId))
            .also {
                observation.highCardinalityKeyValue("productItemId", productItemId.toString())
                observation.highCardinalityKeyValue("orderId", orderId.toString())
                log.info("removeProductItem orderId: $orderId, productItemId: $productItemId")
            }
    }

    @PutMapping(path = ["/pay/{id}"])
    @Operation(method = "payOrder", summary = "pay order", operationId = "payOrder")
    suspend fun payOrder(@PathVariable id: UUID, @Valid @RequestBody dto: PayOrderDTO) = coroutineScopeWithObservation(PAY_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.pay(id, dto.paymentId).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("payOrder result: $it")
            }
    }

    @PutMapping(path = ["/cancel/{id}"])
    @Operation(method = "cancelOrder", summary = "cancel order", operationId = "cancelOrder")
    suspend fun cancelOrder(@PathVariable id: UUID, @Valid @RequestBody dto: CancelOrderDTO) = coroutineScopeWithObservation(CANCEL_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.cancel(id, dto.reason).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("cancelOrder result: $it")
            }
    }

    @PutMapping(path = ["/submit/{id}"])
    @Operation(method = "submitOrder", summary = "submit order", operationId = "submitOrder")
    suspend fun submitOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(SUBMIT_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.submit(id).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("submitOrder result: $it")
            }
    }

    @PutMapping(path = ["/complete/{id}"])
    @Operation(method = "completeOrder", summary = "complete order", operationId = "completeOrder")
    suspend fun completeOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(COMPLETE_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.complete(id).toSuccessResponse())
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