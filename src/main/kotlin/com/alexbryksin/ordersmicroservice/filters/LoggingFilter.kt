package com.alexbryksin.ordersmicroservice.filters

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(5)
class LoggingFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()

        return chain.filter(exchange)
            .doOnSuccess {
                if (!exchange.request.path.value().contains("actuator")) {
                    log.info(
                        "{} {} {} {} {}ms",
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
        private val log = LoggerFactory.getLogger(LoggingFilter::class.java)
    }
}