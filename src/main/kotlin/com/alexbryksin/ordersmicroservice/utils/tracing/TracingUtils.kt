package com.alexbryksin.ordersmicroservice.utils.tracing

import io.micrometer.context.ContextSnapshot
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T : Any> withTracing(name: String, or: ObservationRegistry, block: Mono<T>): Mono<T> {
    return Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { scope: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            block
                .doOnError { ex -> obs.error(ex) }
                .doFinally { signal -> obs.stop() }
        }
    }
}


fun <T : Any> withTracingFields(name: String, or: ObservationRegistry, vararg fields: Pair<String, String>, block: Mono<T>): Mono<T> {
    return Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { scope: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            fields.forEach { obs.highCardinalityKeyValue(it.first, it.second) }
            block
                .doOnError { ex -> obs.error(ex) }
                .doFinally { signal -> obs.stop() }
        }
    }
}


fun <T : Any> withTracingBlock(name: String, or: ObservationRegistry, vararg fields: Pair<String, String>, block: () -> Mono<T>): Mono<T> {
    return Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { scope: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            fields.forEach { obs.highCardinalityKeyValue(it.first, it.second) }
            block()
                .doOnError { ex -> obs.error(ex) }
                .doFinally { signal -> obs.stop() }
        }
    }
}

fun <T : Any> withTracingFluxBlock(name: String, or: ObservationRegistry, vararg fields: Pair<String, String>, block: () -> Flux<T>): Flux<T> {
    return Flux.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { scope: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            fields.forEach { obs.highCardinalityKeyValue(it.first, it.second) }
            block()
                .doOnError { ex -> obs.error(ex) }
                .doFinally { signal -> obs.stop() }
        }
    }
}

suspend fun <T : Any> withObservationSuspend(name: String, or: ObservationRegistry, block: suspend () -> T): T {
    return Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { _: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            mono { block() }
                .doOnError { ex -> obs.error(ex) }
                .doFinally { obs.stop() }
        }
    }
        .contextCapture()
        .awaitSingle()
}

suspend fun <T : Any> coroutineScopeWithObservation(name: String, or: ObservationRegistry, block: suspend () -> T): T = coroutineScope {
    Mono.deferContextual { ctxView ->
        ContextSnapshot.setThreadLocalsFrom(ctxView, ObservationThreadLocalAccessor.KEY).use { _: ContextSnapshot.Scope ->
            val obs = Observation.start(name, or)
            mono { block() }
                .doOnError { ex -> obs.error(ex) }
                .doFinally { obs.stop() }
        }
    }
        .contextCapture()
        .awaitSingle()
}

suspend fun <T> runObservationSuspend(name: String, or: ObservationRegistry, block: suspend () -> T) {
    val obs = Observation.start(name, or)
    val scope = obs.openScope()

    try {
        block()
    } catch (ex: Exception) {
        obs.error(ex)
        throw ex
    } finally {
        obs.stop()
        scope.close()
    }
}
