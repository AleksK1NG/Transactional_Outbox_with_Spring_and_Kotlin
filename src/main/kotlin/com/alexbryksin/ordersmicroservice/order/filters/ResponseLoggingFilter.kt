package com.alexbryksin.ordersmicroservice.order.filters

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


@Component
@Order(5)
class ResponseLoggingFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()

        return chain.filter(exchange)
            .doFinally {
                if (!exchange.request.path.value().contains("actuator")) {
                    log.info(
                        "method: {}, path: {}, status: {}, headers: {}, time: {}ms",
                        exchange.request.method.name().uppercase(),
                        exchange.request.path.value(),
                        exchange.response.statusCode?.value(),
                        exchange.response.headers,
                        (System.currentTimeMillis() - startTime)
                    )
                }
            }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ResponseLoggingFilter::class.java)
    }
}