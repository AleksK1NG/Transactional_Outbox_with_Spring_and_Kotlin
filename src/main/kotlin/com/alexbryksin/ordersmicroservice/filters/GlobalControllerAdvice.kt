package com.alexbryksin.ordersmicroservice.filters

import com.alexbryksin.ordersmicroservice.exceptions.ErrorHttpResponse
import com.alexbryksin.ordersmicroservice.order.exceptions.*
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.LocalDateTime


@ControllerAdvice
@Order(2)
class GlobalControllerAdvice {

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleRuntimeException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.message ?: "",
            LocalDateTime.now().toString()
        )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) INTERNAL_SERVER_ERROR RuntimeException", ex) }
    }

    @ExceptionHandler(
        value = [
            OrderNotPaidException::class,
            CompleteOrderException::class,
            OrderHasNotProductItemsException::class,
            CancelOrderException::class,
            SubmitOrderException::class,
            InvalidPaymentIdException::class
        ]
    )
    fun handleOrderException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.message ?: "",
            LocalDateTime.now().toString()
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) BAD_REQUEST RuntimeException", ex) }
    }

    @ExceptionHandler(value = [OrderNotFoundException::class])
    fun handleOrderNotFoundException(ex: OrderNotFoundException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.message ?: "",
            LocalDateTime.now().toString()
        )
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) NOT_FOUND OrderNotFoundException", ex) }
    }


    companion object {
        private val log = LoggerFactory.getLogger(GlobalControllerAdvice::class.java)
    }
}