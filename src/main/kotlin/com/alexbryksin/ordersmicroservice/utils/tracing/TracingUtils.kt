package com.alexbryksin.ordersmicroservice.utils.tracing

import io.micrometer.context.ContextSnapshot
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono


suspend fun <T : Any> coroutineScopeWithObservation(name: String, or: ObservationRegistry, block: suspend (obs: Observation) -> T): T = coroutineScope {
    Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { _: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            mono { block(obs) }
                .doOnError { ex -> obs.error(ex) }
                .doFinally { obs.stop() }
        }
    }
        .contextCapture()
        .awaitSingle()
}

suspend fun <T : Any> coroutineScopeWithObservation(name: String, or: ObservationRegistry, vararg fields: Pair<String, String>, block: suspend (obs: Observation) -> T): T =
    coroutineScope {
        Mono.deferContextual { ctxView ->
            ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { _: ContextSnapshot.Scope ->
                val obs = Observation.start(name, or)
                fields.forEach { obs.highCardinalityKeyValue(it.first, it.second) }
                mono { block(obs) }
                    .doOnError { ex -> obs.error(ex) }
                    .doFinally { obs.stop() }
            }
        }
            .contextCapture()
            .awaitSingle()
    }


suspend fun <T : Any> coroutineScopeWithObservationMetrics(name: String, or: ObservationRegistry, vararg fields: Pair<String, String>, block: suspend () -> T): T =
    coroutineScope {
        Mono.deferContextual { ctxView ->
            ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { _: ContextSnapshot.Scope ->
                val obs = Observation.start(name, or)
                fields.forEach { obs.highCardinalityKeyValue(it.first, it.second) }
                mono { block() }
                    .doOnError { ex -> obs.error(ex) }
                    .doFinally { obs.stop() }
            }
        }
            .contextCapture()
            .awaitSingle()
    }
