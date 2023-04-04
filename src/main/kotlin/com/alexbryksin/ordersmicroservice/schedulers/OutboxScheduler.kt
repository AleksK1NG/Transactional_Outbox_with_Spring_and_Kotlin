package com.alexbryksin.ordersmicroservice.schedulers

import com.alexbryksin.ordersmicroservice.bankAccount.commands.BankAccountCommandService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@ConditionalOnProperty(prefix = "schedulers", value = ["outbox.enable"], havingValue = "true")
class OutboxScheduler(private val bankAccountCommandService: BankAccountCommandService) {


    @Scheduled(initialDelay = 3000, fixedRate = 1000)
    fun publishAndDeleteOutboxRecords() = runBlocking {
        log.debug("starting scheduled outbox table publishing")
        bankAccountCommandService.deleteOutboxRecordsWithLock()
        log.info("completed scheduled outbox table publishing")
    }


    companion object {
        private val log = LoggerFactory.getLogger(OutboxScheduler::class.java)
    }

}