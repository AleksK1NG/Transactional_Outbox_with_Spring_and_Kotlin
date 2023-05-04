package com.alexbryksin.ordersmicroservice.schedulers

import com.alexbryksin.ordersmicroservice.order.service.OrderService
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@ConditionalOnProperty(prefix = "schedulers", value = ["outbox.enable"], havingValue = "true")
class OutboxScheduler(private val orderService: OrderService, private val or: ObservationRegistry) {

    @Scheduled(initialDelayString = "\${schedulers.outbox.initialDelayMillis}", fixedRateString = "\${schedulers.outbox.fixedRate}")
    fun publishAndDeleteOutboxRecords() = runBlocking {
        coroutineScopeWithObservation(PUBLISH_AND_DELETE_OUTBOX_RECORDS, or) {
            log.debug("starting scheduled outbox table publishing")
            orderService.deleteOutboxRecordsWithLock()
            log.debug("completed scheduled outbox table publishing")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxScheduler::class.java)

        private const val PUBLISH_AND_DELETE_OUTBOX_RECORDS = "OutboxScheduler.publishAndDeleteOutboxRecords"
    }
}